package fpl.ph60001.chathub.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import fpl.ph60001.chathub.data.model.MessageDto
import fpl.ph60001.chathub.domain.model.Message
import fpl.ph60001.chathub.domain.model.UploadState
import fpl.ph60001.chathub.domain.repository.MessageRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thực thi các phương thức nghiệp vụ tin nhắn chi tiết với cơ chế kết nối thời gian thực Firestore
 * kết hợp với dữ liệu giả lập (mock data) động thông minh theo mọi UID của người dùng.
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : MessageRepository {

    // Danh sách lưu trữ cục bộ phục vụ cho chế độ Offline Demo
    private val mockMessagesMap = mutableMapOf<String, MutableList<Message>>()
    private val mockTypingMap = mutableMapOf<String, Boolean>()

    /**
     * Tạo dữ liệu lịch sử chat mẫu tiếng Việt cực kỳ sinh động khớp với ID của người bạn chat.
     */
    private fun generateDynamicMockMessages(conversationId: String): MutableList<Message> {
        val parts = conversationId.split("_")
        if (parts.size != 2) return mutableListOf()

        val demoPartners = listOf("demo_partner_1", "demo_partner_2", "demo_partner_3", "demo_partner_4")
        val partnerId = parts.find { it in demoPartners } ?: "demo_partner_1"
        val userId = parts.find { it !in demoPartners } ?: "demo_user_uid"

        val partnerName = when (partnerId) {
            "demo_partner_1" -> "Nguyễn Lân Anh"
            "demo_partner_2" -> "Trần Hoàng Nam"
            "demo_partner_3" -> "Phạm Thùy Linh"
            "demo_partner_4" -> "Lê Quốc Anh"
            else -> "Thành viên ChatHub"
        }

        return mutableListOf(
            Message(
                messageId = "m1_$partnerId",
                senderId = partnerId,
                senderName = partnerName,
                receiverId = userId,
                content = "Chào bạn! Mình là $partnerName. Rất vui được kết nối với bạn trên ChatHub nhé!",
                timestamp = System.currentTimeMillis() - 3600000 * 2, // 2 giờ trước
                type = "text",
                seenBy = listOf(userId, partnerId)
            ),
            Message(
                messageId = "m2_$partnerId",
                senderId = userId,
                senderName = "Tôi",
                receiverId = partnerId,
                content = "Chào $partnerName! Mình vừa thiết lập thành công giao diện ChatHub bằng Kotlin Jetpack Compose xong.",
                timestamp = System.currentTimeMillis() - 3600000 * 1, // 1 giờ trước
                type = "text",
                seenBy = listOf(userId, partnerId)
            ),
            Message(
                messageId = "m3_$partnerId",
                senderId = partnerId,
                senderName = partnerName,
                receiverId = userId,
                content = "Wow tuyệt vời quá! Hiệu ứng mượt mà và giao diện màu xanh dương nhìn sang trọng cực kỳ luôn á! 😍 Hãy thử nhấn giữ tin nhắn này của mình nhé!",
                timestamp = System.currentTimeMillis() - 1800000, // 30 phút trước
                type = "text",
                seenBy = listOf(partnerId)
            )
        )
    }

    override fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val cleanRoomKey = getCleanKey(conversationId)
        
        // Đăng ký lắng nghe Firestore Collection messages thời gian thực
        val listener = firestore.collection("conversations")
            .document(cleanRoomKey)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    // Lỗi kết nối → dùng mock data (demo) hoặc local cache
                    val isDemoConversation = cleanRoomKey.contains("demo_partner")
                    if (isDemoConversation) {
                        val localList = mockMessagesMap.getOrPut(cleanRoomKey) {
                            generateDynamicMockMessages(cleanRoomKey)
                        }
                        trySend(localList.toList())
                    } else {
                        val localList = mockMessagesMap[cleanRoomKey]?.toList() ?: emptyList()
                        trySend(localList)
                    }
                    return@addSnapshotListener
                }

                val firestoreMessages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MessageDto::class.java)?.toDomain()
                }

                // Xóa các tin nhắn "optimistic" đã được Firestore xác nhận khỏi local cache
                // để tránh tình trạng tin nhắn cũ từ phòng khác bị lọt vào
                val confirmedIds = firestoreMessages.map { it.messageId }.toSet()
                mockMessagesMap[cleanRoomKey]?.removeAll { it.messageId in confirmedIds }

                // Chỉ gộp các tin nhắn "optimistic" thực sự chưa được Firestore xác nhận
                val localPending = mockMessagesMap[cleanRoomKey]
                    ?.filter { local -> local.messageId !in confirmedIds }
                    ?: emptyList()

                val merged = (firestoreMessages + localPending).sortedBy { it.timestamp }
                trySend(merged)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(conversationId: String, message: Message): Result<Unit> = Result.runCatching {
        val cleanRoomKey = getCleanKey(conversationId)
        val docRef = firestore.collection("conversations")
            .document(cleanRoomKey)
            .collection("messages")
            .document()
        
        val finalMessage = message.copy(messageId = docRef.id, timestamp = System.currentTimeMillis())
        val dto = MessageDto.fromDomain(finalMessage)

        // OPTIMISTIC UPDATE: Thêm vào local cache NGAY LẬP TỨC để UI hiển thị không chờ Firestore
        val localList = mockMessagesMap.getOrPut(cleanRoomKey) { mutableListOf() }
        localList.add(finalMessage)

        try {
            // Ghi lên Firestore - realtime listener sẽ đồng bộ lại và xoá bản local khỏi "pending"
            docRef.set(dto).await()
        } catch (e: Exception) {
            // Firestore thất bại → local cache vẫn đang giữ tin nhắn, UI đã hiển thị rồi
            return@runCatching
        }

        try {
            // Cập nhật lastMessage dùng merge để tránh lỗi field chưa tồn tại
            firestore.collection("conversations").document(cleanRoomKey)
                .set(
                    mapOf(
                        "lastMessage" to message.content.ifEmpty { "[Hình ảnh]" },
                        "lastMessageTime" to finalMessage.timestamp
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
        } catch (e: Exception) {
            // Bỏ qua lỗi meta-update
        }
    }

    override suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> = Result.runCatching {
        val cleanRoomKey = getCleanKey(conversationId)
        try {
            firestore.collection("conversations")
                .document(cleanRoomKey)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "content" to "Tin nhắn đã bị thu hồi",
                        "isDeleted" to true
                    )
                ).await()
        } catch (e: Exception) {
            val localList = mockMessagesMap[cleanRoomKey]
            val index = localList?.indexOfFirst { it.messageId == messageId } ?: -1
            if (index != -1) {
                localList!![index] = localList[index].copy(
                    content = "Tin nhắn đã bị thu hồi",
                    isDeleted = true
                )
            }
        }
    }

    override suspend fun editMessage(conversationId: String, messageId: String, newContent: String): Result<Unit> = Result.runCatching {
        val cleanRoomKey = getCleanKey(conversationId)
        try {
            firestore.collection("conversations")
                .document(cleanRoomKey)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "content" to newContent,
                        "isEdited" to true
                    )
                ).await()
        } catch (e: Exception) {
            val localList = mockMessagesMap[cleanRoomKey]
            val index = localList?.indexOfFirst { it.messageId == messageId } ?: -1
            if (index != -1) {
                localList!![index] = localList[index].copy(
                    content = newContent,
                    isEdited = true
                )
            }
        }
    }

    override suspend fun reactToMessage(
        conversationId: String,
        messageId: String,
        userId: String,
        emoji: String
    ): Result<Unit> = Result.runCatching {
        val cleanRoomKey = getCleanKey(conversationId)
        try {
            val docRef = firestore.collection("conversations")
                .document(cleanRoomKey)
                .collection("messages")
                .document(messageId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentReactions = snapshot.get("reactions") as? Map<String, String> ?: emptyMap()
                val updatedReactions = currentReactions.toMutableMap()
                if (emoji.isEmpty()) {
                    updatedReactions.remove(userId)
                } else {
                    updatedReactions[userId] = emoji
                }
                transaction.update(docRef, "reactions", updatedReactions)
            }.await()
        } catch (e: Exception) {
            val localList = mockMessagesMap[cleanRoomKey]
            val index = localList?.indexOfFirst { it.messageId == messageId } ?: -1
            if (index != -1) {
                val current = localList!![index]
                val updated = current.reactions.toMutableMap()
                if (emoji.isEmpty()) {
                    updated.remove(userId)
                } else {
                    updated[userId] = emoji
                }
                localList[index] = current.copy(reactions = updated)
            }
        }
    }

    override suspend fun markAsSeen(conversationId: String, messageId: String, userId: String): Result<Unit> = Result.runCatching {
        val cleanRoomKey = getCleanKey(conversationId)
        try {
            firestore.collection("conversations")
                .document(cleanRoomKey)
                .collection("messages")
                .document(messageId)
                .update("seenBy", FieldValue.arrayUnion(userId))
                .await()
        } catch (e: Exception) {
            val localList = mockMessagesMap[cleanRoomKey]
            val index = localList?.indexOfFirst { it.messageId == messageId } ?: -1
            if (index != -1) {
                val current = localList!![index]
                if (!current.seenBy.contains(userId)) {
                    localList[index] = current.copy(seenBy = current.seenBy + userId)
                }
            }
        }
    }

    override fun getTypingStatus(conversationId: String, partnerId: String): Flow<Boolean> = callbackFlow {
        val cleanRoomKey = getCleanKey(conversationId)
        
        val docRef = firestore.collection("conversations")
            .document(cleanRoomKey)
            .collection("typing")
            .document(partnerId)
            
        val listener = docRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val isTyping = snapshot.getBoolean("isTyping") ?: false
                trySend(isTyping)
            } else {
                trySend(mockTypingMap[cleanRoomKey] ?: false)
            }
        }
        
        awaitClose { listener.remove() }
    }

    override suspend fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean): Result<Unit> = Result.runCatching {
        val cleanRoomKey = getCleanKey(conversationId)
        try {
            firestore.collection("conversations")
                .document(cleanRoomKey)
                .collection("typing")
                .document(userId)
                .set(mapOf("isTyping" to isTyping))
                .await()
        } catch (e: Exception) {
            // Không làm gì, trạng thái typing tự động tắt
        }
    }

    override fun uploadMedia(
        conversationId: String,
        storagePath: String,
        bytes: ByteArray,
        fileName: String
    ): Flow<UploadState> = callbackFlow {
        val cleanRoomKey = getCleanKey(conversationId)
        val storageRef = storage.reference
            .child("chats/$cleanRoomKey/$storagePath/$fileName")

        val uploadTask = storageRef.putBytes(bytes)

        // Lắng nghe tiến trình tải lên realtime
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            trySend(UploadState.Progress(progress))
        }

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                trySend(UploadState.Success(uri.toString()))
                close()
            }.addOnFailureListener { e ->
                trySend(UploadState.Error(e.localizedMessage ?: "Lỗi lấy đường dẫn tải xuống"))
                close()
            }
        }.addOnFailureListener { e ->
            trySend(UploadState.Error(e.localizedMessage ?: "Tải tệp lên thất bại"))
            close()
        }

        awaitClose { uploadTask.cancel() }
    }

    /**
     * Giúp tạo key phòng hội thoại chuẩn hóa (RoomKey).
     * - Nếu là chat 1-1: sắp xếp 2 UID theo alphabet để tạo key nhất quán.
     * - Nếu là nhóm (groupId không có dạng uid_uid): giữ nguyên groupId.
     */
    private fun getCleanKey(id: String): String {
        if (id.isEmpty()) return "unknown_room"
        // Chỉ sắp xếp nếu ĐÚNG dạng uid_uid (2 phần, mỗi phần không rỗng)
        val parts = id.split("_")
        return if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
            // Chat 1-1: sắp xếp để key nhất quán
            if (parts[0] < parts[1]) "${parts[0]}_${parts[1]}" else "${parts[1]}_${parts[0]}"
        } else {
            // Nhóm chat hoặc ID đặc biệt: giữ nguyên
            id
        }
    }

    /**
     * Giả lập bot phản hồi thông minh đồng thời tự động cập nhật status.
     */
    private fun simulateBotReply(
        roomKey: String,
        userText: String,
        writeToFirestore: Boolean,
        userId: String,
        partnerId: String
    ) {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                // 1. Bật trạng thái đối phương "Đang nhập..."
                mockTypingMap[roomKey] = true
                if (writeToFirestore) {
                    firestore.collection("conversations")
                        .document(roomKey)
                        .collection("typing")
                        .document(partnerId)
                        .set(mapOf("isTyping" to true))
                }
                
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        // 2. Tắt trạng thái đối phương "Đang nhập..."
                        mockTypingMap[roomKey] = false
                        if (writeToFirestore) {
                            firestore.collection("conversations")
                                .document(roomKey)
                                .collection("typing")
                                .document(partnerId)
                                .set(mapOf("isTyping" to false))
                        }
                        
                        val partnerName = when (partnerId) {
                            "demo_partner_1" -> "Nguyễn Lân Anh"
                            "demo_partner_2" -> "Trần Hoàng Nam"
                            "demo_partner_3" -> "Phạm Thùy Linh"
                            "demo_partner_4" -> "Lê Quốc Anh"
                            else -> "Thành viên ChatHub"
                        }

                        val replyText = when {
                            userText.contains("xin chào", ignoreCase = true) || userText.contains("hello", ignoreCase = true) -> 
                                "Chào bạn! Rất vui được trò chuyện cùng bạn qua ChatHub. ❤️"
                            userText.contains("emoji", ignoreCase = true) || userText.contains("cảm xúc", ignoreCase = true) -> 
                                "Đúng vậy! Bạn có thể nhấn giữ tin nhắn của tôi và thả icon cực cool nhé! 😂👍"
                            userText.contains("sửa", ignoreCase = true) || userText.contains("xóa", ignoreCase = true) -> 
                                "Bạn có thể tự do chỉnh sửa hoặc thu hồi tin nhắn của bạn mọi lúc mọi nơi."
                            else -> "Ồ! Đó là một điều rất thú vị. Bạn có thể chia sẻ thêm cho tôi không? 🤔"
                        }

                        val botMsg = Message(
                            messageId = "bot_${System.currentTimeMillis()}",
                            senderId = partnerId,
                            senderName = partnerName,
                            receiverId = userId,
                            content = replyText,
                            timestamp = System.currentTimeMillis(),
                            type = "text",
                            seenBy = listOf(partnerId)
                        )

                        if (writeToFirestore) {
                            firestore.collection("conversations")
                                .document(roomKey)
                                .collection("messages")
                                .document(botMsg.messageId)
                                .set(MessageDto.fromDomain(botMsg))
                            
                            firestore.collection("conversations").document(roomKey).update(
                                mapOf(
                                    "lastMessage" to replyText,
                                    "lastMessageTime" to botMsg.timestamp
                                )
                            )
                        } else {
                            val list = mockMessagesMap[roomKey] ?: return
                            list.add(botMsg)
                        }
                    }
                }, 1800)
            }
        }, 1000)
    }

    override suspend fun deleteMessagePermanently(conversationId: String, messageId: String): Result<Unit> = Result.runCatching {
        val cleanRoomKey = getCleanKey(conversationId)
        try {
            firestore.collection("conversations")
                .document(cleanRoomKey)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
        } catch (e: Exception) {
            val localList = mockMessagesMap[cleanRoomKey]
            localList?.removeAll { it.messageId == messageId }
        }
    }

    override fun clearMockMessages(conversationId: String) {
        val cleanRoomKey = getCleanKey(conversationId)
        mockMessagesMap[cleanRoomKey] = mutableListOf()
    }
}

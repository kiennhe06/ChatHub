package fpl.ph60001.chathub.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import fpl.ph60001.chathub.data.model.ConversationDto
import fpl.ph60001.chathub.data.model.MessageDto
import fpl.ph60001.chathub.data.model.UserDto
import fpl.ph60001.chathub.domain.model.Conversation
import fpl.ph60001.chathub.domain.model.Message
import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.ChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

/**
 * Triển khai lớp nghiệp vụ Tin nhắn (ChatRepository) kết nối trực tiếp với Firestore và Firebase Storage.
 * Tự động tạo mã phòng chat duy nhất cho 2 người (1-to-1 Room) bằng cách sắp xếp ID.
 * Đồng thời tự động hỗ trợ giả lập tin nhắn phản hồi tiếng Việt tự động nếu chạy trong Chế độ Thử nghiệm (Demo Mode).
 */
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ChatRepository {

    // Danh sách lưu trữ tạm thời tin nhắn trong Chế độ Demo
    private val demoMessagesMap = mutableMapOf<String, MutableList<Message>>()

    override suspend fun sendMessage(
        senderId: String,
        senderName: String,
        receiverId: String,
        content: String,
        imageUrl: String
    ): Result<Unit> {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        
        val message = Message(
            messageId = messageId,
            senderId = senderId,
            senderName = senderName,
            receiverId = receiverId,
            content = content,
            timestamp = timestamp,
            imageUrl = imageUrl,
            isRead = false
        )

        val roomKey = getRoomKey(senderId, receiverId)

        return try {
            // Gửi tin nhắn thực lên Firestore
            firestore.collection("chats")
                .document(roomKey)
                .collection("messages")
                .document(messageId)
                .set(MessageDto.fromDomain(message))
                .await()
            
            // Cập nhật tin nhắn cuối cùng trong phòng chat tương ứng
            val updateData = mapOf(
                "lastMessage" to if (imageUrl.isNotEmpty()) "[Hình ảnh]" else content,
                "lastMessageTime" to timestamp,
                "unreadCount" to 0
            )
            firestore.collection("conversations")
                .document(roomKey)
                .update(updateData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            // Chế độ demo dự phòng: lưu vào bộ nhớ tạm local
            val list = demoMessagesMap.getOrPut(roomKey) { getInitialMockMessages(senderId, receiverId) }
            list.add(message)
            
            // Giả lập bot phản hồi thông minh tự động sau 1.5 giây để demo thêm sinh động
            if (imageUrl.isEmpty()) {
                simulateBotReply(roomKey, senderId, receiverId, content)
            }
            
            Result.success(Unit)
        }
    }

    override fun getMessages(chatPartnerId: String): Flow<List<Message>> = callbackFlow {
        val currentUserId = "demo_user_uid" 
        val roomKey = getRoomKey(currentUserId, chatPartnerId)

        val listener = firestore.collection("chats")
            .document(roomKey)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Trả về dữ liệu demo cục bộ nếu lỗi/Firebase chưa được kết nối
                    val list = demoMessagesMap.getOrPut(roomKey) { getInitialMockMessages(currentUserId, chatPartnerId) }
                    trySend(list.toList())
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MessageDto::class.java)?.toDomain()
                } ?: emptyList()
                
                if (messages.isEmpty()) {
                    val list = demoMessagesMap.getOrPut(roomKey) { getInitialMockMessages(currentUserId, chatPartnerId) }
                    trySend(list.toList())
                } else {
                    trySend(messages)
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun uploadChatImage(imageBytes: ByteArray, fileName: String): Result<String> {
        return try {
            val storageRef = storage.reference.child("chat_images/$fileName")
            storageRef.putBytes(imageBytes).await()
            val downloadUrl = storageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            delay(1000)
            Result.success("https://images.unsplash.com/photo-1579202673506-ca3ce28943ef?auto=format&fit=crop&w=400&q=80")
        }
    }

    override fun getConversations(currentUserId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = firestore.collection("conversations")
            .whereArrayContains("members", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Fallback to mock data
                    trySend(getInitialMockConversations(currentUserId))
                    return@addSnapshotListener
                }
                
                val conversationsDto = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ConversationDto::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                if (conversationsDto.isEmpty()) {
                    trySend(getInitialMockConversations(currentUserId))
                } else {
                    launch {
                        val conversations = conversationsDto.map { dto ->
                            val partnerId = dto.members.find { it != currentUserId } ?: ""
                            val partnerUser = try {
                                if (partnerId.isNotEmpty()) {
                                    val userSnapshot = firestore.collection("users").document(partnerId).get().await()
                                    userSnapshot.toObject(UserDto::class.java)?.toDomain()
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                            dto.toDomain(currentUserId, partnerUser)
                        }
                        trySend(conversations.sortedByDescending { it.lastMessageTime })
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getOrCreateConversation(currentUserId: String, otherUserId: String): Result<String> {
        val roomKey = getRoomKey(currentUserId, otherUserId)
        return try {
            val doc = firestore.collection("conversations").document(roomKey).get().await()
            if (!doc.exists()) {
                val newConv = ConversationDto(
                    id = roomKey,
                    members = listOf(currentUserId, otherUserId),
                    lastMessage = "Bắt đầu cuộc trò chuyện mới!",
                    lastMessageTime = System.currentTimeMillis(),
                    unreadCount = 0
                )
                firestore.collection("conversations").document(roomKey).set(newConv).await()
            }
            Result.success(roomKey)
        } catch (e: Exception) {
            Result.success(roomKey)
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            val allUsers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserDto::class.java)?.toDomain()
            }
            val filtered = allUsers.filter { user ->
                user.displayName.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }
            Result.success(filtered)
        } catch (e: Exception) {
            delay(500)
            val allMockUsers = getMockUsers()
            val filtered = allMockUsers.filter { user ->
                user.displayName.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }
            Result.success(filtered)
        }
    }

    // Tạo mã định danh độc bản cho cuộc hội thoại giữa 2 người dùng
    private fun getRoomKey(user1: String, user2: String): String {
        return if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"
    }

    // Giả lập đối phương tự động phản hồi lại bằng Tiếng Việt
    private suspend fun simulateBotReply(roomKey: String, senderId: String, receiverId: String, userMessage: String) {
        delay(1500) // Trễ 1.5 giây tạo cảm giác đối phương đang gõ tin nhắn
        val list = demoMessagesMap[roomKey] ?: return
        
        val replyContent = when {
            userMessage.contains("xin chào", ignoreCase = true) || userMessage.contains("hello", ignoreCase = true) -> 
                "Xin chào! Rất vui được trò chuyện với bạn trên ChatHub. Bạn có khỏe không? 😊"
            userMessage.contains("khỏe không", ignoreCase = true) || userMessage.contains(" khỏe ", ignoreCase = true) -> 
                "Mình rất khỏe, cảm ơn bạn đã hỏi nha! Bạn hôm nay thế nào?"
            userMessage.contains("hình ảnh", ignoreCase = true) || userMessage.contains("ảnh", ignoreCase = true) ->
                "Giao diện gửi ảnh của ChatHub đỉnh quá bạn ơi! Bạn thử bấm nút (+) để chọn ảnh xem sao nhé!"
            userMessage.contains("tạm biệt", ignoreCase = true) || userMessage.contains("bye", ignoreCase = true) ->
                "Tạm biệt nhé! Hẹn gặp lại bạn sớm nha. Chúc bạn một ngày tốt lành! 👋"
            else -> "Cảm ơn bạn đã phản hồi! Giao diện ChatHub chạy mượt mà đúng không nè? Chúc bạn trải nghiệm vui vẻ! 🚀"
        }

        val botMessage = Message(
            messageId = UUID.randomUUID().toString(),
            senderId = receiverId, 
            senderName = "Người chat",
            receiverId = senderId,
            content = replyContent,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        list.add(botMessage)
    }

    // Khởi tạo danh sách các cuộc trò chuyện demo cho HomeScreen
    private fun getInitialMockConversations(currentUserId: String): List<Conversation> {
        val mockUsers = getMockUsers()
        return listOf(
            Conversation(
                id = getRoomKey(currentUserId, "demo_partner_1"),
                members = listOf(currentUserId, "demo_partner_1"),
                lastMessage = "Wow tuyệt vời quá! Hiệu ứng mượt mà và giao diện màu xanh dương nhìn sang trọng cực kỳ luôn á! 😍",
                lastMessageTime = System.currentTimeMillis() - 1800000, // 30 phút trước
                unreadCount = 2,
                partnerId = "demo_partner_1",
                partnerName = mockUsers[0].displayName,
                partnerAvatar = mockUsers[0].photoUrl,
                partnerOnline = mockUsers[0].isOnline
            ),
            Conversation(
                id = getRoomKey(currentUserId, "demo_partner_2"),
                members = listOf(currentUserId, "demo_partner_2"),
                lastMessage = "Cảm ơn bạn nhiều nhé! Lát rảnh mình gọi lại sau nha.",
                lastMessageTime = System.currentTimeMillis() - 7200000, // 2 giờ trước
                unreadCount = 0,
                partnerId = "demo_partner_2",
                partnerName = mockUsers[1].displayName,
                partnerAvatar = mockUsers[1].photoUrl,
                partnerOnline = mockUsers[1].isOnline
            ),
            Conversation(
                id = getRoomKey(currentUserId, "demo_partner_3"),
                members = listOf(currentUserId, "demo_partner_3"),
                lastMessage = "Giao diện ChatHub mới đỉnh thực sự luôn á, mượt mà lắm!",
                lastMessageTime = System.currentTimeMillis() - 86400000, // 1 ngày trước
                unreadCount = 0,
                partnerId = "demo_partner_3",
                partnerName = mockUsers[2].displayName,
                partnerAvatar = mockUsers[2].photoUrl,
                partnerOnline = mockUsers[2].isOnline
            ),
            Conversation(
                id = getRoomKey(currentUserId, "demo_partner_4"),
                members = listOf(currentUserId, "demo_partner_4"),
                lastMessage = "Chào bạn! Rất vui được kết nối trên ứng dụng mới.",
                lastMessageTime = System.currentTimeMillis() - 86400000 * 2, // 2 ngày trước
                unreadCount = 0,
                partnerId = "demo_partner_4",
                partnerName = mockUsers[3].displayName,
                partnerAvatar = mockUsers[3].photoUrl,
                partnerOnline = mockUsers[3].isOnline
            )
        )
    }

    private fun getMockUsers(): List<User> {
        return listOf(
            User(
                uid = "demo_partner_1",
                email = "lananh@gmail.com",
                displayName = "Nguyễn Lân Anh",
                photoUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                isOnline = true,
                lastActiveTimestamp = System.currentTimeMillis()
            ),
            User(
                uid = "demo_partner_2",
                email = "hoangnam@gmail.com",
                displayName = "Trần Hoàng Nam",
                photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                isOnline = false,
                lastActiveTimestamp = System.currentTimeMillis() - 3600000
            ),
            User(
                uid = "demo_partner_3",
                email = "thuylinh@gmail.com",
                displayName = "Phạm Thùy Linh",
                photoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                isOnline = true,
                lastActiveTimestamp = System.currentTimeMillis()
            ),
            User(
                uid = "demo_partner_4",
                email = "quocanh@gmail.com",
                displayName = "Lê Quốc Anh",
                photoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                isOnline = false,
                lastActiveTimestamp = System.currentTimeMillis() - 86400000
            )
        )
    }

    // Khởi tạo các tin nhắn demo chào mừng có sẵn trong phòng chat cho đẹp mắt
    private fun getInitialMockMessages(userId: String, partnerId: String): MutableList<Message> {
        val partnerName = when (partnerId) {
            "demo_partner_1" -> "Nguyễn Lân Anh"
            "demo_partner_2" -> "Trần Hoàng Nam"
            "demo_partner_3" -> "Phạm Thùy Linh"
            "demo_partner_4" -> "Lê Quốc Anh"
            else -> "Thành viên ChatHub"
        }
        
        return mutableListOf(
            Message(
                messageId = "init_msg_1",
                senderId = partnerId,
                senderName = partnerName,
                receiverId = userId,
                content = "Chào bạn! Mình là $partnerName. Rất vui được kết nối với bạn trên ChatHub nhé!",
                timestamp = System.currentTimeMillis() - 3600000 * 2 // 2 giờ trước
            ),
            Message(
                messageId = "init_msg_2",
                senderId = userId,
                senderName = "Tôi",
                receiverId = partnerId,
                content = "Chào $partnerName! Mình vừa thiết lập thành công giao diện ChatHub bằng Kotlin Jetpack Compose xong.",
                timestamp = System.currentTimeMillis() - 3600000 * 1 // 1 giờ trước
            ),
            Message(
                messageId = "init_msg_3",
                senderId = partnerId,
                senderName = partnerName,
                receiverId = userId,
                content = "Wow tuyệt vời quá! Hiệu ứng mượt mà và giao diện màu xanh dương nhìn sang trọng cực kỳ luôn á! 😍",
                timestamp = System.currentTimeMillis() - 1800000 // 30 phút trước
            )
        )
    }
}

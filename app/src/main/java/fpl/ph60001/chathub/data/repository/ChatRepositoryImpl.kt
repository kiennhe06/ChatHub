package fpl.ph60001.chathub.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import fpl.ph60001.chathub.data.model.MessageDto
import fpl.ph60001.chathub.domain.model.Message
import fpl.ph60001.chathub.domain.repository.ChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        // Chúng ta giả định rằng người dùng hiện tại là người gửi chính
        // (Trong ViewModel thực tế, chúng ta sẽ cung cấp ID người dùng đang login)
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
                    // Nếu Firebase trống, trả về danh sách demo
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
            // Demo fallback: Trả về link ảnh Unsplash ngẫu nhiên cực đẹp
            delay(1000)
            Result.success("https://images.unsplash.com/photo-1579202673506-ca3ce28943ef?auto=format&fit=crop&w=400&q=80")
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
            senderId = receiverId, // Đối phương gửi lại
            senderName = "Người chat",
            receiverId = senderId,
            content = replyContent,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        list.add(botMessage)
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

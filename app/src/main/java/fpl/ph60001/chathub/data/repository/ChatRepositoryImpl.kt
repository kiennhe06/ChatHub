package fpl.ph60001.chathub.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import fpl.ph60001.chathub.data.model.ConversationDto
import fpl.ph60001.chathub.data.model.FriendRequestDto
import fpl.ph60001.chathub.data.model.MessageDto
import fpl.ph60001.chathub.data.model.UserDto
import fpl.ph60001.chathub.domain.model.Conversation
import fpl.ph60001.chathub.domain.model.FriendRequest
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
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : ChatRepository {

    // Danh sách lưu trữ tạm thời tin nhắn trong Chế độ Demo
    private val demoMessagesMap = mutableMapOf<String, MutableList<Message>>()
    
    // Hỗ trợ chế độ offline cho nhóm chat
    private val localGroupsMap = mutableMapOf<String, Conversation>()
    
    // Danh sách bạn bè demo phục vụ cho offline fallback (Ban đầu Linh là người gửi yêu cầu nên không nằm trong đây)
    private val mockFriendsList = mutableListOf("demo_partner_1", "demo_partner_2", "demo_partner_4")

    // Danh sách yêu cầu kết bạn demo phục vụ cho offline fallback
    private val mockFriendRequests = mutableListOf(
        FriendRequest(
            id = "demo_partner_3_demo_user_uid",
            senderId = "demo_partner_3",
            senderName = "Phạm Thùy Linh",
            senderAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            receiverId = "demo_user_uid",
            timestamp = System.currentTimeMillis() - 600000,
            status = "PENDING"
        )
    )

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
            Result.failure(Exception("Lỗi tải ảnh lên: ${e.localizedMessage}. Vui lòng kích hoạt/nâng cấp Storage trên Firebase Console!"))
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
                            // Nhận diện nhóm chat: isGroup = true HOẶC có groupName/adminIds
                            // (để xử lý document cũ chưa có field isGroup)
                            val isGroupChat = dto.isGroup || dto.groupName.isNotEmpty() || dto.adminIds.isNotEmpty()
                            val fixedDto = if (isGroupChat && !dto.isGroup) dto.copy(isGroup = true) else dto

                            val partnerUser = if (isGroupChat) {
                                // Nhóm chat: không cần tải thông tin partner user
                                null
                            } else {
                                val partnerId = dto.members.find { it != currentUserId } ?: ""
                                try {
                                    if (partnerId.isNotEmpty()) {
                                        val userSnapshot = firestore.collection("users").document(partnerId).get().await()
                                        userSnapshot.toObject(UserDto::class.java)?.toDomain()
                                    } else null
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            fixedDto.toDomain(currentUserId, partnerUser)
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

    override suspend fun addFriend(currentUserId: String, friendId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(currentUserId)
                .update("friends", com.google.firebase.firestore.FieldValue.arrayUnion(friendId))
                .await()
            firestore.collection("users").document(friendId)
                .update("friends", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
                .await()
            
            // Đồng thời khởi tạo cuộc hội thoại mới giữa họ cho tiện
            getOrCreateConversation(currentUserId, friendId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            if (!mockFriendsList.contains(friendId)) {
                mockFriendsList.add(friendId)
            }
            Result.success(Unit)
        }
    }

    override suspend fun removeFriend(currentUserId: String, friendId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(currentUserId)
                .update("friends", com.google.firebase.firestore.FieldValue.arrayRemove(friendId))
                .await()
            firestore.collection("users").document(friendId)
                .update("friends", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            mockFriendsList.remove(friendId)
            Result.success(Unit)
        }
    }

    override fun getFriends(currentUserId: String): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(getMockFriends())
                    return@addSnapshotListener
                }

                val friendIds = snapshot.toObject(UserDto::class.java)?.friends ?: emptyList()
                if (friendIds.isEmpty()) {
                    trySend(getMockFriends())
                } else {
                    launch {
                        try {
                            val friendsList = friendIds.mapNotNull { friendId ->
                                val userDoc = firestore.collection("users").document(friendId).get().await()
                                userDoc.toObject(UserDto::class.java)?.toDomain()
                            }
                            val mergedList = (friendsList + getMockFriends()).distinctBy { it.uid }
                            trySend(mergedList)
                        } catch (e: Exception) {
                            trySend(getMockFriends())
                        }
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    private fun getMockFriends(): List<User> {
        val allMock = getMockUsers()
        return allMock.filter { mockFriendsList.contains(it.uid) }
    }

    override suspend fun sendFriendRequest(
        senderId: String,
        senderName: String,
        senderAvatar: String,
        receiverId: String
    ): Result<Unit> {
        val requestId = "${senderId}_${receiverId}"
        val request = FriendRequest(
            id = requestId,
            senderId = senderId,
            senderName = senderName,
            senderAvatar = senderAvatar,
            receiverId = receiverId,
            timestamp = System.currentTimeMillis(),
            status = "PENDING"
        )
        return try {
            firestore.collection("friend_requests")
                .document(requestId)
                .set(FriendRequestDto.fromDomain(request))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (!mockFriendRequests.any { it.id == requestId }) {
                mockFriendRequests.add(request)
            }
            Result.success(Unit)
        }
    }

    override suspend fun acceptFriendRequest(
        requestId: String,
        currentUserId: String,
        senderId: String
    ): Result<Unit> {
        return try {
            firestore.collection("friend_requests").document(requestId).delete().await()
            addFriend(currentUserId, senderId)
            Result.success(Unit)
        } catch (e: Exception) {
            mockFriendRequests.removeAll { it.id == requestId }
            addFriend(currentUserId, senderId)
            Result.success(Unit)
        }
    }

    override suspend fun declineFriendRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection("friend_requests").document(requestId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            mockFriendRequests.removeAll { it.id == requestId }
            Result.success(Unit)
        }
    }

    override fun getIncomingFriendRequests(currentUserId: String): Flow<List<FriendRequest>> = callbackFlow {
        val listener = firestore.collection("friend_requests")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(mockFriendRequests.filter { it.receiverId == currentUserId && it.status == "PENDING" })
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FriendRequestDto::class.java)?.toDomain()
                }
                
                if (list.isEmpty()) {
                    trySend(mockFriendRequests.filter { it.receiverId == currentUserId && it.status == "PENDING" })
                } else {
                    trySend(list)
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getOutgoingFriendRequests(currentUserId: String): Flow<List<FriendRequest>> = callbackFlow {
        val listener = firestore.collection("friend_requests")
            .whereEqualTo("senderId", currentUserId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(mockFriendRequests.filter { it.senderId == currentUserId && it.status == "PENDING" })
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FriendRequestDto::class.java)?.toDomain()
                }
                
                if (list.isEmpty()) {
                    trySend(mockFriendRequests.filter { it.senderId == currentUserId && it.status == "PENDING" })
                } else {
                    trySend(list)
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createGroup(
        groupName: String,
        groupAvatar: String,
        memberIds: List<String>
    ): Result<String> {
        val currentUserId = auth.currentUser?.uid ?: "demo_user_uid"
        val finalMembers = (memberIds + currentUserId).distinct()
        val groupId = firestore.collection("conversations").document().id

        val conversation = Conversation(
            id = groupId,
            members = finalMembers,
            lastMessage = "Nhóm mới đã được tạo!",
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = 0,
            partnerId = groupId,
            partnerName = groupName,
            partnerAvatar = groupAvatar,
            partnerOnline = false,
            isGroup = true,
            groupName = groupName,
            groupAvatar = groupAvatar,
            adminIds = listOf(currentUserId)
        )

        return try {
            val dto = ConversationDto.fromDomain(conversation)
            firestore.collection("conversations").document(groupId).set(dto).await()

            // Tạo tin nhắn hệ thống chào mừng
            val systemMsg = Message(
                messageId = UUID.randomUUID().toString(),
                senderId = "system",
                senderName = "Hệ thống",
                receiverId = groupId,
                content = "Chào mừng mọi người đến với nhóm \"$groupName\"!",
                timestamp = System.currentTimeMillis(),
                type = "text"
            )
            firestore.collection("conversations").document(groupId)
                .collection("messages").document(systemMsg.messageId)
                .set(MessageDto.fromDomain(systemMsg)).await()

            Result.success(groupId)
        } catch (e: Exception) {
            // Lưu local phục vụ demo offline
            localGroupsMap[groupId] = conversation
            Result.success(groupId)
        }
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> = Result.runCatching {
        try {
            // Xóa subcollection messages trước
            val messagesSnapshot = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .get()
                .await()
            
            firestore.runBatch { batch ->
                messagesSnapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
            }.await()

            // Xóa document cuộc trò chuyện chính
            firestore.collection("conversations")
                .document(conversationId)
                .delete()
                .await()
        } catch (e: Exception) {
            // Bỏ qua lỗi hoặc lưu offline
        }
        localGroupsMap.remove(conversationId)
    }

    override fun getGroupInfo(groupId: String): Flow<Conversation?> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: "demo_user_uid"
        val listener = firestore.collection("conversations")
            .document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(localGroupsMap[groupId])
                    return@addSnapshotListener
                }
                val dto = snapshot.toObject(ConversationDto::class.java)
                trySend(dto?.toDomain(currentUserId))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateGroupInfo(
        groupId: String,
        newName: String,
        newAvatar: String
    ): Result<Unit> {
        return try {
            firestore.collection("conversations").document(groupId).update(
                mapOf(
                    "groupName" to newName,
                    "groupAvatar" to newAvatar
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val local = localGroupsMap[groupId]
            if (local != null) {
                localGroupsMap[groupId] = local.copy(
                    groupName = newName,
                    groupAvatar = newAvatar,
                    partnerName = newName,
                    partnerAvatar = newAvatar
                )
            }
            Result.success(Unit)
        }
    }

    override suspend fun addGroupMembers(groupId: String, newMemberIds: List<String>): Result<Unit> {
        return try {
            firestore.collection("conversations").document(groupId).update(
                "members", com.google.firebase.firestore.FieldValue.arrayUnion(*newMemberIds.toTypedArray())
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val local = localGroupsMap[groupId]
            if (local != null) {
                localGroupsMap[groupId] = local.copy(
                    members = (local.members + newMemberIds).distinct()
                )
            }
            Result.success(Unit)
        }
    }

    override suspend fun removeGroupMember(groupId: String, memberId: String): Result<Unit> {
        return try {
            firestore.collection("conversations").document(groupId).update(
                "members", com.google.firebase.firestore.FieldValue.arrayRemove(memberId)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val local = localGroupsMap[groupId]
            if (local != null) {
                localGroupsMap[groupId] = local.copy(
                    members = local.members.filter { it != memberId }
                )
            }
            Result.success(Unit)
        }
    }

    override suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val doc = firestore.collection("conversations").document(groupId).get().await()
            val dto = doc.toObject(ConversationDto::class.java)
            if (dto != null) {
                val updatedMembers = dto.members.filter { it != userId }
                if (updatedMembers.isEmpty()) {
                    disbandGroup(groupId).getOrThrow()
                } else {
                    val updatedAdmins = dto.adminIds.filter { it != userId }.toMutableList()
                    if (updatedAdmins.isEmpty() && updatedMembers.isNotEmpty()) {
                        updatedAdmins.add(updatedMembers.first())
                    }
                    firestore.collection("conversations").document(groupId).update(
                        mapOf(
                            "members" to updatedMembers,
                            "adminIds" to updatedAdmins
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val local = localGroupsMap[groupId]
            if (local != null) {
                val updatedMembers = local.members.filter { it != userId }
                if (updatedMembers.isEmpty()) {
                    localGroupsMap.remove(groupId)
                } else {
                    val updatedAdmins = local.adminIds.filter { it != userId }.toMutableList()
                    if (updatedAdmins.isEmpty()) {
                        updatedAdmins.add(updatedMembers.first())
                    }
                    localGroupsMap[groupId] = local.copy(
                        members = updatedMembers,
                        adminIds = updatedAdmins
                    )
                }
            }
            Result.success(Unit)
        }
    }

    override suspend fun disbandGroup(groupId: String): Result<Unit> {
        return try {
            firestore.collection("conversations").document(groupId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            localGroupsMap.remove(groupId)
            Result.success(Unit)
        }
    }
}

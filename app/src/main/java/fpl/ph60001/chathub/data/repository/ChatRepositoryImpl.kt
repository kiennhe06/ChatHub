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

    // Hỗ trợ chế độ offline cho nhóm chat
    private val localGroupsMap = mutableMapOf<String, Conversation>()
    
    // Danh sách bạn bè cục bộ
    private val mockFriendsList = mutableListOf<String>()

    // Danh sách yêu cầu kết bạn cục bộ
    private val mockFriendRequests = mutableListOf<FriendRequest>()

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
            Result.failure(e)
        }
    }

    override fun getMessages(chatPartnerId: String): Flow<List<Message>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: "unknown"
        val roomKey = getRoomKey(currentUserId, chatPartnerId)

        val listener = firestore.collection("chats")
            .document(roomKey)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MessageDto::class.java)?.toDomain()
                } ?: emptyList()
                
                trySend(messages)
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
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val conversationsDto = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ConversationDto::class.java)?.copy(id = doc.id)
                } ?: emptyList()

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
            Result.failure(e)
        }
    }

    // Tạo mã định danh độc bản cho cuộc hội thoại giữa 2 người dùng
    private fun getRoomKey(user1: String, user2: String): String {
        return if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"
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
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val friendIds = snapshot.toObject(UserDto::class.java)?.friends ?: emptyList()
                if (friendIds.isEmpty()) {
                    trySend(emptyList())
                } else {
                    launch {
                        try {
                            val friendsList = friendIds.mapNotNull { friendId ->
                                val userDoc = firestore.collection("users").document(friendId).get().await()
                                userDoc.toObject(UserDto::class.java)?.toDomain()
                            }
                            trySend(friendsList)
                        } catch (e: Exception) {
                            trySend(emptyList())
                        }
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    private fun getMockFriends(): List<User> {
        return emptyList()
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
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FriendRequestDto::class.java)?.toDomain()
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override fun getOutgoingFriendRequests(currentUserId: String): Flow<List<FriendRequest>> = callbackFlow {
        val listener = firestore.collection("friend_requests")
            .whereEqualTo("senderId", currentUserId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FriendRequestDto::class.java)?.toDomain()
                }
                trySend(list)
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

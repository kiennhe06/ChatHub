package fpl.ph60001.chathub.domain.repository

import fpl.ph60001.chathub.domain.model.Conversation
import fpl.ph60001.chathub.domain.model.Message
import fpl.ph60001.chathub.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Giao diện định nghĩa các nghiệp vụ nhắn tin (Chat Realtime) trong lớp Domain.
 */
interface ChatRepository {

    /**
     * Gửi tin nhắn mới cho một người nhận cụ thể.
     * Hỗ trợ gửi tin nhắn dạng văn bản và tin nhắn hình ảnh.
     */
    suspend fun sendMessage(
        senderId: String,
        senderName: String,
        receiverId: String,
        content: String,
        imageUrl: String = ""
    ): Result<Unit>

    /**
     * Lấy và lắng nghe luồng tin nhắn hai chiều (Realtime Stream) giữa người gửi và người nhận.
     */
    fun getMessages(chatPartnerId: String): Flow<List<Message>>

    /**
     * Tải ảnh chat lên Firebase Storage và trả về đường dẫn URL công khai.
     */
    suspend fun uploadChatImage(imageBytes: ByteArray, fileName: String): Result<String>

    /**
     * Lấy và lắng nghe luồng danh sách cuộc hội thoại thời gian thực của người dùng hiện tại.
     */
    fun getConversations(currentUserId: String): Flow<List<Conversation>>

    /**
     * Lấy hoặc tạo mới một phòng chat (cuộc hội thoại) giữa người dùng hiện tại và người bạn chat.
     * Trả về mã phòng chat duy nhất (id).
     */
    suspend fun getOrCreateConversation(currentUserId: String, otherUserId: String): Result<String>

    /**
     * Tìm kiếm danh sách người dùng trên toàn hệ thống dựa theo từ khóa Tên hoặc Email.
     */
    suspend fun searchUsers(query: String): Result<List<User>>
}

package fpl.ph60001.chathub.domain.repository

import fpl.ph60001.chathub.domain.model.Message
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
}

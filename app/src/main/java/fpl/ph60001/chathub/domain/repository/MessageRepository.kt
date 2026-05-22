package fpl.ph60001.chathub.domain.repository

import fpl.ph60001.chathub.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Giao diện định nghĩa các nghiệp vụ tin nhắn chi tiết (phòng chat 1-1) trong lớp Domain.
 */
interface MessageRepository {

    /**
     * Lấy luồng tin nhắn thời gian thực của phòng chat.
     */
    fun getMessages(conversationId: String): Flow<List<Message>>

    /**
     * Gửi tin nhắn mới vào phòng chat.
     */
    suspend fun sendMessage(conversationId: String, message: Message): Result<Unit>

    /**
     * Xóa tin nhắn (thu hồi tin nhắn) phía người gửi.
     */
    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit>

    /**
     * Chỉnh sửa nội dung tin nhắn đã gửi.
     */
    suspend fun editMessage(conversationId: String, messageId: String, newContent: String): Result<Unit>

    /**
     * Bày tỏ biểu cảm Emoji (reaction) vào một tin nhắn cụ thể.
     */
    suspend fun reactToMessage(conversationId: String, messageId: String, userId: String, emoji: String): Result<Unit>

    /**
     * Đánh dấu tin nhắn là Đã xem bằng cách thêm ID người xem.
     */
    suspend fun markAsSeen(conversationId: String, messageId: String, userId: String): Result<Unit>

    /**
     * Lắng nghe trạng thái đang gõ phím của đối phương thời gian thực.
     */
    fun getTypingStatus(conversationId: String, partnerId: String): Flow<Boolean>

    /**
     * Cập nhật trạng thái đang gõ phím của bản thân lên hệ thống.
     */
    suspend fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean): Result<Unit>
}

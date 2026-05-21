package fpl.ph60001.chathub.data.model

import fpl.ph60001.chathub.domain.model.Message

/**
 * Đối tượng truyền dữ liệu (DTO) của Tin nhắn dùng cho tương tác với Firestore.
 */
data class MessageDto(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val imageUrl: String = "",
    @field:JvmField // Đảm bảo Firestore đọc đúng thuộc tính Boolean kiểu isRead
    val isRead: Boolean = false
) {
    /**
     * Chuyển đổi DTO thành Business Model trong lớp Domain.
     */
    fun toDomain(): Message {
        return Message(
            messageId = messageId,
            senderId = senderId,
            senderName = senderName,
            receiverId = receiverId,
            content = content,
            timestamp = timestamp,
            imageUrl = imageUrl,
            isRead = isRead
        )
    }

    companion object {
        /**
         * Tạo DTO từ Business Model lớp Domain để gửi lên Firestore.
         */
        fun fromDomain(message: Message): MessageDto {
            return MessageDto(
                messageId = message.messageId,
                senderId = message.senderId,
                senderName = message.senderName,
                receiverId = message.receiverId,
                content = message.content,
                timestamp = message.timestamp,
                imageUrl = message.imageUrl,
                isRead = message.isRead
            )
        }
    }
}

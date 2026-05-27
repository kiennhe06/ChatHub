package fpl.ph60001.chathub.data.model

import fpl.ph60001.chathub.domain.model.Message
import fpl.ph60001.chathub.domain.model.ReplyTo

/**
 * Đối tượng DTO lưu trữ thông tin Reply của tin nhắn để làm việc với Firestore.
 */
data class ReplyToDto(
    val id: String = "",
    val content: String = ""
) {
    fun toDomain(): ReplyTo {
        return ReplyTo(id = id, content = content)
    }

    companion object {
        fun fromDomain(domain: ReplyTo?): ReplyToDto? {
            if (domain == null) return null
            return ReplyToDto(id = domain.id, content = domain.content)
        }
    }
}

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
    val isRead: Boolean = false,
    val type: String = "text",
    val seenBy: List<String> = emptyList(),
    val replyTo: ReplyToDto? = null,
    val reactions: Map<String, String> = emptyMap(),
    @field:JvmField
    val isDeleted: Boolean = false,
    @field:JvmField
    val isEdited: Boolean = false,
    val mediaUrl: String = "",
    val fileName: String = "",
    val fileSize: Long = 0L,
    val duration: Long = 0L,
    @field:JvmField
    val isSecret: Boolean = false
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
            isRead = isRead,
            type = type,
            seenBy = seenBy,
            replyTo = replyTo?.toDomain(),
            reactions = reactions,
            isDeleted = isDeleted,
            isEdited = isEdited,
            mediaUrl = mediaUrl,
            fileName = fileName,
            fileSize = fileSize,
            duration = duration,
            isSecret = isSecret
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
                isRead = message.isRead,
                type = message.type,
                seenBy = message.seenBy,
                replyTo = ReplyToDto.fromDomain(message.replyTo),
                reactions = message.reactions,
                isDeleted = message.isDeleted,
                isEdited = message.isEdited,
                mediaUrl = message.mediaUrl,
                fileName = message.fileName,
                fileSize = message.fileSize,
                duration = message.duration,
                isSecret = message.isSecret
            )
        }
    }
}

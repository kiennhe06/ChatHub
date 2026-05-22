package fpl.ph60001.chathub.domain.model

/**
 * Model phụ đại diện cho thông tin tin nhắn được phản hồi (Reply).
 *
 * @property id ID của tin nhắn gốc.
 * @property content Nội dung của tin nhắn gốc.
 */
data class ReplyTo(
    val id: String = "",
    val content: String = ""
)

package fpl.ph60001.chathub.domain.model

/**
 * Model đại diện cho một Tin nhắn trong lớp nghiệp vụ (Domain Layer).
 *
 * @property messageId Mã định danh duy nhất của tin nhắn.
 * @property senderId Mã định danh người gửi.
 * @property senderName Tên hiển thị người gửi tin.
 * @property receiverId Mã định danh người nhận (hoặc group).
 * @property content Nội dung tin nhắn dạng văn bản.
 * @property timestamp Thời gian gửi tin nhắn (dạng timestamp).
 * @property imageUrl Đường dẫn ảnh đi kèm nếu có (cho tin nhắn hình ảnh).
 * @property isRead Trạng thái đã đọc (True nếu người nhận đã đọc).
 */
data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val imageUrl: String = "",
    val isRead: Boolean = false
)

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
 * @property type Kiểu tin nhắn ("text" / "image" / "file" / "voice").
 * @property seenBy Danh sách UID những người đã xem tin nhắn này.
 * @property replyTo Tin nhắn gốc mà tin nhắn này phản hồi lại (nếu có).
 * @property reactions Map biểu cảm icon: userId -> emoji (Ví dụ: "uid1" -> "❤️").
 * @property isDeleted Đánh dấu nếu tin nhắn này đã bị xóa bởi người gửi.
 * @property isEdited Đánh dấu nếu tin nhắn này đã từng được sửa đổi nội dung.
 * @property mediaUrl Đường dẫn URL tệp media trên Firebase Storage (ảnh/file/voice).
 * @property fileName Tên tệp đính kèm gốc (ví dụ: "bao_cao.pdf").
 * @property fileSize Dung lượng tệp đính kèm tính theo bytes.
 * @property duration Thời lượng tin nhắn thoại tính theo milliseconds (dành cho voice message).
 */
data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val imageUrl: String = "",
    val isRead: Boolean = false,
    val type: String = "text",
    val seenBy: List<String> = emptyList(),
    val replyTo: ReplyTo? = null,
    val reactions: Map<String, String> = emptyMap(),
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val mediaUrl: String = "",
    val fileName: String = "",
    val fileSize: Long = 0L,
    val duration: Long = 0L,
    val isSecret: Boolean = false
)

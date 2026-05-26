package fpl.ph60001.chathub.domain.model

/**
 * Model đại diện cho một Cuộc hội thoại trong lớp nghiệp vụ (Domain Layer).
 *
 * @property id Mã phòng chat duy nhất (Ví dụ: "uid1_uid2").
 * @property members Danh sách các mã UID của thành viên tham gia.
 * @property lastMessage Nội dung tin nhắn cuối cùng gửi trong phòng.
 * @property lastMessageTime Thời gian gửi tin nhắn cuối cùng (dạng timestamp).
 * @property unreadCount Số lượng tin nhắn chưa đọc của cuộc hội thoại này.
 * @property partnerId ID của đối phương (bạn chat).
 * @property partnerName Tên hiển thị của đối phương.
 * @property partnerAvatar Đường dẫn ảnh đại diện của đối phương.
 * @property partnerOnline Trạng thái trực tuyến của đối phương.
 */
data class Conversation(
    val id: String = "",
    val members: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val partnerId: String = "",
    val partnerName: String = "Người dùng ChatHub",
    val partnerAvatar: String = "",
    val partnerOnline: Boolean = false,
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupAvatar: String = "",
    val adminIds: List<String> = emptyList()
)

package fpl.ph60001.chathub.domain.model

/**
 * Model đại diện cho một Lời mời kết bạn (Friend Request).
 */
data class FriendRequest(
    val id: String = "",           // Thường dạng: A_B (senderId_receiverId)
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val receiverId: String = "",
    val timestamp: Long = 0L,
    val status: String = "PENDING" // PENDING, ACCEPTED, DECLINED
)

package fpl.ph60001.chathub.data.model

import fpl.ph60001.chathub.domain.model.FriendRequest

/**
 * Lớp DTO ánh xạ dữ liệu Yêu cầu kết bạn với Firestore.
 */
data class FriendRequestDto(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val receiverId: String = "",
    val timestamp: Long = 0L,
    val status: String = "PENDING"
) {
    fun toDomain(): FriendRequest {
        return FriendRequest(
            id = id,
            senderId = senderId,
            senderName = senderName,
            senderAvatar = senderAvatar,
            receiverId = receiverId,
            timestamp = timestamp,
            status = status
        )
    }

    companion object {
        fun fromDomain(domain: FriendRequest): FriendRequestDto {
            return FriendRequestDto(
                id = domain.id,
                senderId = domain.senderId,
                senderName = domain.senderName,
                senderAvatar = domain.senderAvatar,
                receiverId = domain.receiverId,
                timestamp = domain.timestamp,
                status = domain.status
            )
        }
    }
}

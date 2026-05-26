package fpl.ph60001.chathub.data.model

import fpl.ph60001.chathub.domain.model.Conversation
import fpl.ph60001.chathub.domain.model.User

/**
 * Data Transfer Object đại diện cho tài liệu phòng chat trên Firestore collection "conversations".
 */
data class ConversationDto(
    val id: String = "",
    val members: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupAvatar: String = "",
    val adminIds: List<String> = emptyList()
) {
    /**
     * Chuyển đổi DTO sang Business Model dùng cho lớp Domain và Presentation.
     * Tự động lấy thông tin đối phương (Partner) để người dùng hiển thị lên UI.
     */
    fun toDomain(currentUserId: String, partnerUser: User? = null): Conversation {
        val partnerId = if (isGroup) id else (members.find { it != currentUserId } ?: "")
        return Conversation(
            id = id,
            members = members,
            lastMessage = lastMessage,
            lastMessageTime = lastMessageTime,
            unreadCount = unreadCount,
            partnerId = partnerId,
            partnerName = if (isGroup) groupName else (partnerUser?.displayName ?: "Thành viên ChatHub"),
            partnerAvatar = if (isGroup) groupAvatar else (partnerUser?.photoUrl ?: ""),
            partnerOnline = if (isGroup) false else (partnerUser?.isOnline ?: false),
            isGroup = isGroup,
            groupName = groupName,
            groupAvatar = groupAvatar,
            adminIds = adminIds
        )
    }

    companion object {
        /**
         * Tạo DTO từ Business Model lớp Domain để gửi lên Firestore.
         */
        fun fromDomain(conversation: Conversation): ConversationDto {
            return ConversationDto(
                id = conversation.id,
                members = conversation.members,
                lastMessage = conversation.lastMessage,
                lastMessageTime = conversation.lastMessageTime,
                unreadCount = conversation.unreadCount,
                isGroup = conversation.isGroup,
                groupName = conversation.groupName,
                groupAvatar = conversation.groupAvatar,
                adminIds = conversation.adminIds
            )
        }
    }
}

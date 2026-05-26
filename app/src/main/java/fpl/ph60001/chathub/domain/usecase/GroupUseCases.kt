package fpl.ph60001.chathub.domain.usecase

import fpl.ph60001.chathub.domain.model.Conversation
import fpl.ph60001.chathub.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase tạo nhóm chat mới.
 */
class CreateGroupUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(
        groupName: String,
        groupAvatar: String,
        memberIds: List<String>
    ): Result<String> {
        return repository.createGroup(groupName, groupAvatar, memberIds)
    }
}

/**
 * UseCase lấy thông tin nhóm chat thời gian thực.
 */
class GetGroupInfoUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(groupId: String): Flow<Conversation?> {
        return repository.getGroupInfo(groupId)
    }
}

/**
 * UseCase cập nhật thông tin nhóm (Tên/Ảnh đại diện).
 */
class UpdateGroupInfoUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(
        groupId: String,
        newName: String,
        newAvatar: String
    ): Result<Unit> {
        return repository.updateGroupInfo(groupId, newName, newAvatar)
    }
}

/**
 * UseCase quản lý thành viên nhóm (thêm, xóa, rời, giải tán).
 */
class ManageGroupMembersUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend fun addMembers(groupId: String, newMemberIds: List<String>): Result<Unit> {
        return repository.addGroupMembers(groupId, newMemberIds)
    }

    suspend fun removeMember(groupId: String, memberId: String): Result<Unit> {
        return repository.removeGroupMember(groupId, memberId)
    }

    suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> {
        return repository.leaveGroup(groupId, userId)
    }

    suspend fun disbandGroup(groupId: String): Result<Unit> {
        return repository.disbandGroup(groupId)
    }
}

package fpl.ph60001.chathub.domain.usecase

import fpl.ph60001.chathub.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * UseCase khởi tạo hoặc lấy phòng chat sẵn có giữa người dùng hiện tại và một người dùng khác.
 */
class GetOrCreateConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(currentUserId: String, otherUserId: String): Result<String> {
        if (currentUserId.isEmpty() || otherUserId.isEmpty()) {
            return Result.failure(IllegalArgumentException("ID người dùng không được để trống"))
        }
        return chatRepository.getOrCreateConversation(currentUserId, otherUserId)
    }
}

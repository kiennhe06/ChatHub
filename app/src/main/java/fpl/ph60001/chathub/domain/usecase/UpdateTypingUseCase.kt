package fpl.ph60001.chathub.domain.usecase

import fpl.ph60001.chathub.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * UseCase cập nhật trạng thái đang gõ phím của người dùng lên cuộc hội thoại.
 */
class UpdateTypingUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(conversationId: String, userId: String, isTyping: Boolean): Result<Unit> {
        return messageRepository.updateTypingStatus(conversationId, userId, isTyping)
    }
}

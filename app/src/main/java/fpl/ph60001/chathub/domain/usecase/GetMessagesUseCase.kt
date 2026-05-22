package fpl.ph60001.chathub.domain.usecase

import fpl.ph60001.chathub.domain.model.Message
import fpl.ph60001.chathub.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase lấy luồng tin nhắn thời gian thực của cuộc trò chuyện.
 */
class GetMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(conversationId: String): Flow<List<Message>> {
        return messageRepository.getMessages(conversationId)
    }
}

package fpl.ph60001.chathub.domain.usecase

import fpl.ph60001.chathub.domain.model.Message
import fpl.ph60001.chathub.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * UseCase gửi tin nhắn mới (văn bản hoặc hình ảnh) vào cuộc trò chuyện.
 */
class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(conversationId: String, message: Message): Result<Unit> {
        if (message.content.trim().isEmpty() && message.imageUrl.isEmpty()) {
            return Result.failure(IllegalArgumentException("Nội dung tin nhắn không được để trống"))
        }
        return messageRepository.sendMessage(conversationId, message)
    }
}

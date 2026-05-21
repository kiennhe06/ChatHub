package fpl.ph60001.chathub.domain.usecase

import fpl.ph60001.chathub.domain.model.Conversation
import fpl.ph60001.chathub.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase lấy danh sách các cuộc hội thoại thời gian thực của người dùng đang đăng nhập.
 */
class GetConversationsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(currentUserId: String): Flow<List<Conversation>> {
        return chatRepository.getConversations(currentUserId)
    }
}

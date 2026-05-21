package fpl.ph60001.chathub.domain.usecase

import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * UseCase tìm kiếm người dùng trong hệ thống theo Tên hoặc Email.
 */
class SearchUsersUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(query: String): Result<List<User>> {
        if (query.trim().isEmpty()) {
            return Result.success(emptyList())
        }
        return chatRepository.searchUsers(query.trim())
    }
}

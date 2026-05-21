package fpl.ph60001.chathub.domain.usecase

import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * UseCase thực hiện đăng ký tài khoản mới kèm lưu thông tin họ tên lên hệ thống.
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, displayName: String): Result<User> {
        return authRepository.register(email, password, displayName)
    }
}

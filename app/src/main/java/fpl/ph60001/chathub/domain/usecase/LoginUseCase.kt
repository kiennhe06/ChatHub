package fpl.ph60001.chathub.domain.usecase

import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * UseCase thực hiện đăng nhập bằng tài khoản Email và Mật khẩu.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return authRepository.login(email, password)
    }
}

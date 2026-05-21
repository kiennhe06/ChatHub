package fpl.ph60001.chathub.domain.usecase

import fpl.ph60001.chathub.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * UseCase thực hiện gửi liên kết đặt lại mật khẩu đến Email người dùng.
 */
class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return authRepository.sendPasswordResetEmail(email)
    }
}

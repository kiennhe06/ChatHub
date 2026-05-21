package fpl.ph60001.chathub.domain.usecase

import fpl.ph60001.chathub.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * UseCase kiểm tra xem người dùng đã đăng nhập vào hệ thống từ phiên trước hay chưa.
 */
class CheckUserLoggedInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Boolean {
        return authRepository.getCurrentUser() != null
    }
}

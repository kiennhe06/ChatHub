package fpl.ph60001.chathub.domain.usecase

import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * UseCase thực hiện đăng nhập bằng Google Sign-In thông qua ID Token.
 */
class LoginWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<User> {
        return authRepository.loginWithGoogle(idToken)
    }
}

package fpl.ph60001.chathub.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.usecase.LoginUseCase
import fpl.ph60001.chathub.domain.usecase.LoginWithGoogleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lớp ViewModel quản lý trạng thái và luồng dữ liệu cho màn hình Đăng nhập (LoginScreen).
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase
) : ViewModel() {

    // Trạng thái Email nhập vào
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    // Trạng thái Mật khẩu nhập vào
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    // Trạng thái đang tải dữ liệu (Loading)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Trạng thái thông báo lỗi (Error)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Trạng thái đăng nhập thành công
    private val _isLoginSuccess = MutableStateFlow(false)
    val isLoginSuccess: StateFlow<Boolean> = _isLoginSuccess.asStateFlow()

    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail
        _errorMessage.value = null // Xóa thông báo lỗi khi nhập lại
    }

    fun onPasswordChanged(newPassword: String) {
        _password.value = newPassword
        _errorMessage.value = null
    }

    /**
     * Thực hiện đăng nhập tài khoản bằng Email/Mật khẩu.
     */
    fun login() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value.trim()

        // Kiểm tra dữ liệu đầu vào cơ bản tiếng Việt
        if (currentEmail.isEmpty()) {
            _errorMessage.value = "Vui lòng nhập địa chỉ email!"
            return
        }
        if (currentPassword.isEmpty()) {
            _errorMessage.value = "Vui lòng nhập mật khẩu!"
            return
        }
        if (currentPassword.length < 6) {
            _errorMessage.value = "Mật khẩu phải chứa ít nhất 6 ký tự!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = loginUseCase(currentEmail, currentPassword)
            
            _isLoading.value = false
            result.onSuccess {
                _isLoginSuccess.value = true
            }
            result.onFailure { exception ->
                _errorMessage.value = exception.localizedMessage ?: "Đăng nhập thất bại. Vui lòng kiểm tra lại!"
            }
        }
    }

    /**
     * Thực hiện đăng nhập tài khoản bằng tài khoản Google.
     */
    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = loginWithGoogleUseCase(idToken)
            
            _isLoading.value = false
            result.onSuccess {
                _isLoginSuccess.value = true
            }
            result.onFailure { exception ->
                _errorMessage.value = exception.localizedMessage ?: "Đăng nhập Google thất bại!"
            }
        }
    }

    fun onGoogleSignInFailed(message: String) {
        _errorMessage.value = message
    }
    
    fun resetSuccessState() {
        _isLoginSuccess.value = false
    }
}

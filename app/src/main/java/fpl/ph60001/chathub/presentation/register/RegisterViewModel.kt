package fpl.ph60001.chathub.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lớp ViewModel quản lý trạng thái và luồng dữ liệu cho màn hình Đăng ký (RegisterScreen).
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    // Trạng thái Tên hiển thị
    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    // Trạng thái Email
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    // Trạng thái Mật khẩu
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    // Trạng thái Xác nhận mật khẩu
    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    // Trạng thái đang tải dữ liệu (Loading)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Trạng thái thông báo lỗi (Error)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Trạng thái đăng ký thành công
    private val _isRegisterSuccess = MutableStateFlow(false)
    val isRegisterSuccess: StateFlow<Boolean> = _isRegisterSuccess.asStateFlow()

    fun onDisplayNameChanged(newName: String) {
        _displayName.value = newName
        _errorMessage.value = null
    }

    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail
        _errorMessage.value = null
    }

    fun onPasswordChanged(newPassword: String) {
        _password.value = newPassword
        _errorMessage.value = null
    }

    fun onConfirmPasswordChanged(newConfirm: String) {
        _confirmPassword.value = newConfirm
        _errorMessage.value = null
    }

    /**
     * Thực hiện đăng ký tài khoản mới.
     */
    fun register() {
        val currentName = _displayName.value.trim()
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value.trim()
        val currentConfirm = _confirmPassword.value.trim()

        // Kiểm tra dữ liệu đầu vào cơ bản bằng tiếng Việt
        if (currentName.isEmpty()) {
            _errorMessage.value = "Vui lòng nhập họ và tên của bạn!"
            return
        }
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
        if (currentPassword != currentConfirm) {
            _errorMessage.value = "Xác nhận mật khẩu không khớp!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = registerUseCase(currentEmail, currentPassword, currentName)
            
            _isLoading.value = false
            result.onSuccess {
                _isRegisterSuccess.value = true
            }
            result.onFailure { exception ->
                _errorMessage.value = exception.localizedMessage ?: "Đăng ký thất bại. Vui lòng kiểm tra lại!"
            }
        }
    }

    fun resetSuccessState() {
        _isRegisterSuccess.value = false
    }
}

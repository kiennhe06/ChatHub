package fpl.ph60001.chathub.presentation.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.usecase.ResetPasswordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lớp đại diện cho Trạng thái giao diện của màn hình Quên mật khẩu.
 */
sealed interface ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState
    object Loading : ForgotPasswordUiState
    object Success : ForgotPasswordUiState
    data class Error(val message: String) : ForgotPasswordUiState
}

/**
 * ViewModel quản lý logic và trạng thái màn hình Quên mật khẩu.
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail
        if (_uiState.value is ForgotPasswordUiState.Error) {
            _uiState.value = ForgotPasswordUiState.Idle
        }
    }

    /**
     * Gửi yêu cầu đặt lại mật khẩu qua email.
     */
    fun sendPasswordReset() {
        val currentEmail = _email.value.trim()
        if (currentEmail.isEmpty()) {
            _uiState.value = ForgotPasswordUiState.Error("Vui lòng nhập địa chỉ email của bạn!")
            return
        }

        viewModelScope.launch {
            _uiState.value = ForgotPasswordUiState.Loading
            val result = resetPasswordUseCase(currentEmail)
            
            result.onSuccess {
                _uiState.value = ForgotPasswordUiState.Success
            }
            result.onFailure { exception ->
                _uiState.value = ForgotPasswordUiState.Error(
                    exception.localizedMessage ?: "Gửi yêu cầu thất bại. Vui lòng thử lại!"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = ForgotPasswordUiState.Idle
    }
}

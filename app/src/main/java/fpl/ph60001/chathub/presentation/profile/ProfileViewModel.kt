package fpl.ph60001.chathub.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lớp ViewModel quản lý hồ sơ thông tin cá nhân của người dùng và cập nhật lên Firebase (ProfileScreen).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Người dùng hiện tại
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    // Trạng thái đang tải dữ liệu (Loading)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Thông báo lỗi tiếng Việt
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Thông báo thành công tiếng Việt
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        _user.value = authRepository.getCurrentUser()
        
        // Lắng nghe cập nhật profile từ Firestore realtime
        viewModelScope.launch {
            authRepository.observeCurrentUser().collect { updatedUser ->
                if (updatedUser != null) {
                    _user.value = updatedUser
                }
            }
        }
    }

    /**
     * Cập nhật thông tin hồ sơ của người dùng.
     */
    fun updateProfile(newDisplayName: String, newPhotoUrl: String) {
        val name = newDisplayName.trim()
        if (name.isEmpty()) {
            _errorMessage.value = "Họ và tên không được để trống!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val result = authRepository.updateProfile(name, newPhotoUrl)
            
            _isLoading.value = false
            result.onSuccess {
                _successMessage.value = "Cập nhật thông tin cá nhân thành công!"
            }
            result.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Lỗi khi cập nhật thông tin!"
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}

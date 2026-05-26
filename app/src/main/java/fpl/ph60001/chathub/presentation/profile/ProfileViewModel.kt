package fpl.ph60001.chathub.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lớp ViewModel quản lý hồ sơ thông tin cá nhân của người dùng, tải ảnh đại diện lên Firebase Storage
 * và cập nhật thông tin cá nhân đồng bộ thời gian thực (ProfileScreen).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
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
     * Tải tệp ảnh đại diện lên Firebase Storage và tự động lưu thay đổi.
     */
    fun uploadAvatar(imageBytes: ByteArray) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val uid = authRepository.getCurrentUser()?.uid ?: "user"
            val fileName = "${uid}_avatar_${System.currentTimeMillis()}.jpg"
            
            val result = chatRepository.uploadChatImage(imageBytes, fileName)
            
            _isLoading.value = false
            result.onSuccess { downloadUrl ->
                // Cập nhật trường photoUrl của user nội bộ trước
                _user.value = _user.value?.copy(photoUrl = downloadUrl)
                // Lưu luôn lên máy chủ Auth/Firestore để đồng bộ mượt mà!
                updateProfile(_user.value?.displayName ?: "", downloadUrl, _user.value?.status ?: "Đang hoạt động")
            }
            result.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Lỗi khi tải ảnh đại diện lên Storage"
            }
        }
    }

    /**
     * Cập nhật thông tin hồ sơ của người dùng.
     */
    fun updateProfile(newDisplayName: String, newPhotoUrl: String, newStatus: String) {
        val name = newDisplayName.trim()
        if (name.isEmpty()) {
            _errorMessage.value = "Họ và tên không được để trống!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val result = authRepository.updateProfile(name, newPhotoUrl, newStatus)
            
            _isLoading.value = false
            result.onSuccess {
                _successMessage.value = "Cập nhật thông tin cá nhân thành công!"
            }
            result.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Lỗi khi cập nhật thông tin!"
            }
        }
    }

    /**
     * Đổi mật khẩu tài khoản người dùng.
     */
    fun updatePassword(newPass: String) {
        val pass = newPass.trim()
        if (pass.length < 6) {
            _errorMessage.value = "Mật khẩu mới phải dài ít nhất 6 ký tự!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val result = authRepository.updatePassword(pass)
            
            _isLoading.value = false
            result.onSuccess {
                _successMessage.value = "Đổi mật khẩu thành công!"
            }
            result.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Đổi mật khẩu thất bại. Bạn có thể cần đăng nhập lại trước khi thực hiện đổi mật khẩu."
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}

package fpl.ph60001.chathub.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.model.Conversation
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.repository.SettingsRepository
import fpl.ph60001.chathub.domain.usecase.GetConversationsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel quản lý logic cấu hình cài đặt của ứng dụng ChatHub.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val getConversationsUseCase: GetConversationsUseCase
) : ViewModel() {

    // Trạng thái Dark mode
    val isDarkMode: StateFlow<Boolean> = settingsRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Trạng thái bật/tắt thông báo toàn cục
    val isNotificationsEnabled: StateFlow<Boolean> = settingsRepository.isNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Map các phòng chat bị tắt tiếng và thời gian hết hạn
    val mutedConversations: StateFlow<Map<String, Long>> = settingsRepository.mutedConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Danh sách cuộc trò chuyện để tắt tiếng
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // Trạng thái tải dữ liệu
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Lỗi hệ thống
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Trạng thái điều hướng về đăng nhập/màn hình ngoài khi Logout hoặc Delete thành công
    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            getConversationsUseCase(uid).collect { list ->
                _conversations.value = list
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    /**
     * Mute cuộc trò chuyện trong khoảng thời gian durationMs.
     */
    fun muteConversation(conversationId: String, durationMs: Long) {
        viewModelScope.launch {
            settingsRepository.muteConversation(conversationId, durationMs)
        }
    }

    /**
     * Unmute cuộc trò chuyện.
     */
    fun unmuteConversation(conversationId: String) {
        viewModelScope.launch {
            settingsRepository.unmuteConversation(conversationId)
        }
    }

    /**
     * Đăng xuất tài khoản.
     */
    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.logout()
            _isLoading.value = false
            result.onSuccess {
                _isLoggedOut.value = true
            }
            result.onFailure {
                _error.value = it.localizedMessage ?: "Đã xảy ra lỗi khi đăng xuất"
            }
        }
    }

    /**
     * Xóa vĩnh viễn tài khoản.
     */
    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.deleteAccount()
            _isLoading.value = false
            result.onSuccess {
                _isLoggedOut.value = true
            }
            result.onFailure {
                _error.value = it.localizedMessage ?: "Xóa tài khoản thất bại. Có thể bạn cần đăng nhập lại trước khi xóa vì lý do bảo mật."
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

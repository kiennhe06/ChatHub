package fpl.ph60001.chathub.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.model.Conversation
import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.usecase.GetConversationsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lớp ViewModel quản lý danh sách cuộc hội thoại thời gian thực, trạng thái người dùng hiện tại và xử lý đăng xuất tại Trang chủ.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getConversationsUseCase: GetConversationsUseCase
) : ViewModel() {

    // Danh sách cuộc hội thoại thời gian thực
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // Trạng thái đang tải dữ liệu
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Người dùng hiện tại đang đăng nhập
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Trạng thái đăng xuất thành công
    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _isLoading.value = true
        
        // Lấy thông tin người dùng đang hoạt động
        val user = authRepository.getCurrentUser()
        _currentUser.value = user
        val currentUserId = user?.uid ?: "demo_user_uid"

        // Lắng nghe realtime danh sách các cuộc hội thoại
        viewModelScope.launch {
            getConversationsUseCase(currentUserId).collect { list ->
                _conversations.value = list
                _isLoading.value = false
            }
        }
    }

    /**
     * Đăng xuất tài khoản người dùng khỏi hệ thống.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _isLoggedOut.value = true
        }
    }

    fun resetLogoutState() {
        _isLoggedOut.value = false
    }
}

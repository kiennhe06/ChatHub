package fpl.ph60001.chathub.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lớp ViewModel quản lý danh sách cuộc hội thoại, lọc tìm kiếm người dùng và xử lý trạng thái trang chủ (HomeScreen).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Danh sách toàn bộ người dùng khác
    private val _usersList = MutableStateFlow<List<User>>(emptyList())

    // Từ khóa tìm kiếm của người dùng
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Trạng thái đang tải dữ liệu
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Người dùng hiện tại đang đăng nhập
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Trạng thái đăng xuất thành công
    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()

    // Luồng dữ liệu danh sách người dùng đã được lọc theo từ khóa tìm kiếm
    val filteredUsers: StateFlow<List<User>> = combine(_usersList, _searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { it.displayName.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        _isLoading.value = true
        
        // Lấy thông tin người dùng đang hoạt động
        _currentUser.value = authRepository.getCurrentUser()

        // Lắng nghe realtime danh sách người dùng
        viewModelScope.launch {
            authRepository.getAllUsers().collect { list ->
                _usersList.value = list
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Đăng xuất tài khoản người dùng.
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

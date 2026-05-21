package fpl.ph60001.chathub.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.usecase.GetOrCreateConversationUseCase
import fpl.ph60001.chathub.domain.usecase.SearchUsersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lớp ViewModel quản lý tìm kiếm người dùng và khởi tạo phòng hội thoại mới trong ChatHub.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val getOrCreateConversationUseCase: GetOrCreateConversationUseCase
) : ViewModel() {

    // Từ khóa tìm kiếm của người dùng
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Danh sách kết quả người dùng tìm được
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    // Trạng thái đang tải dữ liệu tìm kiếm
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Trạng thái thông báo lỗi (nếu có) bằng tiếng Việt
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Thay đổi từ khóa tìm kiếm và thực hiện truy vấn trực tiếp.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            return
        }
        
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            searchUsersUseCase(query)
                .onSuccess { list ->
                    // Không hiển thị chính bản thân người dùng hiện tại trong kết quả tìm kiếm
                    val currentUserId = authRepository.getCurrentUser()?.uid ?: "demo_user_uid"
                    _searchResults.value = list.filter { it.uid != currentUserId }
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.localizedMessage ?: "Lỗi kết nối máy chủ tìm kiếm"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Khởi tạo cuộc hội thoại mới với người dùng được chọn.
     * Khi hoàn thành, gọi hàm callback để chuyển sang phòng chat chi tiết.
     */
    fun startConversation(
        partnerId: String,
        onConversationStarted: (String) -> Unit
    ) {
        val currentUserId = authRepository.getCurrentUser()?.uid ?: "demo_user_uid"
        _isLoading.value = true
        
        viewModelScope.launch {
            getOrCreateConversationUseCase(currentUserId, partnerId)
                .onSuccess { roomId ->
                    _isLoading.value = false
                    onConversationStarted(roomId)
                }
                .onFailure { exception ->
                    _error.value = exception.localizedMessage ?: "Không thể tạo phòng chat"
                    _isLoading.value = false
                }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

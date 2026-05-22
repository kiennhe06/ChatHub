package fpl.ph60001.chathub.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.model.FriendRequest
import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.repository.ChatRepository
import fpl.ph60001.chathub.domain.usecase.GetOrCreateConversationUseCase
import fpl.ph60001.chathub.domain.usecase.SearchUsersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lớp ViewModel quản lý tìm kiếm người dùng và gửi/nhận lời mời kết bạn trong ChatHub.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val getOrCreateConversationUseCase: GetOrCreateConversationUseCase
) : ViewModel() {

    val currentUserId: String
        get() = authRepository.getCurrentUser()?.uid ?: "demo_user_uid"

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

    // Lắng nghe luồng danh sách bạn bè thời gian thực
    val friendsList: StateFlow<List<User>> = chatRepository.getFriends(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Lắng nghe các lời mời kết bạn gửi đến
    val incomingRequests: StateFlow<List<FriendRequest>> = chatRepository.getIncomingFriendRequests(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Lắng nghe các lời mời kết bạn gửi đi
    val outgoingRequests: StateFlow<List<FriendRequest>> = chatRepository.getOutgoingFriendRequests(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
     * Xử lý tương tác nút kết bạn: Gửi yêu cầu / Chấp nhận / Hủy yêu cầu / Hủy kết bạn
     */
    fun handleFriendAction(friendId: String) {
        viewModelScope.launch {
            val isFriend = friendsList.value.any { it.uid == friendId }
            if (isFriend) {
                // Đã là bạn bè -> Hủy kết bạn
                chatRepository.removeFriend(currentUserId, friendId)
                return@launch
            }

            val outgoing = outgoingRequests.value.find { it.receiverId == friendId }
            if (outgoing != null) {
                // Đã gửi yêu cầu trước đó -> Hủy yêu cầu
                chatRepository.declineFriendRequest(outgoing.id)
                return@launch
            }

            val incoming = incomingRequests.value.find { it.senderId == friendId }
            if (incoming != null) {
                // Có lời mời đến từ đối phương -> Chấp nhận luôn
                chatRepository.acceptFriendRequest(incoming.id, currentUserId, friendId)
                return@launch
            }

            // Chưa có liên kết nào -> Gửi lời mời kết bạn!
            val currentUser = authRepository.getCurrentUser()
            val senderName = currentUser?.displayName ?: "Người dùng ChatHub"
            val senderAvatar = currentUser?.photoUrl ?: ""
            chatRepository.sendFriendRequest(
                senderId = currentUserId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                receiverId = friendId
            )
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

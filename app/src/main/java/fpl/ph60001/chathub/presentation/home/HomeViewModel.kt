package fpl.ph60001.chathub.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.model.Conversation
import fpl.ph60001.chathub.domain.model.FriendRequest
import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.repository.ChatRepository
import fpl.ph60001.chathub.domain.repository.MessageRepository
import fpl.ph60001.chathub.domain.usecase.GetConversationsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.messaging.FirebaseMessaging
import android.app.Application
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService

/**
 * Lớp ViewModel quản lý danh sách cuộc hội thoại, danh sách bạn bè, lời mời kết bạn và xử lý đăng xuất tại Trang chủ.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
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

    // Tab đang chọn ở trang chủ: 0 -> Tin nhắn, 1 -> Bạn bè
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Danh sách bạn bè thời gian thực
    private val _friendsList = MutableStateFlow<List<User>>(emptyList())
    val friendsList: StateFlow<List<User>> = _friendsList.asStateFlow()

    // Danh sách các lời mời kết bạn gửi đến thời gian thực
    private val _incomingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val incomingRequests: StateFlow<List<FriendRequest>> = _incomingRequests.asStateFlow()

    val currentUserId: String
        get() = authRepository.getCurrentUser()?.uid ?: "demo_user_uid"

    private var isZegoInitialized = false

    init {
        loadData()
        fetchAndSaveFcmToken()
    }
    
    private fun initZegoCloud(user: User) {
        if (isZegoInitialized) return
        val appID: Long = 889575180
        val appSign = "5718908f2cf0eb98c71fdfc496f5bfa801302094dec5587f2d060abc2b86542e"
        val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig()
        
        // Đảm bảo userName không bị rỗng, nếu không ZIM SDK sẽ báo PARAM_INVALID
        val userName = if (user.displayName.isNullOrBlank() || user.displayName == "Thành viên ChatHub") {
            "User_${user.uid.take(5)}"
        } else {
            user.displayName
        }
        
        ZegoUIKitPrebuiltCallInvitationService.init(
            application,
            appID,
            appSign,
            user.uid,
            userName,
            callInvitationConfig
        )
        isZegoInitialized = true
    }
    
    private fun fetchAndSaveFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            viewModelScope.launch {
                authRepository.updateFcmToken(token)
            }
        }
    }

    private fun loadData() {
        _isLoading.value = true
        
        // Lấy thông tin người dùng đang hoạt động
        val user = authRepository.getCurrentUser()
        _currentUser.value = user
        val uid = user?.uid ?: "demo_user_uid"

        // Lắng nghe realtime thông tin người dùng hiện tại từ Firestore để lấy tên thật
        viewModelScope.launch {
            authRepository.observeCurrentUser().collect { updatedUser ->
                if (updatedUser != null) {
                    _currentUser.value = updatedUser
                    initZegoCloud(updatedUser)
                }
            }
        }

        // Lắng nghe realtime danh sách các cuộc hội thoại
        viewModelScope.launch {
            getConversationsUseCase(uid).collect { list ->
                _conversations.value = list
                _isLoading.value = false
            }
        }

        // Lắng nghe realtime danh sách bạn bè
        viewModelScope.launch {
            chatRepository.getFriends(uid).collect { list ->
                _friendsList.value = list
            }
        }

        // Lắng nghe realtime các lời mời kết bạn đến
        viewModelScope.launch {
            chatRepository.getIncomingFriendRequests(uid).collect { list ->
                _incomingRequests.value = list
            }
        }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    /**
     * Đồng ý lời mời kết bạn.
     */
    fun acceptFriendRequest(requestId: String, senderId: String) {
        viewModelScope.launch {
            chatRepository.acceptFriendRequest(requestId, currentUserId, senderId)
        }
    }

    /**
     * Từ chối lời mời kết bạn.
     */
    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            chatRepository.declineFriendRequest(requestId)
        }
    }

    /**
     * Hủy kết bạn với một người dùng.
     */
    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            chatRepository.removeFriend(currentUserId, friendId)
        }
    }

    /**
     * Đăng xuất tài khoản người dùng khỏi hệ thống.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            ZegoUIKitPrebuiltCallInvitationService.unInit()
            _isLoggedOut.value = true
        }
    }

    fun resetLogoutState() {
        _isLoggedOut.value = false
    }

    /**
     * Xóa cuộc trò chuyện và toàn bộ tin nhắn liên quan.
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
            messageRepository.clearMockMessages(conversationId)
        }
    }
}

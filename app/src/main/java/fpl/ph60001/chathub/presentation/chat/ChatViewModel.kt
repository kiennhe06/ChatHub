package fpl.ph60001.chathub.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.model.Message
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lớp ViewModel quản lý luồng hội thoại tin nhắn realtime giữa 2 người dùng (ChatScreen).
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Lấy ID người chat cùng từ Navigation parameters
    val partnerId: String = checkNotNull(savedStateHandle["partnerId"])
    val partnerName: String = checkNotNull(savedStateHandle["partnerName"])

    // Nội dung tin nhắn đang gõ trong ô Input
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    // Trạng thái đang tải dữ liệu hoặc gửi ảnh
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Luồng tin nhắn realtime kết nối với Repository
    val messagesList: StateFlow<List<Message>> = chatRepository.getMessages(partnerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onMessageTextChanged(newText: String) {
        _messageText.value = newText
    }

    /**
     * Gửi tin nhắn văn bản hiện tại.
     */
    fun sendMessage() {
        val content = _messageText.value.trim()
        if (content.isEmpty()) return

        val currentUser = authRepository.getCurrentUser()
        val currentUserId = currentUser?.uid ?: "demo_user_uid"
        val currentUserName = currentUser?.displayName ?: "Tôi"

        // Reset ô nhập text ngay lập tức để tạo cảm giác mượt mà tức thì
        _messageText.value = ""

        viewModelScope.launch {
            chatRepository.sendMessage(
                senderId = currentUserId,
                senderName = currentUserName,
                receiverId = partnerId,
                content = content
            )
        }
    }

    /**
     * Gửi tin nhắn chứa hình ảnh.
     */
    fun sendImageMessage(imageBytes: ByteArray) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val uploadResult = chatRepository.uploadChatImage(imageBytes, fileName)
            
            uploadResult.onSuccess { downloadUrl ->
                val currentUser = authRepository.getCurrentUser()
                val currentUserId = currentUser?.uid ?: "demo_user_uid"
                val currentUserName = currentUser?.displayName ?: "Tôi"
                
                chatRepository.sendMessage(
                    senderId = currentUserId,
                    senderName = currentUserName,
                    receiverId = partnerId,
                    content = "[Hình ảnh]",
                    imageUrl = downloadUrl
                )
            }
            _isLoading.value = false
        }
    }
}

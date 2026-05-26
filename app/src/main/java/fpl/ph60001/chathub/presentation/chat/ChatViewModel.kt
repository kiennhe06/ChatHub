package fpl.ph60001.chathub.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.data.model.UserDto
import fpl.ph60001.chathub.domain.model.Message
import fpl.ph60001.chathub.domain.model.ReplyTo
import fpl.ph60001.chathub.domain.model.UploadState
import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.repository.MessageRepository
import fpl.ph60001.chathub.domain.usecase.GetMessagesUseCase
import fpl.ph60001.chathub.domain.usecase.SendMessageUseCase
import fpl.ph60001.chathub.domain.usecase.UpdateTypingUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Lớp ViewModel quản lý toàn bộ luồng nghiệp vụ của ChatScreen chi tiết,
 * bao gồm gửi/nhận tin nhắn realtime, gửi ảnh/file media, biểu cảm emoji,
 * chỉnh sửa, xóa, phản hồi (reply) và chỉ báo đang gõ (typing indicator).
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val messageRepository: MessageRepository,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val updateTypingUseCase: UpdateTypingUseCase,
    private val firestore: FirebaseFirestore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Lấy ID và Tên đối phương từ tham số điều hướng
    val partnerId: String = checkNotNull(savedStateHandle["partnerId"])
    val partnerName: String = checkNotNull(savedStateHandle["partnerName"])
    val isGroup: Boolean = savedStateHandle["isGroup"] ?: false

    // Lấy UID người dùng hiện tại để phân biệt tin nhắn bên Trái/Phải
    val currentUserUid: String = authRepository.getCurrentUser()?.uid ?: "demo_user_uid"
    private val currentUserName: String = authRepository.getCurrentUser()?.displayName ?: "Tôi"

    // Key phòng chat chuẩn hóa: Nếu là nhóm, conversationId chính là partnerId (groupId)
    val conversationId: String = if (isGroup) partnerId else getRoomKey(currentUserUid, partnerId)

    // Map lưu thông tin các thành viên trong nhóm để hiển thị tên và avatar
    private val _groupMembers = MutableStateFlow<Map<String, User>>(emptyMap())
    val groupMembers: StateFlow<Map<String, User>> = _groupMembers.asStateFlow()

    private val _groupAvatar = MutableStateFlow("")
    val groupAvatar: StateFlow<String> = _groupAvatar.asStateFlow()

    private val _groupNameFlow = MutableStateFlow("")
    val groupNameFlow: StateFlow<String> = _groupNameFlow.asStateFlow()

    // Nội dung ô nhập văn bản
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    // Trạng thái đang tải
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Tin nhắn đang được chọn phản hồi (Reply To)
    private val _replyingTo = MutableStateFlow<Message?>(null)
    val replyingTo: StateFlow<Message?> = _replyingTo.asStateFlow()

    // Tin nhắn đang được chọn chỉnh sửa (Edit Mode)
    private val _editingMessage = MutableStateFlow<Message?>(null)
    val editingMessage: StateFlow<Message?> = _editingMessage.asStateFlow()

    // Danh sách lưu trạng thái tải lên tệp tin
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(-1)
    val uploadProgress: StateFlow<Int> = _uploadProgress.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    // Luồng tin nhắn realtime kết nối với UseCase
    val messagesList: StateFlow<List<Message>> = getMessagesUseCase(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chỉ báo đối phương đang gõ tin nhắn
    val isPartnerTyping: StateFlow<Boolean> = if (isGroup) {
        flowOf(false).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    } else {
        messageRepository.getTypingStatus(conversationId, partnerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    }

    // Công việc định thời gian tự động xóa trạng thái đang gõ phím
    private var typingJob: Job? = null

    init {
        if (isGroup) {
            loadGroupMembers()
        }
    }

    private fun loadGroupMembers() {
        viewModelScope.launch {
            try {
                // Tải danh sách thành viên của cuộc hội thoại
                firestore.collection("conversations")
                    .document(conversationId)
                    .addSnapshotListener { snapshot, error ->
                        if (snapshot != null && snapshot.exists()) {
                            val avatar = snapshot.getString("groupAvatar") ?: ""
                            val name = snapshot.getString("groupName") ?: ""
                            _groupAvatar.value = avatar
                            _groupNameFlow.value = name

                            val members = snapshot.get("members") as? List<String>
                            if (members != null) {
                                viewModelScope.launch {
                                    val list = mutableMapOf<String, User>()
                                    members.chunked(10).forEach { chunk ->
                                        try {
                                            val usersSnapshot = firestore.collection("users")
                                                .whereIn("uid", chunk)
                                                .get()
                                                .await()
                                            usersSnapshot.documents.forEach { doc ->
                                                val user = doc.toObject(UserDto::class.java)?.toDomain()
                                                if (user != null) {
                                                    list[user.uid] = user
                                                }
                                            }
                                        } catch (e: Exception) {
                                            // Lỗi
                                        }
                                    }
                                    _groupMembers.value = list
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                // Lỗi
            }
        }
    }

    /**
     * Lắng nghe sự thay đổi của ô nhập chữ và kích hoạt typing indicator.
     */
    fun onMessageTextChanged(newText: String) {
        _messageText.value = newText
        
        // Kích hoạt trạng thái đang gõ phím của bản thân lên hệ thống
        viewModelScope.launch {
            updateTypingUseCase(conversationId, currentUserUid, true)
        }

        // Tự động tắt trạng thái gõ phím sau 2 giây nếu người dùng không gõ tiếp
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000)
            updateTypingUseCase(conversationId, currentUserUid, false)
        }
    }

    /**
     * Gửi tin nhắn mới (hỗ trợ cả Reply).
     */
    fun sendMessage() {
        val content = _messageText.value.trim()
        if (content.isEmpty()) return

        // Dọn dẹp trạng thái typing
        typingJob?.cancel()
        viewModelScope.launch {
            updateTypingUseCase(conversationId, currentUserUid, false)
        }

        val parentMsg = _replyingTo.value
        val replyToModel = if (parentMsg != null) ReplyTo(id = parentMsg.messageId, content = parentMsg.content) else null

        val newMessage = Message(
            senderId = currentUserUid,
            senderName = currentUserName,
            receiverId = partnerId,
            content = content,
            timestamp = System.currentTimeMillis(),
            type = "text",
            seenBy = listOf(currentUserUid),
            replyTo = replyToModel
        )

        // Reset ô nhập chữ và reply ngay lập tức tạo cảm giác mượt mà tức thì
        _messageText.value = ""
        _replyingTo.value = null

        viewModelScope.launch {
            sendMessageUseCase(conversationId, newMessage)
        }
    }

    /**
     * Gửi tin nhắn ảnh (type = "image").
     * Upload ảnh lên Firebase Storage trước, phát tiến trình %, rồi gửi tin nhắn chứa URL.
     */
    fun sendImageMessage(bytes: ByteArray, fileName: String) {
        _isUploading.value = true
        _uploadProgress.value = 0
        _uploadError.value = null

        viewModelScope.launch {
            messageRepository.uploadMedia(conversationId, "images", bytes, fileName)
                .collect { state ->
                    when (state) {
                        is UploadState.Progress -> {
                            _uploadProgress.value = state.percent
                        }
                        is UploadState.Success -> {
                            _isUploading.value = false
                            _uploadProgress.value = -1

                            val imageMsg = Message(
                                senderId = currentUserUid,
                                senderName = currentUserName,
                                receiverId = partnerId,
                                content = "📷 Hình ảnh",
                                timestamp = System.currentTimeMillis(),
                                type = "image",
                                mediaUrl = state.downloadUrl,
                                seenBy = listOf(currentUserUid)
                            )
                            sendMessageUseCase(conversationId, imageMsg)
                        }
                        is UploadState.Error -> {
                            _isUploading.value = false
                            _uploadProgress.value = -1
                            _uploadError.value = state.message
                        }
                    }
                }
        }
    }

    /**
     * Gửi tin nhắn file đính kèm (type = "file").
     * Upload file lên Firebase Storage, phát tiến trình %, rồi gửi tin nhắn chứa URL + metadata.
     */
    fun sendFileMessage(bytes: ByteArray, fileName: String, fileSize: Long) {
        _isUploading.value = true
        _uploadProgress.value = 0
        _uploadError.value = null

        viewModelScope.launch {
            messageRepository.uploadMedia(conversationId, "files", bytes, fileName)
                .collect { state ->
                    when (state) {
                        is UploadState.Progress -> {
                            _uploadProgress.value = state.percent
                        }
                        is UploadState.Success -> {
                            _isUploading.value = false
                            _uploadProgress.value = -1

                            val fileMsg = Message(
                                senderId = currentUserUid,
                                senderName = currentUserName,
                                receiverId = partnerId,
                                content = "📎 $fileName",
                                timestamp = System.currentTimeMillis(),
                                type = "file",
                                mediaUrl = state.downloadUrl,
                                fileName = fileName,
                                fileSize = fileSize,
                                seenBy = listOf(currentUserUid)
                            )
                            sendMessageUseCase(conversationId, fileMsg)
                        }
                        is UploadState.Error -> {
                            _isUploading.value = false
                            _uploadProgress.value = -1
                            _uploadError.value = state.message
                        }
                    }
                }
        }
    }

    fun clearUploadError() {
        _uploadError.value = null
    }

    /**
     * Thiết lập tin nhắn đang phản hồi.
     */
    fun setReplyingTo(message: Message?) {
        _replyingTo.value = message
        _editingMessage.value = null // Tắt chế độ sửa
    }

    /**
     * Thiết lập tin nhắn đang chỉnh sửa.
     */
    fun setEditingMessage(message: Message?) {
        _editingMessage.value = message
        _replyingTo.value = null // Tắt chế độ reply
        if (message != null) {
            _messageText.value = message.content
        } else {
            _messageText.value = ""
        }
    }

    /**
     * Thực hiện lưu thay đổi nội dung chỉnh sửa tin nhắn.
     */
    fun saveEditMessage() {
        val target = _editingMessage.value ?: return
        val newContent = _messageText.value.trim()
        if (newContent.isNotEmpty() && newContent != target.content) {
            viewModelScope.launch {
                messageRepository.editMessage(conversationId, target.messageId, newContent)
            }
        }
        setEditingMessage(null)
    }

    /**
     * Thu hồi/Xóa tin nhắn.
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(conversationId, messageId)
        }
    }

    /**
     * Xóa vĩnh viễn tin nhắn (cho tất cả mọi người, biến mất khỏi UI).
     */
    fun deleteMessagePermanently(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessagePermanently(conversationId, messageId)
        }
    }

    /**
     * Xóa toàn bộ lịch sử cuộc trò chuyện này.
     */
    fun clearChatHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Lấy tất cả tin nhắn trên Firestore của cuộc trò chuyện này
                val snapshot = firestore.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .get()
                    .await()
                
                // Xóa từng tài liệu bằng batch
                firestore.runBatch { batch ->
                    snapshot.documents.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                }.await()

                // Cập nhật tin nhắn cuối cùng trong cuộc trò chuyện thành rỗng
                firestore.collection("conversations")
                    .document(conversationId)
                    .update(mapOf(
                        "lastMessage" to "",
                        "lastMessageTime" to 0L
                    ))
                    .await()

                // Xóa cả mock messages cục bộ để không tự sinh lại tin nhắn mẫu
                messageRepository.clearMockMessages(conversationId)
            } catch (e: Exception) {
                // Bỏ qua lỗi
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Bày tỏ cảm xúc Emoji lên tin nhắn.
     */
    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            messageRepository.reactToMessage(conversationId, messageId, currentUserUid, emoji)
        }
    }

    /**
     * Đánh dấu Đã xem cho tin nhắn cụ thể.
     */
    fun markMessageAsSeen(messageId: String) {
        viewModelScope.launch {
            messageRepository.markAsSeen(conversationId, messageId, currentUserUid)
        }
    }

    /**
     * Tạo RoomKey phòng chat 1-1 thống nhất.
     */
    private fun getRoomKey(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    override fun onCleared() {
        super.onCleared()
        // Đảm bảo tắt typing khi rời màn hình
        viewModelScope.launch {
            updateTypingUseCase(conversationId, currentUserUid, false)
        }
    }
}

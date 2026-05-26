package fpl.ph60001.chathub.presentation.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.data.model.UserDto
import fpl.ph60001.chathub.domain.model.Conversation
import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.repository.ChatRepository
import fpl.ph60001.chathub.domain.usecase.GetGroupInfoUseCase
import fpl.ph60001.chathub.domain.usecase.ManageGroupMembersUseCase
import fpl.ph60001.chathub.domain.usecase.UpdateGroupInfoUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val getGroupInfoUseCase: GetGroupInfoUseCase,
    private val updateGroupInfoUseCase: UpdateGroupInfoUseCase,
    private val manageGroupMembersUseCase: ManageGroupMembersUseCase,
    private val firestore: FirebaseFirestore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])
    val currentUserId: String = authRepository.getCurrentUser()?.uid ?: "demo_user_uid"

    // Thông tin phòng chat nhóm thời gian thực
    val groupConversation: StateFlow<Conversation?> = getGroupInfoUseCase(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Hồ sơ của tất cả các thành viên trong nhóm
    private val _memberProfiles = MutableStateFlow<List<User>>(emptyList())
    val memberProfiles: StateFlow<List<User>> = _memberProfiles.asStateFlow()

    // Danh sách bạn bè chưa tham gia vào nhóm (để mời thêm)
    private val _nonMemberFriends = MutableStateFlow<List<User>>(emptyList())
    val nonMemberFriends: StateFlow<List<User>> = _nonMemberFriends.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _exitEvent = MutableSharedFlow<Unit>()
    val exitEvent = _exitEvent.asSharedFlow()

    init {
        // Lắng nghe thay đổi members trong groupConversation để tải thông tin hồ sơ của họ
        viewModelScope.launch {
            groupConversation.collect { conv ->
                conv?.let {
                    fetchMemberProfiles(it.members)
                    fetchNonMemberFriends(it.members)
                }
            }
        }
    }

    private fun fetchMemberProfiles(memberIds: List<String>) {
        if (memberIds.isEmpty()) return
        viewModelScope.launch {
            try {
                // Tải trực tiếp thông tin từ Firestore
                val list = mutableListOf<User>()
                // Chia nhỏ để truy vấn whereIn tối đa 10 phần tử của Firestore
                memberIds.chunked(10).forEach { chunk ->
                    val snapshot = firestore.collection("users")
                        .whereIn("uid", chunk)
                        .get()
                        .await()
                    val chunkUsers = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserDto::class.java)?.toDomain()
                    }
                    list.addAll(chunkUsers)
                }
                _memberProfiles.value = list
            } catch (e: Exception) {
                // Fallback nếu offline/demo mode
                _memberProfiles.value = memberIds.map { uid ->
                    User(
                        uid = uid,
                        displayName = if (uid == currentUserId) "Tôi" else "Thành viên nhóm ($uid)",
                        photoUrl = ""
                    )
                }
            }
        }
    }

    private fun fetchNonMemberFriends(memberIds: List<String>) {
        viewModelScope.launch {
            chatRepository.getFriends(currentUserId).collect { friends ->
                _nonMemberFriends.value = friends.filter { friend ->
                    !memberIds.contains(friend.uid)
                }
            }
        }
    }

    fun updateGroupNameAndAvatar(newName: String, newAvatar: String) {
        val name = newName.trim()
        if (name.isEmpty()) {
            _error.value = "Tên nhóm không được để trống!"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = updateGroupInfoUseCase(groupId, name, newAvatar)
            _isLoading.value = false
            result.onSuccess {
                // Cập nhật thành công
            }.onFailure { e ->
                _error.value = e.localizedMessage ?: "Cập nhật thông tin thất bại"
            }
        }
    }

    fun uploadGroupAvatar(imageBytes: ByteArray, onUploaded: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val fileName = "group_avatar_${groupId}_${System.currentTimeMillis()}.jpg"
            val result = chatRepository.uploadChatImage(imageBytes, fileName)
            _isLoading.value = false
            result.onSuccess { url ->
                onUploaded(url)
            }.onFailure { e ->
                _error.value = e.localizedMessage ?: "Không thể tải ảnh nhóm lên"
            }
        }
    }

    fun addMember(memberId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = manageGroupMembersUseCase.addMembers(groupId, listOf(memberId))
            _isLoading.value = false
            result.onFailure { e ->
                _error.value = e.localizedMessage ?: "Thêm thành viên thất bại"
            }
        }
    }

    fun removeMember(memberId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = manageGroupMembersUseCase.removeMember(groupId, memberId)
            _isLoading.value = false
            result.onFailure { e ->
                _error.value = e.localizedMessage ?: "Xóa thành viên thất bại"
            }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = manageGroupMembersUseCase.leaveGroup(groupId, currentUserId)
            _isLoading.value = false
            result.onSuccess {
                _exitEvent.emit(Unit)
            }.onFailure { e ->
                _error.value = e.localizedMessage ?: "Rời nhóm thất bại"
            }
        }
    }

    fun disbandGroup() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = manageGroupMembersUseCase.disbandGroup(groupId)
            _isLoading.value = false
            result.onSuccess {
                _exitEvent.emit(Unit)
            }.onFailure { e ->
                _error.value = e.localizedMessage ?: "Giải tán nhóm thất bại"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

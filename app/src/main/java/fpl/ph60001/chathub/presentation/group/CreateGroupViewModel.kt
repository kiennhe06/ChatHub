package fpl.ph60001.chathub.presentation.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.repository.ChatRepository
import fpl.ph60001.chathub.domain.usecase.CreateGroupUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val createGroupUseCase: CreateGroupUseCase
) : ViewModel() {

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _groupAvatar = MutableStateFlow("https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?w=150") // Default group avatar
    val groupAvatar: StateFlow<String> = _groupAvatar.asStateFlow()

    val currentUserId: String = authRepository.getCurrentUser()?.uid ?: "demo_user_uid"

    // Danh sách bạn bè để mời vào nhóm
    val friendsList: StateFlow<List<User>> = chatRepository.getFriends(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMembers = MutableStateFlow<Set<String>>(emptySet())
    val selectedMembers: StateFlow<Set<String>> = _selectedMembers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _groupCreatedEvent = MutableSharedFlow<Pair<String, String>>() // Pair(groupId, groupName)
    val groupCreatedEvent = _groupCreatedEvent.asSharedFlow()

    fun onGroupNameChanged(name: String) {
        _groupName.value = name
    }

    fun toggleMemberSelection(uid: String) {
        val current = _selectedMembers.value.toMutableSet()
        if (current.contains(uid)) {
            current.remove(uid)
        } else {
            current.add(uid)
        }
        _selectedMembers.value = current
    }

    fun uploadGroupAvatar(imageBytes: ByteArray) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val fileName = "group_avatar_${System.currentTimeMillis()}.jpg"
            val result = chatRepository.uploadChatImage(imageBytes, fileName)
            _isLoading.value = false
            result.onSuccess { url ->
                _groupAvatar.value = url
            }.onFailure { e ->
                _error.value = e.localizedMessage ?: "Không thể tải ảnh nhóm lên"
            }
        }
    }

    fun createGroup() {
        val name = _groupName.value.trim()
        if (name.isEmpty()) {
            _error.value = "Tên nhóm không được để trống!"
            return
        }
        val members = _selectedMembers.value.toList()
        if (members.isEmpty()) {
            _error.value = "Vui lòng chọn ít nhất 1 thành viên khác!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = createGroupUseCase(name, _groupAvatar.value, members)
            _isLoading.value = false
            result.onSuccess { groupId ->
                _groupCreatedEvent.emit(Pair(groupId, name))
            }.onFailure { e ->
                _error.value = e.localizedMessage ?: "Tạo nhóm thất bại"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

package fpl.ph60001.chathub.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fpl.ph60001.chathub.domain.usecase.CheckUserLoggedInUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel quản lý logic kiểm tra trạng thái đăng nhập cho màn hình Splash.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkUserLoggedInUseCase: CheckUserLoggedInUseCase
) : ViewModel() {

    private val _isUserLoggedIn = MutableStateFlow<Boolean?>(null)
    val isUserLoggedIn: StateFlow<Boolean?> = _isUserLoggedIn.asStateFlow()

    /**
     * Thực hiện kiểm tra xem phiên đăng nhập người dùng còn hoạt động hay không.
     */
    fun checkSession() {
        viewModelScope.launch {
            _isUserLoggedIn.value = checkUserLoggedInUseCase()
        }
    }
}

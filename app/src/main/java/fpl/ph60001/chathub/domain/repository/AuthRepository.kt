package fpl.ph60001.chathub.domain.repository

import fpl.ph60001.chathub.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Giao diện định nghĩa các nghiệp vụ Xác thực (Authentication) trong lớp Domain.
 */
interface AuthRepository {
    
    /**
     * Đăng nhập người dùng bằng Email và Mật khẩu.
     */
    suspend fun login(email: String, password: String): Result<User>

    /**
     * Đăng ký tài khoản người dùng mới bằng Email, Mật khẩu và Tên hiển thị.
     */
    suspend fun register(email: String, password: String, displayName: String): Result<User>

    /**
     * Đăng nhập người dùng bằng tài khoản Google (ID Token).
     */
    suspend fun loginWithGoogle(idToken: String): Result<User>

    /**
     * Gửi liên kết đặt lại mật khẩu đến địa chỉ Email.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /**
     * Đăng xuất tài khoản hiện tại.
     */
    suspend fun logout(): Result<Unit>

    /**
     * Lấy thông tin người dùng đang đăng nhập hiện tại dưới dạng đối tượng tĩnh (nếu có).
     */
    fun getCurrentUser(): User?

    /**
     * Lắng nghe cập nhật thông tin người dùng đang hoạt động theo thời gian thực.
     */
    fun observeCurrentUser(): Flow<User?>

    /**
     * Cập nhật thông tin hồ sơ cá nhân (Tên hiển thị, đường dẫn ảnh đại diện và trạng thái).
     */
    suspend fun updateProfile(displayName: String, photoUrl: String, status: String): Result<Unit>

    /**
     * Đổi mật khẩu của người dùng hiện tại.
     */
    suspend fun updatePassword(newPassword: String): Result<Unit>

    /**
     * Xóa vĩnh viễn tài khoản người dùng hiện tại.
     */
    suspend fun deleteAccount(): Result<Unit>

    /**
     * Cập nhật FCM token lên Firestore để nhận thông báo đẩy.
     */
    suspend fun updateFcmToken(token: String): Result<Unit>

    /**
     * Lấy danh sách tất cả người dùng khác để hiển thị trong phòng chat.
     */
    fun getAllUsers(): Flow<List<User>>

    /**
     * Cập nhật trạng thái hoạt động trực tuyến/ngoại tuyến của người dùng.
     */
    suspend fun updateOnlineStatus(isOnline: Boolean): Result<Unit>
}

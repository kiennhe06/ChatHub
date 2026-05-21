package fpl.ph60001.chathub.domain.model

/**
 * Model đại diện cho Người dùng trong lớp nghiệp vụ (Domain Layer).
 *
 * @property uid Mã định danh duy nhất từ Firebase Auth.
 * @property email Địa chỉ email của người dùng.
 * @property displayName Tên hiển thị công khai.
 * @property photoUrl Đường dẫn ảnh đại diện (avatar).
 * @property isOnline Trạng thái trực tuyến (True nếu đang online).
 * @property lastActiveTimestamp Thời gian hoạt động cuối cùng (dạng timestamp).
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "Người dùng ChatHub",
    val photoUrl: String = "",
    val isOnline: Boolean = false,
    val lastActiveTimestamp: Long = 0L
)

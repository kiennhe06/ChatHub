package fpl.ph60001.chathub.data.model

import fpl.ph60001.chathub.domain.model.User

data class UserDto(
    val uid: String = "",
    val hoTen: String = "",
    val email: String = "",
    val anhDaiDien: String = "",
    val trangThai: String = "Ngoại tuyến",
    @field:JvmField // Đảm bảo Firestore đọc đúng thuộc tính Boolean kiểu online
    val online: Boolean = false,
    val lastSeen: Long = 0L,
    val friends: List<String> = emptyList()
) {

    fun toDomain(): User {
        return User(
            uid = uid,
            email = email,
            displayName = hoTen.ifEmpty { "Thành viên ChatHub" },
            photoUrl = anhDaiDien,
            isOnline = online,
            lastActiveTimestamp = lastSeen,
            friends = friends,
            status = trangThai.ifEmpty { "Đang hoạt động" }
        )
    }

    companion object {
        /**
         * Tạo DTO từ Business Model lớp Domain để gửi lên Firestore.
         */
        fun fromDomain(user: User): UserDto {
            return UserDto(
                uid = user.uid,
                hoTen = user.displayName,
                email = user.email,
                anhDaiDien = user.photoUrl,
                trangThai = user.status,
                online = user.isOnline,
                lastSeen = user.lastActiveTimestamp,
                friends = user.friends
            )
        }
    }
}

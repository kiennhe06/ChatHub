package fpl.ph60001.chathub.domain.model

/**
 * Trạng thái tải tệp lên Firebase Storage.
 */
sealed class UploadState {
    /** Đang tải lên, kèm theo phần trăm tiến trình (0-100). */
    data class Progress(val percent: Int) : UploadState()

    /** Tải lên thành công, trả về URL tải xuống. */
    data class Success(val downloadUrl: String) : UploadState()

    /** Tải lên thất bại, trả về thông báo lỗi tiếng Việt. */
    data class Error(val message: String) : UploadState()
}

package fpl.ph60001.chathub.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import fpl.ph60001.chathub.data.model.UserDto
import fpl.ph60001.chathub.domain.model.User
import fpl.ph60001.chathub.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Triển khai lớp nghiệp vụ Xác thực (AuthRepository) kết nối trực tiếp với Firebase Auth và Firestore.
 */
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Đăng nhập không thành công!"))

            val userDocument = firestore.collection("users").document(uid).get().await()
            val userDto = userDocument.toObject(UserDto::class.java)

            if (userDto != null) {
                // Cập nhật trạng thái trực tuyến
                firestore.collection("users").document(uid)
                    .update(mapOf(
                        "online" to true,
                        "trangThai" to "Đang hoạt động",
                        "lastSeen" to System.currentTimeMillis()
                    ))
                    .await()
                Result.success(userDto.copy(online = true, trangThai = "Đang hoạt động").toDomain())
            } else {
                // Tạo mới nếu chưa tồn tại trong Firestore
                val hoTen = email.substringBefore("@")
                val newUser = User(
                    uid = uid,
                    email = email,
                    displayName = hoTen,
                    isOnline = true,
                    lastActiveTimestamp = System.currentTimeMillis()
                )
                firestore.collection("users").document(uid).set(UserDto.fromDomain(newUser)).await()
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi đăng nhập: ${e.localizedMessage}"))
        }
    }

    override suspend fun register(email: String, password: String, displayName: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Đăng ký không thành công!"))

            val newUser = User(
                uid = uid,
                email = email,
                displayName = displayName,
                isOnline = true,
                lastActiveTimestamp = System.currentTimeMillis()
            )

            firestore.collection("users").document(uid).set(UserDto.fromDomain(newUser)).await()
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi đăng ký: ${e.localizedMessage}"))
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Đăng nhập bằng Google thất bại!"))

            val uid = firebaseUser.uid
            val email = firebaseUser.email ?: ""
            val hoTen = firebaseUser.displayName ?: email.substringBefore("@")
            val anhDaiDien = firebaseUser.photoUrl?.toString() ?: ""

            val userDocument = firestore.collection("users").document(uid).get().await()
            var userDto = userDocument.toObject(UserDto::class.java)

            if (userDto == null) {
                val newUser = User(
                    uid = uid,
                    email = email,
                    displayName = hoTen,
                    photoUrl = anhDaiDien,
                    isOnline = true,
                    lastActiveTimestamp = System.currentTimeMillis()
                )
                userDto = UserDto.fromDomain(newUser)
                firestore.collection("users").document(uid).set(userDto).await()
            } else {
                firestore.collection("users").document(uid)
                    .update(mapOf(
                        "online" to true,
                        "trangThai" to "Đang hoạt động",
                        "lastSeen" to System.currentTimeMillis()
                    ))
                    .await()
                userDto = userDto.copy(online = true, trangThai = "Đang hoạt động")
            }

            Result.success(userDto.toDomain())
        } catch (e: Exception) {
            Result.failure(Exception("Đăng nhập Google thất bại: ${e.localizedMessage}"))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi gửi liên kết: ${e.localizedMessage}"))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid
            if (currentUid != null) {
                // Đánh dấu offline trước khi đăng xuất
                firestore.collection("users").document(currentUid)
                    .update(mapOf(
                        "online" to false,
                        "trangThai" to "Ngoại tuyến",
                        "lastSeen" to System.currentTimeMillis()
                    ))
                    .await()
            }
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            // Đảm bảo đăng xuất dù Firestore có lỗi
            auth.signOut()
            Result.success(Unit)
        }
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "Thành viên ChatHub",
            photoUrl = firebaseUser.photoUrl?.toString() ?: ""
        )
    }

    override fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val userDto = snapshot?.toObject(UserDto::class.java)
                trySend(userDto?.toDomain())
            }

        awaitClose { listener.remove() }
    }

    override suspend fun updateProfile(displayName: String, photoUrl: String, status: String): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Chưa đăng nhập"))
            firestore.collection("users").document(currentUid)
                .update(mapOf(
                    "hoTen" to displayName,
                    "anhDaiDien" to photoUrl,
                    "trangThai" to status
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser
                ?: return Result.failure(Exception("Chưa đăng nhập"))
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser
                ?: return Result.failure(Exception("Chưa đăng nhập"))
            val uid = user.uid
            firestore.collection("users").document(uid).delete().await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Chưa đăng nhập"))
            firestore.collection("users").document(currentUid)
                .update("fcmToken", token)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val currentUid = auth.currentUser?.uid

        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    val user = doc.toObject(UserDto::class.java)?.toDomain()
                    if (user?.uid != currentUid) user else null
                } ?: emptyList()
                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun updateOnlineStatus(isOnline: Boolean): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Chưa đăng nhập"))
            firestore.collection("users").document(currentUid)
                .update(mapOf(
                    "online" to isOnline,
                    "lastSeen" to System.currentTimeMillis()
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

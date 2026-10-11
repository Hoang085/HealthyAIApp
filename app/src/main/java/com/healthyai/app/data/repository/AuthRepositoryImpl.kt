package com.healthyai.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.healthyai.app.domain.repository.AuthRepository
import com.healthyai.app.util.runCatchingCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Triển khai [AuthRepository] trên `FirebaseAuth`.
 *
 * Session được Firebase tự lưu (persist) trên thiết bị, nên người dùng đã đăng nhập
 * sẽ được đưa thẳng vào app ở lần mở sau mà không phải nhập lại.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * `displayName` của Firebase; tài khoản tạo trong app **không** đặt trường này nên đa số trường
     * hợp rơi xuống nhánh **phần trước `@` của email**. Trả `null` khi ở chế độ khách (chưa đăng
     * nhập) để màn Trang chủ không hiện tên nào.
     */
    override val currentUserName: String?
        get() = auth.currentUser?.let { user ->
            user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
        }

    override val authState: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        // Firebase gọi listener ngay sau khi đăng ký với trạng thái hiện tại,
        // nên flow luôn phát giá trị đầu tiên mà không cần gọi mạng.
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Result<String> = runCatchingCancellable {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user?.uid ?: error("Firebase không trả về user sau khi đăng nhập")
    }

    override suspend fun signUp(email: String, password: String): Result<String> = runCatchingCancellable {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.uid ?: error("Firebase không trả về user sau khi tạo tài khoản")
    }

    /**
     * `signOut()` của Firebase là thao tác cục bộ (không gọi mạng): xoá phiên đã lưu và phát
     * `AuthStateListener` với `currentUser = null` → [authState] phát `null`.
     */
    override suspend fun signOut(): Result<Unit> = runCatchingCancellable {
        auth.signOut()
    }
}

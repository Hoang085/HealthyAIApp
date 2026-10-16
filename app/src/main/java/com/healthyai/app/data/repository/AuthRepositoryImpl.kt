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

/** Triển khai [AuthRepository] trên FirebaseAuth; session do Firebase tự lưu nên lần mở sau không phải đăng nhập lại. */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    /** Tên hiển thị: displayName Firebase, không có thì lấy phần trước @ của email; null khi chưa đăng nhập. */
    override val currentUserName: String?
        get() = auth.currentUser?.let { user ->
            user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
        }

    override val authState: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        // Firebase gọi listener ngay sau khi đăng ký với trạng thái hiện tại nên flow phát giá trị đầu tiên, không cần gọi mạng.
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

    /** signOut là thao tác cục bộ: xoá phiên và phát AuthStateListener với currentUser = null nên [authState] phát null. */
    override suspend fun signOut(): Result<Unit> = runCatchingCancellable {
        auth.signOut()
    }
}

package com.healthyai.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository cho phiên đăng nhập Firebase Auth (email + mật khẩu).
 *
 * App **không bắt buộc** đăng nhập: người dùng có thể vào ở chế độ khách, khi đó
 * [currentUserId] == null và `SyncWorker` sẽ bỏ qua bước đồng bộ Firestore.
 */
interface AuthRepository {

    /**
     * uid của người dùng đang đăng nhập, `null` nếu chưa đăng nhập.
     * Đọc đồng bộ từ cache của `FirebaseAuth` (không gọi mạng).
     */
    val currentUserId: String?

    /**
     * uid hiện tại, phát lại mỗi khi trạng thái đăng nhập thay đổi.
     * Phát ngay giá trị đầu tiên khi bắt đầu collect.
     */
    val authState: Flow<String?>

    /**
     * Tên hiển thị của người dùng đang đăng nhập — `displayName` của Firebase, nếu trống thì lấy
     * **phần trước `@` của email** (tài khoản tạo trong app không đặt `displayName`).
     *
     * `null` khi chưa đăng nhập (chế độ khách) → UI **không** hiện dòng tên thay vì một tên cứng.
     * Đọc đồng bộ từ cache của `FirebaseAuth` (không gọi mạng).
     */
    val currentUserName: String?

    /** Đăng nhập bằng email + mật khẩu. Trả về uid khi thành công. */
    suspend fun signIn(email: String, password: String): Result<String>

    /** Tạo tài khoản mới bằng email + mật khẩu. Trả về uid khi thành công. */
    suspend fun signUp(email: String, password: String): Result<String>

    /**
     * Đăng xuất: xoá phiên Firebase đã lưu trên máy (mục 3 #19).
     *
     * Sau khi gọi, [authState] phát `null` và [currentUserId] trả `null` → `AuthGateViewModel`
     * đưa người dùng về màn đăng nhập. **Không** xoá dữ liệu cục bộ (Room/SQLCipher).
     */
    suspend fun signOut(): Result<Unit>
}

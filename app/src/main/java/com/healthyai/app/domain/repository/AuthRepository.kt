package com.healthyai.app.domain.repository

import kotlinx.coroutines.flow.Flow

/** Repository cho phiên đăng nhập Firebase Auth; app dùng được ở chế độ khách khi chưa đăng nhập. */
interface AuthRepository {

    /** uid người dùng đang đăng nhập, `null` nếu chưa; đọc đồng bộ từ cache (không gọi mạng). */
    val currentUserId: String?

    /** uid hiện tại, phát lại mỗi khi trạng thái đăng nhập đổi; phát giá trị đầu tiên ngay khi collect. */
    val authState: Flow<String?>

    /** Tên hiển thị: `displayName`, nếu trống lấy phần trước `@` của email; `null` khi chưa đăng nhập. */
    val currentUserName: String?

    /** Đăng nhập bằng email + mật khẩu. Trả về uid khi thành công. */
    suspend fun signIn(email: String, password: String): Result<String>

    /** Tạo tài khoản mới bằng email + mật khẩu. Trả về uid khi thành công. */
    suspend fun signUp(email: String, password: String): Result<String>

    /** Đăng xuất: xoá phiên Firebase trên máy, không xoá dữ liệu cục bộ (Room/SQLCipher). */
    suspend fun signOut(): Result<Unit>
}

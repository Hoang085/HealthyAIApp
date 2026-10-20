package com.healthyai.app.presentation.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit test JVM cho màu "nền / thẻ" của theme. */
class ThemeSurfaceColorsTest {

    @Test
    fun `nền thẻ khác nền màn hình ở cả chế độ sáng và tối`() {
        assertNotEquals(BackgroundLight, SurfaceContainerLowLight)
        assertNotEquals(BackgroundDark, SurfaceContainerLowDark)
    }

    @Test
    fun `nền thẻ luôn sáng hơn nền màn hình để thẻ còn nổi lên`() {
        assertTrue(
            "chế độ sáng: nền thẻ phải sáng hơn nền màn hình",
            luminance(SurfaceContainerLowLight) > luminance(BackgroundLight)
        )
        assertTrue(
            "chế độ tối: nền thẻ phải sáng hơn nền màn hình",
            luminance(SurfaceContainerLowDark) > luminance(BackgroundDark)
        )
    }

    @Test
    fun `chế độ tối không còn dùng thẻ trắng`() {
        assertNotEquals(Color.White, SurfaceContainerLowDark)
        assertTrue(
            "nền thẻ ở chế độ tối phải đủ tối (độ sáng tương đối < 0.2)",
            luminance(SurfaceContainerLowDark) < 0.2
        )
    }

    @Test
    fun `chữ trên nền thẻ đạt tương phản WCAG AA ở cả hai chế độ`() {
        assertTrue(
            "chế độ sáng",
            contrast(OnSurfaceLight, SurfaceContainerLowLight) >= 4.5
        )
        assertTrue(
            "chế độ tối",
            contrast(OnSurfaceDark, SurfaceContainerLowDark) >= 4.5
        )
    }

    private companion object {
        /** Độ sáng tương đối theo WCAG 2.x (0.0 = đen, 1.0 = trắng). */
        fun luminance(color: Color): Double {
            fun channel(component: Float): Double {
                val c = component.toDouble()
                return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * channel(color.red) +
                0.7152 * channel(color.green) +
                0.0722 * channel(color.blue)
        }

        /** Tỉ lệ tương phản WCAG giữa hai màu (1.0 = trùng nhau, 21.0 = đen trên trắng). */
        fun contrast(a: Color, b: Color): Double {
            val first = luminance(a)
            val second = luminance(b)
            return (max(first, second) + 0.05) / (min(first, second) + 0.05)
        }
    }
}

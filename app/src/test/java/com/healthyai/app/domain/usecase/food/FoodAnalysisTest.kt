package com.healthyai.app.domain.usecase.food

import com.healthyai.app.domain.model.FoodInsight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho [FoodAnalysis] — luật thuần Kotlin của tính năng "phân tích món ăn bằng AI" (mục 3 #42).
 *
 * Chốt hai nhóm hành vi dễ hỏng:
 * 1. **Prompt** — vì đây là "hợp đồng" với model: phải đòi tiếng Việt, ưu tiên món Việt/Á, phủ trái
 *    cây/đồ uống, bắt ghi "(có thể)" khi không chắc (chống đúng lỗi *"đĩa cơm → pizza"*), và **cấm**
 *    phát ngôn y tế.
 * 2. **[FoodAnalysis.toInsight]** — dữ liệu AI trả về là **không tin cậy**: phải cắt độ dài, bỏ mục
 *    rỗng, bỏ mục trùng, giới hạn số gạch đầu dòng, và thay emoji rác bằng emoji mặc định.
 */
class FoodAnalysisTest {

    // ── Prompt ─────────────────────────────────────────────────

    @Test
    fun `prompt asks for Vietnamese and prioritises Vietnamese or Asian dishes`() {
        val prompt = FoodAnalysis.buildPrompt()

        assertTrue("Prompt phải yêu cầu trả lời tiếng Việt", prompt.contains("TIẾNG VIỆT"))
        assertTrue("Prompt phải ưu tiên món Việt Nam/châu Á", prompt.contains("Việt Nam"))
    }

    @Test
    fun `prompt covers fruits, drinks and snacks — not only main dishes`() {
        val prompt = FoodAnalysis.buildPrompt()

        assertTrue("Prompt phải phủ trái cây", prompt.contains("trái cây"))
        assertTrue("Prompt phải phủ đồ uống", prompt.contains("đồ uống"))
        assertTrue("Prompt phải phủ đồ ăn vặt", prompt.contains("đồ ăn vặt"))
    }

    @Test
    fun `prompt asks for short bullets and tells the model how long they may be`() {
        val prompt = FoodAnalysis.buildPrompt()

        assertTrue("Prompt phải nêu giới hạn ký tự của gạch đầu dòng", prompt.contains("120 ký tự"))
        assertTrue("Prompt phải nói rõ số gạch đầu dòng", prompt.contains("gạch đầu dòng"))
    }

    @Test
    fun `prompt forbids medical claims`() {
        val prompt = FoodAnalysis.buildPrompt()

        assertTrue("Prompt phải cấm chẩn đoán bệnh", prompt.contains("KHÔNG chẩn đoán bệnh"))
        assertTrue("Prompt phải cấm nói về thuốc/chữa bệnh", prompt.contains("thuốc"))
    }

    @Test
    fun `prompt asks to flag uncertain guesses instead of naming a different dish`() {
        val prompt = FoodAnalysis.buildPrompt()

        assertTrue(
            "Chống lỗi \"đĩa cơm ra pizza\": không chắc thì phải ghi kèm \"(có thể)\"",
            prompt.contains("(có thể)")
        )
    }

    // ── toInsight: trường hợp bình thường ──────────────────────

    @Test
    fun `a dish with benefits and cautions is kept as is`() {
        val insight = FoodAnalysis.toInsight(
            isFood = true,
            dishName = "Cơm tấm sườn",
            emoji = "🍚",
            benefits = listOf("Nhiều năng lượng"),
            cautions = listOf("Nhiều dầu mỡ")
        )

        assertEquals("Cơm tấm sườn", insight.dishName)
        assertEquals("🍚", insight.emoji)
        assertEquals(listOf("Nhiều năng lượng"), insight.benefits)
        assertEquals(listOf("Nhiều dầu mỡ"), insight.cautions)
        assertTrue(insight.hasDish)
    }

    @Test
    fun `missing lists do not crash and come back empty`() {
        val insight = FoodAnalysis.toInsight(true, "Phở bò", "🍜", benefits = null, cautions = null)

        assertEquals("Phở bò", insight.dishName)
        assertTrue("Thiếu danh sách thì coi như rỗng", insight.benefits.isEmpty() && insight.cautions.isEmpty())
    }

    // ── toInsight: ảnh không có đồ ăn / không gọi được tên ─────

    @Test
    fun `not food means an empty insight`() {
        val insight = FoodAnalysis.toInsight(
            isFood = false,
            dishName = "Cái bàn",
            emoji = "🪑",
            benefits = listOf("không có"),
            cautions = listOf("không có")
        )

        assertEquals(FoodInsight(), insight)
        assertFalse(insight.hasDish)
    }

    @Test
    fun `a food answer without a dish name counts as not recognized`() {
        val insight = FoodAnalysis.toInsight(true, dishName = "   ", emoji = "🍽️", benefits = listOf("a"), cautions = listOf("b"))

        assertFalse(
            "Thẻ kết quả không có tên món thì không nói lên điều gì → coi như chưa nhận diện được",
            insight.hasDish
        )
    }

    @Test
    fun `a null dish name counts as not recognized`() {
        assertFalse(FoodAnalysis.toInsight(true, null, null, listOf("a"), listOf("b")).hasDish)
    }

    // ── toInsight: dọn dữ liệu bẩn ─────────────────────────────

    @Test
    fun `blank and whitespace bullets are dropped and the rest is trimmed`() {
        val insight = FoodAnalysis.toInsight(
            true, "Cháo", "🥣",
            benefits = listOf("  Giàu chất xơ  ", "", "   ", "Dễ tiêu"),
            cautions = listOf("\t")
        )

        assertEquals(listOf("Giàu chất xơ", "Dễ tiêu"), insight.benefits)
        assertTrue("Toàn gạch đầu dòng rỗng thì mục đó rỗng", insight.cautions.isEmpty())
    }

    @Test
    fun `at most three bullets are kept on each side`() {
        val insight = FoodAnalysis.toInsight(
            true, "Pizza", "🍕",
            benefits = listOf("a", "b", "c", "d", "e"),
            cautions = listOf("1", "2", "3", "4")
        )

        assertEquals(listOf("a", "b", "c"), insight.benefits)
        assertEquals(listOf("1", "2", "3"), insight.cautions)
    }

    @Test
    fun `duplicate bullets are collapsed`() {
        val insight = FoodAnalysis.toInsight(
            true, "Trứng", "🥚",
            benefits = listOf("Giàu đạm", "Giàu đạm", "Dễ chế biến"),
            cautions = emptyList()
        )

        assertEquals(listOf("Giàu đạm", "Dễ chế biến"), insight.benefits)
    }

    @Test
    fun `a too long bullet is cut to the limit`() {
        val insight = FoodAnalysis.toInsight(
            true, "Bún chả", "🍜",
            benefits = listOf("x".repeat(500)),
            cautions = emptyList()
        )

        assertEquals(FoodAnalysis.MAX_BULLET_LENGTH, insight.benefits.single().length)
    }

    @Test
    fun `a too long dish name is cut`() {
        val insight = FoodAnalysis.toInsight(true, "y".repeat(500), "🍜", listOf("a"), listOf("b"))

        assertEquals(FoodAnalysis.MAX_DISH_NAME_LENGTH, insight.dishName.length)
    }

    @Test
    fun `a missing emoji falls back to the default one`() {
        assertEquals(FoodAnalysis.DEFAULT_EMOJI, FoodAnalysis.toInsight(true, "Cơm", null, listOf("a"), listOf("b")).emoji)
        assertEquals(FoodAnalysis.DEFAULT_EMOJI, FoodAnalysis.toInsight(true, "Cơm", "  ", listOf("a"), listOf("b")).emoji)
    }

    @Test
    fun `text placed in the emoji field is replaced by the default emoji`() {
        val insight = FoodAnalysis.toInsight(
            true, "Cơm", "Đây là món cơm rất ngon", listOf("a"), listOf("b")
        )

        assertEquals(
            "Model nhét văn bản vào ô emoji ⇒ không hiển thị văn bản đó lên thẻ kết quả",
            FoodAnalysis.DEFAULT_EMOJI,
            insight.emoji
        )
    }
}

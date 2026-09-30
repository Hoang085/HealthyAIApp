package com.healthyai.app.domain.usecase

import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.SuggestionPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test kho mẹo [SuggestionCatalog]: mỗi danh mục phải có **cả** nhóm mẹo cho lúc "có vấn đề" (CONCERN)
 * **và** lúc "đang ổn" (GENERAL) — điều kiện để feed gợi ý luôn đa dạng, không lặp một thẻ duy nhất.
 */
class SuggestionCatalogTest {

    @Test
    fun `every category has both concern and general tips`() {
        SuggestionCatalog.categories.forEach { category ->
            assertTrue(
                "$category thiếu mẹo CONCERN",
                SuggestionCatalog.concernTips(category).isNotEmpty()
            )
            assertTrue(
                "$category thiếu mẹo GENERAL",
                SuggestionCatalog.generalTips(category).isNotEmpty()
            )
        }
    }

    @Test
    fun `all tips have meaningful content`() {
        SuggestionCatalog.categories.forEach { category ->
            (SuggestionCatalog.concernTips(category) + SuggestionCatalog.generalTips(category)).forEach {
                assertTrue("Tiêu đề không được rỗng", it.title.isNotBlank())
                assertTrue("Nội dung không được rỗng", it.message.isNotBlank())
                assertEquals(category, it.category)
            }
        }
    }

    @Test
    fun `concern tips stand out from maintenance tips`() {
        SuggestionCatalog.categories.forEach { category ->
            assertTrue(
                "$category: mẹo CONCERN phải nổi mức ưu tiên hơn mẹo duy trì",
                SuggestionCatalog.concernTips(category)
                    .any { it.priority.ordinal >= SuggestionPriority.MEDIUM.ordinal }
            )
        }
    }

    @Test
    fun `tip titles are unique across the catalog`() {
        val titles = SuggestionCatalog.categories.flatMap {
            SuggestionCatalog.concernTips(it) + SuggestionCatalog.generalTips(it)
        }.map { it.title }

        assertEquals(titles.size, titles.distinct().size)
    }

    @Test
    fun `every category has at least one action tip`() {
        SuggestionCatalog.categories.forEach { category ->
            assertTrue(
                "$category nên có ít nhất một mẹo kèm nút hành động",
                (SuggestionCatalog.concernTips(category) + SuggestionCatalog.generalTips(category))
                    .any { it.actionLabel != null }
            )
        }
    }

    @Test
    fun `all habit categories are covered by the catalog`() {
        assertEquals(HabitCategory.entries.toList(), SuggestionCatalog.categories)
    }
}

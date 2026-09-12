package ch.no1hardy.orbit7.core.domain.model

import java.time.Instant

/**
 * A spending category.
 *
 * Seeded categories carry a [key] so their name can be localized; user-created ones carry a
 * [customName]. Categories are archived, never hard-deleted, so historical expenses always resolve
 * to a name (`docs/05-architecture.md` §4).
 */
data class Category(
    val id: Long,
    val key: CategoryKey?,
    val customName: String?,
    val iconKey: String,
    val colorToken: String,
    val sortOrder: Int,
    val archivedAt: Instant? = null,
) {
    init {
        require(key != null || customName != null) { "A category needs either a key or a custom name" }
    }

    val isArchived: Boolean get() = archivedAt != null
}

/** The seeded Swiss category set offered during onboarding (`docs/03-screens.md` §10). */
enum class CategoryKey {
    GROCERIES,
    EATING_OUT,
    TRANSPORT,
    HOUSEHOLD,
    HEALTH,
    LEISURE,
    SUBSCRIPTIONS,
    OTHER,
}

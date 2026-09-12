package ch.no1hardy.orbit7.core.designsystem

import androidx.annotation.StringRes
import ch.no1hardy.orbit7.core.domain.model.CategoryKey
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey

/**
 * Names of the seeded categories and of the station's modules.
 *
 * Seeded categories are stored by key and named at the UI edge, so switching the app language
 * renames them; user-created categories keep the name the user typed, in the language they typed it.
 */
@StringRes
fun categoryNameRes(key: CategoryKey): Int =
    when (key) {
        CategoryKey.GROCERIES -> R.string.category_groceries
        CategoryKey.EATING_OUT -> R.string.category_eating_out
        CategoryKey.TRANSPORT -> R.string.category_transport
        CategoryKey.HOUSEHOLD -> R.string.category_household
        CategoryKey.HEALTH -> R.string.category_health
        CategoryKey.LEISURE -> R.string.category_leisure
        CategoryKey.SUBSCRIPTIONS -> R.string.category_subscriptions
        CategoryKey.OTHER -> R.string.category_other
    }

@StringRes
fun moduleNameRes(key: StationModuleKey): Int =
    when (key) {
        StationModuleKey.REACTOR_CORE -> R.string.module_reactor_core
        StationModuleKey.LIFE_SUPPORT -> R.string.module_life_support
        StationModuleKey.HYDROPONICS -> R.string.module_hydroponics
        StationModuleKey.COMMS_ARRAY -> R.string.module_comms_array
        StationModuleKey.OBSERVATION_DECK -> R.string.module_observation_deck
        StationModuleKey.HANGAR -> R.string.module_hangar
        StationModuleKey.CRYO_LAB -> R.string.module_cryo_lab
    }

@StringRes
fun moduleDescriptionRes(key: StationModuleKey): Int =
    when (key) {
        StationModuleKey.REACTOR_CORE -> R.string.module_reactor_core_desc
        StationModuleKey.LIFE_SUPPORT -> R.string.module_life_support_desc
        StationModuleKey.HYDROPONICS -> R.string.module_hydroponics_desc
        StationModuleKey.COMMS_ARRAY -> R.string.module_comms_array_desc
        StationModuleKey.OBSERVATION_DECK -> R.string.module_observation_deck_desc
        StationModuleKey.HANGAR -> R.string.module_hangar_desc
        StationModuleKey.CRYO_LAB -> R.string.module_cryo_lab_desc
    }

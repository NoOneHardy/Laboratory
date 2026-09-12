package ch.no1hardy.orbit7.core.designsystem

/**
 * Stable identifiers for tests (`docs/03-screens.md`, `docs/06-test-strategy.md` §6).
 *
 * Test tags are part of the spec, not an afterthought: UI and screenshot tests assert against these
 * constants and **never** against user-visible text, which is localized and would make the suite
 * break on a translation change.
 */
object TestTags {
    // Screens
    const val BRIDGE = "bridge"
    const val QUICK_ADD = "quickadd"
    const val LOG = "log"
    const val BUDGETS = "budgets"
    const val STATION = "station"
    const val DRAINS = "drains"
    const val CONTRACT_EDITOR = "contract_editor"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
    const val SETTLEMENT_SUMMARY = "settlement_summary"
    const val WIDGET = "widget"
    const val GALLERY = "gallery"

    // Bridge
    const val STATION_VIEWPORT = "bridge_station_viewport"
    const val POWER_READOUT = "bridge_power_readout"
    const val PROVISIONAL_EP = "bridge_provisional_ep"
    const val WEEK_GAUGE = "bridge_week_gauge"
    const val TODAY_CARD = "bridge_today_card"
    const val ZERO_SPEND_MARK = "bridge_zero_spend_mark"
    const val ZERO_SPEND_UNDO = "bridge_zero_spend_undo"
    const val STREAK_CHIP = "bridge_streak_chip"
    const val NEXT_DEADLINE = "bridge_next_deadline"
    const val SETTLEMENT_BANNER = "bridge_settlement_banner"
    const val FAB_QUICK_ADD = "bridge_fab_quick_add"
    const val EMPTY_STATE = "empty_state"

    // Quick Add
    const val AMOUNT_READOUT = "quickadd_amount"
    const val NUMPAD = "quickadd_numpad"

    fun numpadKey(key: String) = "quickadd_key_$key"

    const val CATEGORY_CHIPS = "quickadd_category_chips"

    fun categoryChip(id: Long) = "quickadd_category_$id"

    const val DATE_STEPPER = "quickadd_date"
    const val NOTE_FIELD = "quickadd_note"
    const val SAVE = "quickadd_save"
    const val VALIDATION_MESSAGE = "quickadd_validation"

    // Log
    const val LOG_LIST = "log_list"

    fun logEntry(id: Long) = "log_entry_$id"

    const val LOG_FILTERS = "log_filters"
    const val LOG_CLEAR_FILTERS = "log_clear_filters"
    const val UNDO_SNACKBAR = "undo_snackbar"

    // Budgets
    fun budgetRow(categoryId: Long) = "budget_row_$categoryId"

    const val BUDGET_PERIOD_TOGGLE = "budget_period_toggle"

    fun baselineSuggestion(categoryId: Long) = "budget_suggestion_$categoryId"

    fun suggestionAccept(categoryId: Long) = "budget_suggestion_accept_$categoryId"

    fun suggestionDismiss(categoryId: Long) = "budget_suggestion_dismiss_$categoryId"

    // Station
    fun moduleTile(key: String) = "station_module_$key"

    const val MODULE_DETAIL = "station_module_detail"
    const val POWER_UP = "station_power_up"
    const val NEAREST_GOAL = "station_nearest_goal"

    // Drains
    const val DRAINS_HEADER = "drains_header"

    fun drainRow(id: Long) = "drain_row_$id"

    const val DRAINS_SALVAGED_SECTION = "drains_salvaged"
    const val CONTRACT_CANCEL = "contract_cancel"
    const val CONTRACT_RENEGOTIATE = "contract_renegotiate"
    const val CONTRACT_SAVE = "contract_save"
    const val DERIVED_DEADLINE = "contract_derived_deadline"

    // Reports
    const val REPORTS_LOCKED = "reports_locked"
    const val SETTLEMENT_HISTORY = "reports_history"
    const val SUCCESS_CRITERIA = "reports_success_criteria"

    // Settings
    const val SETTING_REDUCE_EFFECTS = "settings_reduce_effects"
    const val SETTING_REMINDER = "settings_reminder"
    const val SETTING_EXPORT = "settings_export"
    const val SETTING_IMPORT = "settings_import"
    const val SETTING_WIPE = "settings_wipe"

    // Onboarding
    const val ONBOARDING_NEXT = "onboarding_next"
    const val ONBOARDING_SKIP = "onboarding_skip"

    fun onboardingCategory(key: String) = "onboarding_category_$key"

    // Components
    const val PANEL = "component_panel"
    const val READOUT_WINDOW = "component_readout_window"
    const val DIGIT_ROLL = "component_digit_roll"
    const val GAUGE = "component_gauge"
    const val HAZARD_BANNER = "component_hazard_banner"
    const val SWITCH_TOGGLE = "component_switch_toggle"
    const val BUS_LINE = "component_bus_line"
    const val SCANLINE_OVERLAY = "component_scanline_overlay"
}

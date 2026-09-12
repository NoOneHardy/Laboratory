package ch.no1hardy.orbit7.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.component.CategoryChip
import ch.no1hardy.orbit7.core.designsystem.component.HazardBanner
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.component.SwitchToggle
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.model.AppLanguage
import java.time.DayOfWeek
import java.time.LocalTime
import ch.no1hardy.orbit7.core.designsystem.R as CoreR

@Composable
fun SettingsRoute(
    onExport: (String) -> Unit,
    onPickImportFile: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is SettingsEffect.ExportReady) onExport(effect.json)
        }
    }

    SettingsScreen(
        state = state,
        onLanguageChanged = viewModel::onLanguageChanged,
        onFirstDayOfWeekChanged = viewModel::onFirstDayOfWeekChanged,
        onRappenFirstChanged = viewModel::onRappenFirstChanged,
        onDailyReminderChanged = viewModel::onDailyReminderChanged,
        onReminderTimeChanged = viewModel::onReminderTimeChanged,
        onContractRemindersChanged = viewModel::onContractRemindersChanged,
        onReduceEffectsChanged = viewModel::onReduceEffectsChanged,
        onSoundChanged = viewModel::onSoundChanged,
        onExport = viewModel::onExport,
        onImport = onPickImportFile,
        onWipeRequested = viewModel::onWipeRequested,
        onWipeConfirmed = viewModel::onWipeConfirmed,
        onWipeDismissed = viewModel::onWipeDismissed,
    )
}

@Composable
@Suppress("LongParameterList", "LongMethod")
fun SettingsScreen(
    state: SettingsUiState,
    onLanguageChanged: (AppLanguage) -> Unit,
    onFirstDayOfWeekChanged: (DayOfWeek) -> Unit,
    onRappenFirstChanged: (Boolean) -> Unit,
    onDailyReminderChanged: (Boolean) -> Unit,
    onReminderTimeChanged: (LocalTime) -> Unit,
    onContractRemindersChanged: (Boolean) -> Unit,
    onReduceEffectsChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onWipeRequested: () -> Unit,
    onWipeConfirmed: () -> Unit,
    onWipeDismissed: () -> Unit,
) {
    val on = stringResource(CoreR.string.state_on)
    val off = stringResource(CoreR.string.state_off)

    Column(
        modifier =
            Modifier
                .testTag(TestTags.SETTINGS)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Orbit7Theme.shapes.screenGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Panel(caption = stringResource(R.string.settings_language)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    val selected = state.settings.language == language
                    CategoryChip(
                        label = stringResource(languageLabel(language)),
                        selected = selected,
                        onClick = { onLanguageChanged(language) },
                        stateDescription =
                            stringResource(
                                if (selected) CoreR.string.state_selected else CoreR.string.state_not_selected,
                            ),
                        testTag = "settings_language_${language.name}",
                    )
                }
            }
        }

        Panel(caption = stringResource(R.string.settings_week)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY).forEach { day ->
                    val selected = state.settings.firstDayOfWeek == day
                    CategoryChip(
                        label =
                            stringResource(
                                if (day ==
                                    DayOfWeek.MONDAY
                                ) {
                                    R.string.settings_week_monday
                                } else {
                                    R.string.settings_week_sunday
                                },
                            ),
                        selected = selected,
                        onClick = { onFirstDayOfWeekChanged(day) },
                        stateDescription =
                            stringResource(
                                if (selected) CoreR.string.state_selected else CoreR.string.state_not_selected,
                            ),
                        testTag = "settings_week_${day.name}",
                    )
                }
            }
        }

        Panel(caption = stringResource(R.string.settings_input)) {
            SwitchToggle(
                checked = state.settings.rappenFirstInput,
                onCheckedChange = onRappenFirstChanged,
                label = stringResource(R.string.settings_rappen_first),
                stateOn = on,
                stateOff = off,
                testTag = "settings_rappen_first",
            )
        }

        Panel(caption = stringResource(R.string.settings_notifications)) {
            SwitchToggle(
                checked = state.settings.dailyReminderEnabled,
                onCheckedChange = onDailyReminderChanged,
                label = stringResource(R.string.settings_daily_reminder),
                stateOn = on,
                stateOff = off,
                testTag = TestTags.SETTING_REMINDER,
            )
            if (state.settings.dailyReminderEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(18, 20, 21).forEach { hour ->
                        val time = LocalTime.of(hour, 0)
                        val selected = state.settings.dailyReminderTime == time
                        CategoryChip(
                            label = stringResource(R.string.settings_reminder_hour, hour),
                            selected = selected,
                            onClick = { onReminderTimeChanged(time) },
                            stateDescription =
                                stringResource(
                                    if (selected) CoreR.string.state_selected else CoreR.string.state_not_selected,
                                ),
                            testTag = "settings_reminder_$hour",
                        )
                    }
                }
            }
            SwitchToggle(
                checked = state.settings.contractRemindersEnabled,
                onCheckedChange = onContractRemindersChanged,
                label = stringResource(R.string.settings_contract_reminders),
                stateOn = on,
                stateOff = off,
                testTag = "settings_contract_reminders",
            )
        }

        Panel(caption = stringResource(R.string.settings_presentation)) {
            SwitchToggle(
                checked = state.settings.reduceEffects,
                onCheckedChange = onReduceEffectsChanged,
                label = stringResource(R.string.settings_reduce_effects),
                stateOn = on,
                stateOff = off,
                testTag = TestTags.SETTING_REDUCE_EFFECTS,
            )
            Text(
                text = stringResource(R.string.settings_reduce_effects_body),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
            SwitchToggle(
                checked = state.settings.soundEnabled,
                onCheckedChange = onSoundChanged,
                label = stringResource(R.string.settings_sound),
                stateOn = on,
                stateOff = off,
                testTag = "settings_sound",
            )
        }

        Panel(caption = stringResource(R.string.settings_backup)) {
            Text(
                text = stringResource(R.string.settings_backup_body),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
            TextButton(onClick = onExport, modifier = Modifier.testTag(TestTags.SETTING_EXPORT)) {
                Text(stringResource(R.string.settings_export))
            }
            TextButton(onClick = onImport, modifier = Modifier.testTag(TestTags.SETTING_IMPORT)) {
                Text(stringResource(R.string.settings_import))
            }
        }

        Panel(caption = stringResource(R.string.settings_danger)) {
            TextButton(onClick = onWipeRequested, modifier = Modifier.testTag(TestTags.SETTING_WIPE)) {
                Text(stringResource(R.string.settings_wipe), color = Orbit7Theme.colors.danger)
            }
            if (state.confirmingWipe) {
                HazardBanner(
                    title = stringResource(R.string.settings_wipe_confirm_title),
                    detail = stringResource(R.string.settings_wipe_confirm_body),
                    testTag = "settings_wipe_confirm",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onWipeConfirmed, modifier = Modifier.testTag("settings_wipe_confirm_yes")) {
                        Text(stringResource(R.string.settings_wipe_confirm_action), color = Orbit7Theme.colors.danger)
                    }
                    TextButton(onClick = onWipeDismissed) { Text(stringResource(CoreR.string.action_cancel)) }
                }
            }
        }

        // The privacy claim, stated where the user can check it.
        Panel(caption = stringResource(R.string.settings_about)) {
            Text(
                text = stringResource(R.string.settings_about_body),
                style = Orbit7Theme.typography.body,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
        }
    }
}

private fun languageLabel(language: AppLanguage): Int =
    when (language) {
        AppLanguage.SYSTEM -> R.string.settings_language_system
        AppLanguage.DE_CH -> R.string.settings_language_de
        AppLanguage.EN -> R.string.settings_language_en
    }

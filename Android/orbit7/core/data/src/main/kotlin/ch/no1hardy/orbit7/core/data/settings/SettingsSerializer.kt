package ch.no1hardy.orbit7.core.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import ch.no1hardy.orbit7.core.domain.model.AppLanguage
import ch.no1hardy.orbit7.core.domain.model.AppSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * The on-disk shape of [AppSettings].
 *
 * Typed settings, as `docs/05-architecture.md` §1 requires — a schema, defaults and no untyped
 * string keys. It is a serializable data class rather than a protobuf message so that the module
 * needs no code generator of its own; the guarantees that matter (a typed schema, an explicit
 * default, and a versioned document) are the same.
 */
@Serializable
internal data class SettingsDto(
    val version: Int = CURRENT_VERSION,
    val language: String = AppLanguage.SYSTEM.name,
    val currency: String = "CHF",
    val firstDayOfWeek: String = DayOfWeek.MONDAY.name,
    val rappenFirstInput: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderTime: String = "20:00",
    val contractRemindersEnabled: Boolean = true,
    val reduceEffects: Boolean = false,
    val soundEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val lastAcknowledgedSettlementWeekId: String? = null,
    val dismissedSuggestions: Map<String, String> = emptyMap(),
    val notifiedDeadlines: Map<String, List<Int>> = emptyMap(),
    val lastAppUsageOn: String? = null,
) {
    fun toDomain(): AppSettings =
        AppSettings(
            language = runCatching { AppLanguage.valueOf(language) }.getOrDefault(AppLanguage.SYSTEM),
            currency = currency,
            firstDayOfWeek = runCatching { DayOfWeek.valueOf(firstDayOfWeek) }.getOrDefault(DayOfWeek.MONDAY),
            rappenFirstInput = rappenFirstInput,
            dailyReminderEnabled = dailyReminderEnabled,
            dailyReminderTime = LocalTime.parse(dailyReminderTime),
            contractRemindersEnabled = contractRemindersEnabled,
            reduceEffects = reduceEffects,
            soundEnabled = soundEnabled,
            onboardingCompleted = onboardingCompleted,
            lastAcknowledgedSettlementWeekId = lastAcknowledgedSettlementWeekId,
            dismissedSuggestions =
                dismissedSuggestions
                    .mapKeys { it.key.toLong() }
                    .mapValues { LocalDate.parse(it.value) },
            notifiedDeadlines = notifiedDeadlines.mapKeys { it.key.toLong() }.mapValues { it.value.toSet() },
            lastAppUsageOn = lastAppUsageOn?.let(LocalDate::parse),
        )

    companion object {
        const val CURRENT_VERSION = 1

        fun from(settings: AppSettings): SettingsDto =
            SettingsDto(
                version = CURRENT_VERSION,
                language = settings.language.name,
                currency = settings.currency,
                firstDayOfWeek = settings.firstDayOfWeek.name,
                rappenFirstInput = settings.rappenFirstInput,
                dailyReminderEnabled = settings.dailyReminderEnabled,
                dailyReminderTime = settings.dailyReminderTime.toString(),
                contractRemindersEnabled = settings.contractRemindersEnabled,
                reduceEffects = settings.reduceEffects,
                soundEnabled = settings.soundEnabled,
                onboardingCompleted = settings.onboardingCompleted,
                lastAcknowledgedSettlementWeekId = settings.lastAcknowledgedSettlementWeekId,
                dismissedSuggestions =
                    settings.dismissedSuggestions
                        .mapKeys { it.key.toString() }
                        .mapValues { it.value.toString() },
                notifiedDeadlines =
                    settings.notifiedDeadlines
                        .mapKeys { it.key.toString() }
                        .mapValues { it.value.toList() },
                lastAppUsageOn = settings.lastAppUsageOn?.toString(),
            )
    }
}

/** Reads and writes [SettingsDto] as JSON. A corrupt file falls back to defaults, loudly. */
object SettingsSerializer : Serializer<SettingsDto> {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }

    override val defaultValue: SettingsDto = SettingsDto()

    override suspend fun readFrom(input: InputStream): SettingsDto =
        try {
            json.decodeFromString(SettingsDto.serializer(), input.readBytes().decodeToString())
        } catch (malformed: SerializationException) {
            throw CorruptionException("Settings could not be read", malformed)
        }

    override suspend fun writeTo(
        t: SettingsDto,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(SettingsDto.serializer(), t).encodeToByteArray())
    }
}

package ch.no1hardy.orbit7.core.data.settings

import androidx.datastore.core.DataStore
import ch.no1hardy.orbit7.core.domain.model.AppSettings
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<SettingsDto>,
    ) : SettingsRepository {
        override fun observeSettings(): Flow<AppSettings> = dataStore.data.map { it.toDomain() }

        override suspend fun settings(): AppSettings = dataStore.data.first().toDomain()

        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            dataStore.updateData { stored -> SettingsDto.from(transform(stored.toDomain())) }
        }
    }

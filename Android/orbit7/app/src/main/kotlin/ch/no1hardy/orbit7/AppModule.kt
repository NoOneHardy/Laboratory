package ch.no1hardy.orbit7

import ch.no1hardy.orbit7.core.data.notification.Orbit7Notifier
import ch.no1hardy.orbit7.notification.AndroidOrbit7Notifier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/** Bindings that need something only the application module knows about. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindNotifier(implementation: AndroidOrbit7Notifier): Orbit7Notifier

    companion object {
        @Provides
        @Named("appVersion")
        fun provideAppVersion(): String = BuildConfig.VERSION_NAME
    }
}

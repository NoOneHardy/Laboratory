# Room, Hilt and Compose ship their own rules; these are ORBIT-7's own.

# Kotlinx serialization keeps the backup document readable after R8.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class ch.no1hardy.orbit7.core.data.backup.** {
    *** Companion;
}
-keepclasseswithmembers class ch.no1hardy.orbit7.core.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class ch.no1hardy.orbit7.core.data.settings.SettingsDto {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Enum names are the on-disk representation of every status column: never rename them.
-keepclassmembers enum ch.no1hardy.orbit7.core.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

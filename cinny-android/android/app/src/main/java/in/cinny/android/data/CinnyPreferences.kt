package `in`.cinny.android.data

enum class CinnyThemeMode {
    System,
    Light,
    Silver,
    Dark,
    Butter
}

enum class CinnyMessageLayout {
    Modern,
    Compact,
    Bubble
}

enum class CinnyMessageSpacing {
    Compact,
    Normal,
    Relaxed
}

enum class CinnyDateFormat {
    Short,
    Full,
    Numeric
}

data class CinnySettings(
    val themeMode: CinnyThemeMode = CinnyThemeMode.System,
    val messageLayout: CinnyMessageLayout = CinnyMessageLayout.Modern,
    val bubbleOwnRight: Boolean = true,
    val messageSpacing: CinnyMessageSpacing = CinnyMessageSpacing.Normal,
    val use24HourClock: Boolean = true,
    val dateFormat: CinnyDateFormat = CinnyDateFormat.Short,
    val showRoomAvatars: Boolean = true,
    val sortRoomsByActivity: Boolean = true,
    val hideActivity: Boolean = false,
    val markdownEnabled: Boolean = true,
    val enterForNewline: Boolean = false,
    val legacyUsernameColor: Boolean = false,
    val autoLoadMedia: Boolean = true,
    val developerTools: Boolean = false
)

package `in`.cinny.android.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import uniffi.cinny_core.CinnyAuthSession
import java.security.SecureRandom

class SecureSessionStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "cinny_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getOrCreateSqlitePassphrase(): String {
        val existing = prefs.getString(KEY_SQLITE_PASSPHRASE, null)
        if (!existing.isNullOrBlank()) return existing
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        prefs.edit().putString(KEY_SQLITE_PASSPHRASE, encoded).apply()
        return encoded
    }

    fun saveSession(session: CinnyAuthSession) {
        val json = JSONObject().apply {
            put("homeserver_url", session.homeserverUrl)
            put("user_id", session.userId)
            put("device_id", session.deviceId)
            put("access_token", session.accessToken)
            put("refresh_token", session.refreshToken ?: JSONObject.NULL)
            put("sqlite_store_subpath", session.sqliteStoreSubpath)
        }
        prefs.edit().putString(KEY_SESSION_JSON, json.toString()).apply()
    }

    fun loadSession(): CinnyAuthSession? {
        val raw = prefs.getString(KEY_SESSION_JSON, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            CinnyAuthSession(
                homeserverUrl = obj.getString("homeserver_url"),
                userId = obj.getString("user_id"),
                deviceId = obj.getString("device_id"),
                accessToken = obj.getString("access_token"),
                refreshToken = if (obj.isNull("refresh_token")) null else obj.getString("refresh_token"),
                sqliteStoreSubpath = obj.getString("sqlite_store_subpath")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearSession() {
        prefs.edit().remove(KEY_SESSION_JSON).apply()
    }

    fun savePendingSsoHomeserver(homeserverUrl: String?) {
        if (homeserverUrl.isNullOrBlank()) {
            prefs.edit().remove(KEY_PENDING_SSO_HS).apply()
        } else {
            prefs.edit().putString(KEY_PENDING_SSO_HS, homeserverUrl).apply()
        }
    }

    fun loadPendingSsoHomeserver(): String? = prefs.getString(KEY_PENDING_SSO_HS, null)

    fun savePushConfig(endpoint: String?, gatewayUrl: String) {
        prefs.edit().apply {
            if (endpoint.isNullOrBlank()) remove(KEY_PUSH_ENDPOINT) else putString(KEY_PUSH_ENDPOINT, endpoint)
            putString(KEY_PUSH_GATEWAY, gatewayUrl)
        }.apply()
    }

    fun loadPushEndpoint(): String? = prefs.getString(KEY_PUSH_ENDPOINT, null)

    fun loadPushGateway(): String =
        prefs.getString(KEY_PUSH_GATEWAY, "https://up.cinny.in/_matrix/push/v1/notify")
            ?: "https://up.cinny.in/_matrix/push/v1/notify"

    fun loadCinnySettings(): CinnySettings {
        val raw = prefs.getString(KEY_CINNY_SETTINGS, null)
        if (raw.isNullOrBlank()) {
            val legacyTheme = prefs.getString(KEY_THEME_PREF, null)
            val migrated = when (legacyTheme) {
                "Light" -> CinnyThemeMode.Light
                "Dark" -> CinnyThemeMode.Dark
                else -> CinnyThemeMode.System
            }
            return CinnySettings(themeMode = migrated)
        }
        return try {
            val obj = JSONObject(raw)
            CinnySettings(
                themeMode = runCatching {
                    CinnyThemeMode.valueOf(obj.optString("themeMode", CinnyThemeMode.System.name))
                }.getOrDefault(CinnyThemeMode.System),
                messageLayout = runCatching {
                    CinnyMessageLayout.valueOf(obj.optString("messageLayout", CinnyMessageLayout.Modern.name))
                }.getOrDefault(CinnyMessageLayout.Modern),
                bubbleOwnRight = obj.optBoolean("bubbleOwnRight", true),
                messageSpacing = runCatching {
                    CinnyMessageSpacing.valueOf(obj.optString("messageSpacing", CinnyMessageSpacing.Normal.name))
                }.getOrDefault(CinnyMessageSpacing.Normal),
                use24HourClock = obj.optBoolean("use24HourClock", true),
                dateFormat = runCatching {
                    CinnyDateFormat.valueOf(obj.optString("dateFormat", CinnyDateFormat.Short.name))
                }.getOrDefault(CinnyDateFormat.Short),
                showRoomAvatars = obj.optBoolean("showRoomAvatars", true),
                sortRoomsByActivity = obj.optBoolean("sortRoomsByActivity", true),
                hideActivity = obj.optBoolean("hideActivity", false),
                markdownEnabled = obj.optBoolean("markdownEnabled", true),
                enterForNewline = obj.optBoolean("enterForNewline", false),
                legacyUsernameColor = obj.optBoolean("legacyUsernameColor", false),
                autoLoadMedia = obj.optBoolean("autoLoadMedia", true),
                developerTools = obj.optBoolean("developerTools", false)
            )
        } catch (_: Exception) {
            CinnySettings()
        }
    }

    fun saveCinnySettings(settings: CinnySettings) {
        val json = JSONObject().apply {
            put("themeMode", settings.themeMode.name)
            put("messageLayout", settings.messageLayout.name)
            put("bubbleOwnRight", settings.bubbleOwnRight)
            put("messageSpacing", settings.messageSpacing.name)
            put("use24HourClock", settings.use24HourClock)
            put("dateFormat", settings.dateFormat.name)
            put("showRoomAvatars", settings.showRoomAvatars)
            put("sortRoomsByActivity", settings.sortRoomsByActivity)
            put("hideActivity", settings.hideActivity)
            put("markdownEnabled", settings.markdownEnabled)
            put("enterForNewline", settings.enterForNewline)
            put("legacyUsernameColor", settings.legacyUsernameColor)
            put("autoLoadMedia", settings.autoLoadMedia)
            put("developerTools", settings.developerTools)
        }
        prefs.edit().putString(KEY_CINNY_SETTINGS, json.toString()).apply()
    }

    private companion object {
        const val KEY_SQLITE_PASSPHRASE = "sqlite_passphrase"
        const val KEY_SESSION_JSON = "matrix_session_json"
        const val KEY_PENDING_SSO_HS = "pending_sso_homeserver"
        const val KEY_PUSH_ENDPOINT = "unifiedpush_endpoint"
        const val KEY_PUSH_GATEWAY = "push_gateway_url"
        const val KEY_THEME_PREF = "theme_preference"
        const val KEY_CINNY_SETTINGS = "cinny_settings_json"
    }
}

package `in`.cinny.android.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import uniffi.cinny_core.CinnyAuthSession
import java.security.SecureRandom

enum class ThemePreference {
    FollowSystem,
    Light,
    Dark
}

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

    fun loadThemePreference(): ThemePreference {
        val name = prefs.getString(KEY_THEME_PREF, ThemePreference.FollowSystem.name)
        return runCatching { ThemePreference.valueOf(name!!) }.getOrDefault(ThemePreference.FollowSystem)
    }

    fun saveThemePreference(pref: ThemePreference) {
        prefs.edit().putString(KEY_THEME_PREF, pref.name).apply()
    }

    private companion object {
        const val KEY_SQLITE_PASSPHRASE = "sqlite_passphrase"
        const val KEY_SESSION_JSON = "matrix_session_json"
        const val KEY_PENDING_SSO_HS = "pending_sso_homeserver"
        const val KEY_PUSH_ENDPOINT = "unifiedpush_endpoint"
        const val KEY_PUSH_GATEWAY = "push_gateway_url"
        const val KEY_THEME_PREF = "theme_preference"
    }
}

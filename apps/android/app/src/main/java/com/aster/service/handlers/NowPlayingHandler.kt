package com.aster.service.handlers

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import com.aster.data.model.Command
import com.aster.service.AsterNotificationListenerService
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * NowPlayingHandler — reads the currently-playing media (title / artist / source
 * app) from the OS media sessions so a client (the OpenAlly companion face) can
 * react to music WITHOUT holding a notification-listener permission itself.
 *
 * ACCESS (why this lives in Aster, not OpenAlly): `MediaSessionManager
 * .getActiveSessions(component)` is gated on the caller being an *enabled
 * notification listener* (or holding the privileged `MEDIA_CONTENT_CONTROL`).
 * A normal Play-Store app can only satisfy that by declaring
 * `BIND_NOTIFICATION_LISTENER_SERVICE` — which fails Google Play's approved-use
 * list for a decorative feature and can trip Play Protect. Aster is a sideloaded
 * companion that ALREADY ships [AsterNotificationListenerService] with that grant
 * (for its notification tools), so this verb reuses it — **no new permission** —
 * and OpenAlly reads now-playing purely over the `get_now_playing` IPC verb,
 * never touching MediaSessionManager or the sensitive grant.
 *
 * Result (always `success` unless a hard error; the client degrades on each):
 *   { "hasAccess": false }                                   // listener not enabled
 *   { "hasAccess": true, "isPlaying": false }                // nothing playing
 *   { "hasAccess": true, "isPlaying": true,
 *     "title": "...", "artist": "...",
 *     "app": "Spotify", "package": "com.spotify.music" }     // a live track
 */
class NowPlayingHandler(
    private val context: Context
) : CommandHandler {

    companion object {
        private const val TAG = "NowPlayingHandler"
    }

    override fun supportedActions() = listOf("get_now_playing")

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "get_now_playing" -> getNowPlaying()
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun getNowPlaying(): CommandResult {
        // The media-session query is gated on notification-listener access, which
        // Aster's own listener holds. If it isn't connected, report no access (the
        // client shows a "grant access in Aster" hint) rather than failing hard.
        if (!AsterNotificationListenerService.isServiceEnabled()) {
            return CommandResult.success(buildJsonObject { put("hasAccess", false) })
        }

        val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            ?: return notPlaying()

        val component = ComponentName(context, AsterNotificationListenerService::class.java)

        // Prefer a session that is actively PLAYING; otherwise the first one that
        // at least carries metadata (a paused player — reported isPlaying=false).
        val controller: MediaController? = runCatching {
            val controllers = msm.getActiveSessions(component)
            controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: controllers.firstOrNull { it.metadata != null }
        }.onFailure { Log.w(TAG, "getActiveSessions failed", it) }.getOrNull()

        val metadata = controller?.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
        if (controller == null || title == null) return notPlaying()

        val artist = (metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: "").trim()
        val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING

        return CommandResult.success(buildJsonObject {
            put("hasAccess", true)
            put("isPlaying", isPlaying)
            put("title", title)
            put("artist", artist)
            put("app", appLabel(controller.packageName))
            put("package", controller.packageName ?: "")
        })
    }

    private fun notPlaying(): CommandResult = CommandResult.success(
        buildJsonObject {
            put("hasAccess", true)
            put("isPlaying", false)
        }
    )

    /** A human label for the source player ("Spotify"), falling back to the package. */
    private fun appLabel(pkg: String?): String {
        if (pkg.isNullOrEmpty()) return ""
        return runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
    }
}

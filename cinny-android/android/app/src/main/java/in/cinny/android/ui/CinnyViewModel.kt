package `in`.cinny.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import `in`.cinny.android.CinnyApplication
import `in`.cinny.android.data.CinnyDateFormat
import `in`.cinny.android.data.CinnyRepository
import uniffi.cinny_core.RoomCategory
import uniffi.cinny_core.RoomSummary
import uniffi.cinny_core.SpaceNode
import uniffi.cinny_core.TimelineMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CinnyViewModel(application: Application) : AndroidViewModel(application) {
    val repository: CinnyRepository = (application as CinnyApplication).repository

    companion object {
        fun filterRooms(
            rooms: List<RoomSummary>,
            selectedSpaceId: String?,
            query: String
        ): List<RoomSummary> {
            val trimmed = query.trim()
            return rooms.filter { room ->
                val matchesSpace = when (selectedSpaceId) {
                    null -> true
                    "dm" -> room.category == RoomCategory.DIRECT_MESSAGE
                    else -> room.parentSpaceIds.contains(selectedSpaceId)
                }
                val matchesQuery = trimmed.isEmpty() ||
                    room.name.contains(trimmed, ignoreCase = true) ||
                    (room.topic?.contains(trimmed, ignoreCase = true) == true)
                matchesSpace && matchesQuery
            }
        }

        fun sortRooms(
            rooms: List<RoomSummary>,
            sortByActivity: Boolean
        ): List<RoomSummary> {
            return if (sortByActivity) {
                rooms.sortedWith(
                    compareByDescending<RoomSummary> { it.highlightCount }
                        .thenByDescending { it.lastActivityTsMs }
                        .thenByDescending { it.unreadCount }
                        .thenBy { it.name.lowercase(Locale.ROOT) }
                )
            } else {
                rooms.sortedWith(
                    compareBy<RoomSummary> { it.name.lowercase(Locale.ROOT) }
                )
            }
        }

        fun childSpacesFor(spaces: List<SpaceNode>, selectedSpaceId: String?): List<SpaceNode> {
            if (selectedSpaceId == null || selectedSpaceId == "dm") {
                return spaces.filter { it.parentSpaceIds.isEmpty() }
            }
            val selected = spaces.find { it.roomId == selectedSpaceId } ?: return emptyList()
            return spaces.filter { selected.childSpaceIds.contains(it.roomId) }
        }

        fun shouldGroupWithPrevious(
            previous: TimelineMessage?,
            current: TimelineMessage
        ): Boolean {
            if (previous == null) return false
            if (previous.senderId != current.senderId) return false
            if (previous.isRedacted || current.isRedacted) return false
            if (current.replyTo != null) return false
            val deltaMs = if (current.timestampMs >= previous.timestampMs) {
                current.timestampMs - previous.timestampMs
            } else {
                return false
            }
            return deltaMs <= 5u * 60u * 1000u
        }

        fun isSameCalendarDay(tsA: ULong, tsB: ULong): Boolean {
            val calA = Calendar.getInstance().apply { timeInMillis = tsA.toLong() }
            val calB = Calendar.getInstance().apply { timeInMillis = tsB.toLong() }
            return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
                calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
        }

        fun formatTimeLabel(timestampMs: ULong, use24Hour: Boolean): String {
            val pattern = if (use24Hour) "HH:mm" else "h:mm a"
            val fmt = SimpleDateFormat(pattern, Locale.getDefault())
            return fmt.format(Date(timestampMs.toLong()))
        }

        fun formatDateDivider(timestampMs: ULong, dateFormat: CinnyDateFormat): String {
            val pattern = when (dateFormat) {
                CinnyDateFormat.Full -> "EEEE, MMM d, yyyy"
                CinnyDateFormat.Short -> "MMM d, yyyy"
                CinnyDateFormat.Numeric -> "yyyy/MM/dd"
            }
            val fmt = SimpleDateFormat(pattern, Locale.getDefault())
            return fmt.format(Date(timestampMs.toLong()))
        }
    }
}

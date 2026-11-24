package `in`.cinny.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import `in`.cinny.android.CinnyApplication
import `in`.cinny.android.data.CinnyRepository
import uniffi.cinny_core.RoomCategory
import uniffi.cinny_core.RoomSummary
import uniffi.cinny_core.SpaceNode

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

        fun childSpacesFor(spaces: List<SpaceNode>, selectedSpaceId: String?): List<SpaceNode> {
            if (selectedSpaceId == null || selectedSpaceId == "dm") {
                return spaces.filter { it.parentSpaceIds.isEmpty() }
            }
            val selected = spaces.find { it.roomId == selectedSpaceId } ?: return emptyList()
            return spaces.filter { selected.childSpaceIds.contains(it.roomId) }
        }
    }
}

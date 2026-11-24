package `in`.cinny.android

import `in`.cinny.android.ui.CinnyViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import uniffi.cinny_core.RoomCategory
import uniffi.cinny_core.RoomSummary
import uniffi.cinny_core.SpaceNode

class CinnyFilteringTest {
    private val sampleRooms = listOf(
        RoomSummary(
            roomId = "!dev:matrix.org",
            name = "cinny-android",
            topic = "Rust & Compose",
            avatarMxc = null,
            isEncrypted = true,
            isDirect = false,
            category = RoomCategory.SPACE_CHANNEL,
            unreadCount = 2u,
            highlightCount = 0u,
            lastMessagePreview = "UniFFI updated",
            lastMessageSender = "@lin:matrix.org",
            lastActivityTsMs = 200u,
            parentSpaceIds = listOf("!space-rust:matrix.org")
        ),
        RoomSummary(
            roomId = "!dm:matrix.org",
            name = "Ajay",
            topic = null,
            avatarMxc = null,
            isEncrypted = true,
            isDirect = true,
            category = RoomCategory.DIRECT_MESSAGE,
            unreadCount = 1u,
            highlightCount = 1u,
            lastMessagePreview = "Review ready",
            lastMessageSender = "@ajbura:matrix.org",
            lastActivityTsMs = 300u,
            parentSpaceIds = emptyList()
        )
    )

    private val sampleSpaces = listOf(
        SpaceNode(
            roomId = "!space-rust:matrix.org",
            name = "Rust Core",
            topic = null,
            avatarMxc = null,
            childRoomIds = listOf("!dev:matrix.org"),
            childSpaceIds = listOf("!sub-crypto:matrix.org"),
            parentSpaceIds = emptyList(),
            unreadNotifications = 2u,
            highlightCount = 0u
        ),
        SpaceNode(
            roomId = "!sub-crypto:matrix.org",
            name = "Megolm & 4S",
            topic = null,
            avatarMxc = null,
            childRoomIds = emptyList(),
            childSpaceIds = emptyList(),
            parentSpaceIds = listOf("!space-rust:matrix.org"),
            unreadNotifications = 0u,
            highlightCount = 0u
        )
    )

    @Test
    fun filterRooms_handlesSpacesDirectMessagesAndSearch() {
        assertEquals(2, CinnyViewModel.filterRooms(sampleRooms, null, "").size)
        assertEquals(1, CinnyViewModel.filterRooms(sampleRooms, "dm", "").size)
        assertEquals(1, CinnyViewModel.filterRooms(sampleRooms, "!space-rust:matrix.org", "").size)
        assertEquals(1, CinnyViewModel.filterRooms(sampleRooms, null, "compose").size)
        assertEquals(0, CinnyViewModel.filterRooms(sampleRooms, "dm", "compose").size)
    }

    @Test
    fun childSpacesFor_resolvesTopLevelAndNestedSpaces() {
        val roots = CinnyViewModel.childSpacesFor(sampleSpaces, null)
        assertEquals(1, roots.size)
        assertEquals("!space-rust:matrix.org", roots[0].roomId)

        val children = CinnyViewModel.childSpacesFor(sampleSpaces, "!space-rust:matrix.org")
        assertEquals(1, children.size)
        assertEquals("!sub-crypto:matrix.org", children[0].roomId)
    }
}

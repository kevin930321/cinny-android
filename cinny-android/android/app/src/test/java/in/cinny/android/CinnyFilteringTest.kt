package `in`.cinny.android

import `in`.cinny.android.ui.CinnyViewModel
import `in`.cinny.android.ui.theme.mxidColorIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uniffi.cinny_core.MessageDeliveryStatus
import uniffi.cinny_core.RoomCategory
import uniffi.cinny_core.RoomSummary
import uniffi.cinny_core.SpaceNode
import uniffi.cinny_core.TimelineMessage

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
    fun sortRooms_respectsActivityVsAlphabetical() {
        val byActivity = CinnyViewModel.sortRooms(sampleRooms, sortByActivity = true)
        assertEquals("!dm:matrix.org", byActivity[0].roomId)

        val byName = CinnyViewModel.sortRooms(sampleRooms, sortByActivity = false)
        assertEquals("!dm:matrix.org", byName[0].roomId) // Ajay < cinny-android
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

    @Test
    fun mxidColorIndex_isDeterministicAndInRange() {
        val idx = mxidColorIndex("@ajbura:matrix.org")
        assertTrue(idx in 0..7)
        assertEquals(idx, mxidColorIndex("@ajbura:matrix.org"))
    }

    @Test
    fun shouldGroupWithPrevious_groupsSameSenderWithinFiveMinutes() {
        val msg1 = TimelineMessage(
            eventId = "\$e1",
            senderId = "@alice:matrix.org",
            senderName = "Alice",
            senderAvatarMxc = null,
            timestampMs = 1_000_000u,
            body = "Hello",
            formattedHtml = null,
            isEdited = false,
            isRedacted = false,
            isEncrypted = true,
            isUnableToDecrypt = false,
            isOwn = false,
            canReply = true,
            threadRootId = null,
            threadReplyCount = 0u,
            replyTo = null,
            attachment = null,
            reactions = emptyList(),
            deliveryStatus = MessageDeliveryStatus.Sent,
            rawJson = null
        )
        val msg2 = msg1.copy(eventId = "\$e2", timestampMs = 1_120_000u)
        val msg3 = msg1.copy(eventId = "\$e3", timestampMs = 1_500_000u)
        val msgOther = msg1.copy(eventId = "\$e4", senderId = "@bob:matrix.org", timestampMs = 1_050_000u)

        assertTrue(CinnyViewModel.shouldGroupWithPrevious(msg1, msg2))
        assertFalse(CinnyViewModel.shouldGroupWithPrevious(msg1, msg3))
        assertFalse(CinnyViewModel.shouldGroupWithPrevious(msg1, msgOther))
    }
}

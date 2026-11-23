package com.example

import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.ChatRoom
import com.example.data.model.ChatType
import com.example.data.model.DeliveryStatus
import com.example.data.model.MessageType
import com.example.data.model.VisibilityOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmileCryptoAndModelTest {

    @Test
    fun `enum parsing safely falls back on unknown values`() {
        assertEquals(VisibilityOption.CONTACTS, VisibilityOption.fromString("CONTACTS"))
        assertEquals(VisibilityOption.EVERYONE, VisibilityOption.fromString("INVALID"))
        assertEquals(ChatType.GROUP, ChatType.fromString("GROUP"))
        assertEquals(ChatType.DIRECT, ChatType.fromString(null))
        assertEquals(MessageType.AUDIO, MessageType.fromString("AUDIO"))
        assertEquals(DeliveryStatus.READ, DeliveryStatus.fromString("READ"))
        assertEquals(CallType.VIDEO, CallType.fromString("VIDEO"))
        assertEquals(CallStatus.ACTIVE, CallStatus.fromString("ACTIVE"))
        assertEquals(CallStatus.RINGING, CallStatus.fromString("UNKNOWN"))
    }

    @Test
    fun `chat room peerUid returns peer only for direct chats`() {
        val direct = ChatRoom(
            id = "direct_a_b",
            chatType = "DIRECT",
            memberUids = listOf("alice", "bob")
        )
        val group = ChatRoom(
            id = "grp_123",
            chatType = "GROUP",
            memberUids = listOf("alice", "bob", "charlie")
        )

        assertEquals("bob", direct.peerUid("alice"))
        assertEquals("alice", direct.peerUid("bob"))
        assertNull(group.peerUid("alice"))
    }
}

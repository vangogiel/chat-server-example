package io.vangogiel.chat.domain.message

import wvlet.airframe.ulid.ULID

import java.time.Instant
import java.util.UUID

case class Message(id: String = ULID.newULIDString, senderId: UUID, recipientId: UUID, sentAt: Instant, content: String)

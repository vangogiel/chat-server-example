package io.vangogiel.chat.domain.message

import java.time.Instant
import java.util.UUID

case class Message(id: UUID, senderId: UUID, recipientId: UUID, sentAt: Instant, content: String)

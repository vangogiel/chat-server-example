package io.vangogiel.chat.domain

import java.time.Instant
import java.util.UUID

sealed trait CommandRequest

object CommandRequest {
  case class ConfirmDeliveryRequest(messageId: String) extends CommandRequest
  case class GetUndeliveredMessagesRequest(senderUuid: UUID, recipientUuid: UUID)
      extends CommandRequest
  case class SendMessageRequest(
      senderUuid: UUID,
      recipientUuid: UUID,
      content: String,
      sentAt: Instant
  ) extends CommandRequest
}

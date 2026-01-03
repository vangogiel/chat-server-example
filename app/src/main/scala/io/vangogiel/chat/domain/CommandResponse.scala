package io.vangogiel.chat.domain

import io.vangogiel.chat.domain.conversation.ConversationId
import io.vangogiel.chat.domain.message.Message

import java.util.UUID

sealed trait CommandResponse

object CommandResponse {
  case class ConfirmDeliveryResponse(messageId: String) extends CommandResponse
  case class GetUndeliveredMessagesResponse(conversationId: ConversationId, messages: List[Message])
      extends CommandResponse
  case class SendMessageResponseSuccess(
      conversationId: ConversationId,
      messageId: String,
      senderUuid: UUID,
      recipientUuid: UUID
  ) extends CommandResponse
  case class SendMessageRequestFailure(messageId: String) extends CommandResponse
  case class ConfirmDeliveryRequestFailure(messageId: String) extends CommandResponse
}

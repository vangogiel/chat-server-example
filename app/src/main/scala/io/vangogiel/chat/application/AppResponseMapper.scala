package io.vangogiel.chat.application

import io.vangogiel.chat.domain.AppResponse
import io.vangogiel.chat.domain.CommandRequest.SendMessageRequest
import io.vangogiel.chat.domain.CommandResponse.{
  ConfirmDeliveryRequestFailure,
  ConfirmDeliveryResponse,
  GetUndeliveredMessagesResponse,
  SendMessageRequestFailure,
  SendMessageResponseSuccess
}
import io.vangogiel.chat.domain.conversation.{ Conversation, ConversationId }
import io.vangogiel.chat.domain.message.Message

object AppResponseMapper {
  def mapSendMessageResponseToAppResponse(
      conversationId: ConversationId,
      message: Message,
      request: SendMessageRequest
  )(correlationId: String): Boolean => AppResponse = {
    case false =>
      AppResponse(
        correlationId = correlationId,
        payload = SendMessageRequestFailure(message.id)
      )
    case true =>
      AppResponse(
        correlationId = correlationId,
        payload = SendMessageResponseSuccess(
          conversationId = conversationId,
          messageId = message.id,
          senderUuid = request.senderUuid,
          recipientUuid = request.recipientUuid
        )
      )
  }

  def mapUndeliveredMessagesRequestToAppResponse(
      conversation: Conversation
  )(correlationId: String): AppResponse = {
    AppResponse(
      correlationId = correlationId,
      payload = GetUndeliveredMessagesResponse(
        conversationId = conversation.conversationId,
        messages = conversation.messages
      )
    )
  }

  def mapConfirmDeliveryRequestToAppResponse(
      messageId: String,
  )(correlationId: String): Boolean => AppResponse = {
    case false =>
      AppResponse(
        correlationId = correlationId,
        payload = ConfirmDeliveryRequestFailure(messageId)
      )
    case true =>
      AppResponse(
        correlationId = correlationId,
        payload = ConfirmDeliveryResponse(messageId = messageId)
      )
  }
}

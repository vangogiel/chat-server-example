package io.vangogiel.chat.infrastructure.grpc

import cats.implicits.catsSyntaxOptionId
import com.google.protobuf.timestamp.Timestamp
import io.grpc.Status
import io.vangogiel.chat.chat_message_response.ChatStreamResponse.Payload.{ConfirmDeliveryResponse => ConfirmDeliveryResponsePayloadType, ErrorResponse => ErrorResponseType, GetUndeliveredMessagesResponse => GetUndeliveredMessagesResponsePayloadType, SendMessageResponse => SendMessageResponsePayloadType}
import io.vangogiel.chat.chat_message_response.{ChatStreamResponse, ConfirmDeliveryResponse, GetUndeliveredMessagesResponse, SendMessageResponse, ErrorResponse => ErrorResponseProto}
import io.vangogiel.chat.chat_message_request.{ SendMessageRequest => SendMessageRequestProto }
import io.vangogiel.chat.domain.message.{Conversation, ConversationId, Message}
import io.vangogiel.chat.handling_result.HandlingResult
import io.vangogiel.chat.message.{Message => MessageProto}
import wvlet.airframe.ulid.ULID

import java.time.Instant
import java.util.UUID

object ChatProtocolMapper {
  def mapToReceiveMessageStreamResponseProto(correlationId: String, conversation: Conversation): ChatStreamResponse = {
    ChatStreamResponse(
      correlationId = correlationId,
      GetUndeliveredMessagesResponsePayloadType(
        GetUndeliveredMessagesResponse(
          conversationId = conversation.conversationId.value,
          messages = conversation.messages.map { message =>
            MessageProto(
              messageId = message.id,
              senderUuid = message.senderId.toString,
              recipientUuid = message.recipientId.toString,
              content = message.content,
              sentAt = Some(Timestamp(message.sentAt))
            )
          },
        )
      )
    )
  }

  def mapMessageFromProto(value: SendMessageRequestProto): Message = {
    Message(
      id = ULID.newULIDString,
      senderId = UUID.fromString(value.senderUuid),
      recipientId = UUID.fromString(value.recipientUuid),
      sentAt = value.sentAt
        .map(a => Instant.ofEpochSecond(a.seconds, a.nanos.toLong))
        .getOrElse(Instant.now()),
      content = value.content
    )
  }

  def mapToSendMessageResponse(correlationId: String, conversationId: ConversationId, messageId: String, senderUuid: UUID, recipientUuid: UUID, result: HandlingResult): ChatStreamResponse = {
    ChatStreamResponse(
      correlationId = correlationId,
      SendMessageResponsePayloadType(
        SendMessageResponse(conversationId.value, messageId, senderUuid.toString, recipientUuid.toString, result.some)
      )
    )
  }

  def mapToConfirmDeliveryResponse(correlationId: String, messageId: String, result: HandlingResult): ChatStreamResponse = {
    ChatStreamResponse(
      correlationId = correlationId,
      ConfirmDeliveryResponsePayloadType(
        ConfirmDeliveryResponse(messageId, result.some)
      )
    )
  }

  def mapToErrorResponse(correlationId: String, status: Status, message: String): ChatStreamResponse = {
    ChatStreamResponse(
      correlationId = correlationId,
      payload = ErrorResponseType(
        ErrorResponseProto(
          code = status.getCode.value.toString,
          message = message
        )
      )
    )
  }
}

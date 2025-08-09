package io.vangogiel.chat.infrastructure.grpc

import cats.implicits.catsSyntaxOptionId
import com.google.protobuf.timestamp.Timestamp
import io.grpc.Status
import io.grpc.Status.INTERNAL
import io.vangogiel.chat.chat_message_response.ChatStreamResponse.Payload.{ConfirmDeliveryResponse => ConfirmDeliveryResponsePayloadType, ErrorResponse => ErrorResponseType, GetUndeliveredMessagesResponse => GetUndeliveredMessagesResponsePayloadType, SendMessageResponse => SendMessageResponsePayloadType}
import io.vangogiel.chat.chat_message_response.{ChatStreamResponse, ConfirmDeliveryResponse, GetUndeliveredMessagesResponse, SendMessageResponse, ErrorResponse => ErrorResponseProto}
import io.vangogiel.chat.chat_message_request.{ConfirmDeliveryRequest => ConfirmDeliveryRequestProto, GetUndeliveredMessagesRequest => GetUnreadMessagesRequestProto, SendMessageRequest => SendMessageRequestProto}
import io.vangogiel.chat.domain.message.Message
import io.vangogiel.chat.handling_result.HandlingResult
import io.vangogiel.chat.message.{Message => MessageProto}

import java.time.Instant
import java.util.UUID

object ChatProtocolMapper {
  def mapToReceiveMessageStreamResponseProto(correlationId: String, messages: List[Message]): ChatStreamResponse = {
    ChatStreamResponse(
      correlationId = correlationId,
      GetUndeliveredMessagesResponsePayloadType(
        GetUndeliveredMessagesResponse(
          messages = messages.map { message =>
            MessageProto(
              messageUuid = message.id.toString,
              senderUuid = message.senderId.toString,
              recipientUuid = message.recipientId.toString,
              content = message.content,
              sentAt = Some(Timestamp(message.sentAt))
            )
          }
        )
      )
    )
  }

  def mapMessageFromProto(senderId: UUID, recipientId: UUID, value: SendMessageRequestProto): Message = {
    Message(
      id = UUID.randomUUID(),
      senderId = UUID.fromString(value.senderUuid),
      recipientId = UUID.fromString(value.recipientUuid),
      sentAt = value.sentAt
        .map(a => Instant.ofEpochSecond(a.seconds, a.nanos.toLong))
        .getOrElse(Instant.now()),
      content = value.content
    )
  }

  def mapToSendMessageResponse(correlationId: String, messageId: UUID, result: HandlingResult): ChatStreamResponse = {
    ChatStreamResponse(
      correlationId = correlationId,
      SendMessageResponsePayloadType(
        SendMessageResponse(messageId.toString, result.some)
      )
    )
  }

  def mapToConfirmDeliveryResponse(correlationId: String, messageId: UUID, result: HandlingResult): ChatStreamResponse = {
    ChatStreamResponse(
      correlationId = correlationId,
      ConfirmDeliveryResponsePayloadType(
        ConfirmDeliveryResponse(messageId.toString, result.some)
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

package io.vangogiel.chat.infrastructure.grpc

import cats.implicits.catsSyntaxOptionId
import com.google.protobuf.timestamp.Timestamp
import io.vangogiel.chat.chat_message_response.ChatStreamResponse.Payload.{ConfirmDeliveryResponse => ConfirmDeliveryResponsePayloadType, GetUndeliveredMessagesResponse => GetUndeliveredMessagesResponsePayloadType, SendMessageResponse => SendMessageResponsePayloadType}
import io.vangogiel.chat.chat_message_response.{ChatStreamResponse, ConfirmDeliveryResponse, GetUndeliveredMessagesResponse, SendMessageResponse}
import io.vangogiel.chat.chat_message_request.{ConfirmDeliveryRequest => ConfirmDeliveryRequestProto, GetUndeliveredMessagesRequest => GetUnreadMessagesRequestProto, SendMessageRequest => SendMessageRequestProto}
import io.vangogiel.chat.domain.message.Message
import io.vangogiel.chat.handling_result.HandlingResult
import io.vangogiel.chat.message.{Message => MessageProto}

import java.time.Instant
import java.util.UUID

object ChatProtocolMapper {
  def mapToReceiveMessageStreamResponseProto(messages: List[Message]): ChatStreamResponse = {
    ChatStreamResponse(
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

  def mapMessageFromProto(value: SendMessageRequestProto): Message = {
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

  def mapToSendMessageResponse(messageId: UUID, result: HandlingResult): ChatStreamResponse = {
    ChatStreamResponse(
      SendMessageResponsePayloadType(
        SendMessageResponse(messageId.toString, result.some)
      )
    )
  }

  def mapToConfirmDeliveryResponse(messageId: UUID, result: HandlingResult): ChatStreamResponse = {
    ChatStreamResponse(
      ConfirmDeliveryResponsePayloadType(
        ConfirmDeliveryResponse(messageId.toString, result.some)
      )
    )
  }
}

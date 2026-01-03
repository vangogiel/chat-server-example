package io.vangogiel.chat.infrastructure.grpc

import cats.syntax.either._
import cats.implicits.catsSyntaxOptionId
import com.google.protobuf.timestamp.Timestamp
import io.grpc.Status
import io.grpc.Status.Code.INTERNAL
import io.vangogiel.chat.chat_message_request.ChatStreamRequest.Payload.{ConfirmDeliveryRequest => ConfirmDeliveryRequestProto, GetUndeliveredMessagesRequest => GetUndeliveredMessagesRequestProto, SendMessageRequest => SendMessageRequestProto}
import io.vangogiel.chat.chat_message_response.ChatStreamResponse.Payload.{ConfirmDeliveryResponse => ConfirmDeliveryResponsePayloadType, ErrorResponse => ErrorResponseType, GetUndeliveredMessagesResponse => GetUndeliveredMessagesResponsePayloadType, SendMessageResponse => SendMessageResponsePayloadType}
import io.vangogiel.chat.chat_message_response.{ChatStreamResponse, SendMessageResponse, ConfirmDeliveryResponse => ConfirmDeliveryResponseProto, ErrorResponse => ErrorResponseProto, GetUndeliveredMessagesResponse => GetUndeliveredMessagesResponseProto}
import io.vangogiel.chat.chat_message_request.{ChatStreamRequest => ProtoChatStreamRequest}
import io.vangogiel.chat.domain.{AppRequest, AppResponse}
import io.vangogiel.chat.domain.CommandRequest.{ConfirmDeliveryRequest, GetUndeliveredMessagesRequest, SendMessageRequest}
import io.vangogiel.chat.domain.CommandResponse.{ConfirmDeliveryRequestFailure, ConfirmDeliveryResponse, GetUndeliveredMessagesResponse, SendMessageRequestFailure, SendMessageResponseSuccess}
import io.vangogiel.chat.domain.conversation.{Conversation, ConversationId}
import io.vangogiel.chat.domain.message.Message
import io.vangogiel.chat.handling_result.HandlingResult
import io.vangogiel.chat.handling_result.HandlingResult.{Failure, Success}
import io.vangogiel.chat.infrastructure.InvalidRequestException
import io.vangogiel.chat.message.{Message => MessageProto}

import java.time.Instant
import java.util.UUID

object ChatProtocolMapper {
  def mapProtoRequestToAppRequest(
      request: ProtoChatStreamRequest
  ): Either[Throwable, AppRequest] = {
    request.payload match {
      case SendMessageRequestProto(value) =>
        for {
          senderUuid <- parseUuid(value.senderUuid)
          recipientUuid <- parseUuid(value.recipientUuid)
        } yield
          AppRequest(
            correlationId = request.correlationId,
            payload = SendMessageRequest(
              senderUuid = senderUuid,
              recipientUuid = recipientUuid,
              content = value.content,
              sentAt = mapTimestampToInstant(value.sentAt)
            )
          )
      case GetUndeliveredMessagesRequestProto(value) =>
        for {
          senderId <- parseUuid(value.senderUuid)
          recipientId <- parseUuid(value.recipientUuid)
        } yield
          AppRequest(
            correlationId = request.correlationId,
            payload = GetUndeliveredMessagesRequest(senderId, recipientId)
          )
      case ConfirmDeliveryRequestProto(value) =>
        AppRequest(
          correlationId = request.correlationId,
          payload = ConfirmDeliveryRequest(value.messageId)
        ).asRight
      case _ => InvalidRequestException.asLeft
    }
  }

  def mapAppResponseToProto(response: AppResponse): ChatStreamResponse = {
    implicit val correlationId: String = response.correlationId
    response.payload match {
      case ConfirmDeliveryResponse(messageId) =>
        mapToConfirmDeliveryResponse(messageId, successResultHandler)
      case ConfirmDeliveryRequestFailure(messageId) =>
        mapToConfirmDeliveryResponse(messageId, failureResultHandler("Unable to update. Please try later."))
      case GetUndeliveredMessagesResponse(conversationId, messages) =>
        createGetUndeliveredMessagesResponseProto(conversationId, messages)
      case SendMessageResponseSuccess(conversationId, messageId, senderUuid, recipientUuid) =>
        createSendMessageResponseProto(conversationId, messageId, senderUuid, recipientUuid, successResultHandler)
      case SendMessageRequestFailure(messageId) =>
        mapToConfirmDeliveryResponse(messageId, failureResultHandler("Unable to send the message. Please try later."))
    }
  }

  private def createGetUndeliveredMessagesResponseProto(
      conversationId: ConversationId,
      messages: List[Message]
  )(implicit correlationId: String): ChatStreamResponse = {
    ChatStreamResponse(
      correlationId = correlationId,
      GetUndeliveredMessagesResponsePayloadType(
        GetUndeliveredMessagesResponseProto(
          conversationId = conversationId.value,
          messages = messages.map { message =>
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

  private def createSendMessageResponseProto(
      conversationId: ConversationId,
      messageId: String,
      senderUuid: UUID,
      recipientUuid: UUID,
      result: HandlingResult
  )(implicit correlationId: String): ChatStreamResponse = {
    ChatStreamResponse(
      correlationId = correlationId,
      SendMessageResponsePayloadType(
        SendMessageResponse(
          conversationId.value,
          messageId,
          senderUuid.toString,
          recipientUuid.toString,
          result.some
        )
      )
    )
  }

  private def mapToConfirmDeliveryResponse(
      messageId: String,
      result: HandlingResult
  )(implicit correlationId: String): ChatStreamResponse = {
    ChatStreamResponse(
      correlationId = correlationId,
      ConfirmDeliveryResponsePayloadType(
        ConfirmDeliveryResponseProto(messageId, result.some)
      )
    )
  }

  def mapToErrorResponse(
      correlationId: String,
      status: Status,
      message: String
  ): ChatStreamResponse = {
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

  private def parseUuid(raw: String) =
    Either.catchNonFatal(UUID.fromString(raw))

  private def mapTimestampToInstant(timestamp: Option[Timestamp]) = {
    timestamp
      .map(a => Instant.ofEpochSecond(a.seconds, a.nanos.toLong))
      .getOrElse(Instant.now())
  }

  private def successResultHandler = {
    HandlingResult(HandlingResult.Result.Success(Success()))
  }

  private def failureResultHandler(message: String) = {
    HandlingResult(
      HandlingResult.Result.Failure(
        Failure()
          .withCode(INTERNAL.value().toString)
          .withMessage(message)
      )
    )
  }
}

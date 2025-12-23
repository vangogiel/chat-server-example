package io.vangogiel.chat.infrastructure.grpc

import cats.MonadThrow
import cats.effect.kernel.{ Async, Sync }
import cats.implicits._
import fs2.Stream
import fs2.concurrent.Topic
import io.grpc.Status._
import io.grpc.{ Metadata, Status }
import io.vangogiel.chat.application.MessageHandler
import io.vangogiel.chat.chat_message_request.ChatStreamRequest.Payload.{
  ConfirmDeliveryRequest,
  GetUndeliveredMessagesRequest,
  SendMessageRequest
}
import io.vangogiel.chat.chat_message_request.{
  ChatStreamRequest,
  ConfirmDeliveryRequest => ConfirmDeliveryRequestProto,
  GetUndeliveredMessagesRequest => GetUndeliveredMessagesRequestProto,
  SendMessageRequest => SendMessageRequestProto
}
import io.vangogiel.chat.chat_message_response.ChatStreamResponse
import io.vangogiel.chat.chat_service.ChatServiceFs2Grpc
import io.vangogiel.chat.domain.message.ConversationId
import io.vangogiel.chat.handling_result.HandlingResult
import io.vangogiel.chat.handling_result.HandlingResult.{ Failure, Success }
import io.vangogiel.chat.infrastructure.grpc.ChatProtocolMapper.{
  mapMessageFromProto,
  mapToConfirmDeliveryResponse,
  mapToErrorResponse,
  mapToReceiveMessageStreamResponseProto,
  mapToSendMessageResponse
}

import java.util.UUID

class ChatServiceImpl[F[_]: Async](
    messagesHandler: MessageHandler[F]
) extends ChatServiceFs2Grpc[F, Metadata] {

  override def chatStream(
      request: Stream[F, ChatStreamRequest],
      ctx: Metadata
  ): Stream[F, ChatStreamResponse] = {
    for {
      topic <- Stream.eval(Topic[F, ChatStreamResponse])
      incoming = request.flatMap { body =>
        handleRequests(body.payload, topic)(body.correlationId)
      }
      out <- topic.subscribe(1000).concurrently(incoming)
    } yield out
  }

  private def handleRequests(
      payload: ChatStreamRequest.Payload,
      topic: Topic[F, ChatStreamResponse]
  )(correlationId: String) = {
    Stream
      .eval {
        payload match {
          case SendMessageRequest(value) =>
            handleSendMessageRequest(topic, value)(correlationId)
          case GetUndeliveredMessagesRequest(value) =>
            handleUndeliveredMessagesRequest(topic, value)(correlationId)
          case ConfirmDeliveryRequest(value) =>
            handleConfirmDeliveryRequest(topic, value)(correlationId)
          case _ => Async[F].unit
        }
      }
      .handleErrorWith(e => errorHandler(e, topic, correlationId))
  }

  private def handleSendMessageRequest(
      topic: Topic[F, ChatStreamResponse],
      value: SendMessageRequestProto
  )(correlationId: String): F[Unit] = {
    for {
      senderUuid <- parseUuid(value.senderUuid)
      recipientUuid <- parseUuid(value.recipientUuid)
      conversationId <- Sync[F].delay(ConversationId.of(senderUuid, recipientUuid))
      message <- mapMessageFromProto(value).pure[F]
      _ <- messagesHandler
        .addMessage(conversationId, message)
        .map(maybeMessageFailedToSendHandlingResult)
        .map(
          result =>
            mapToSendMessageResponse(
              correlationId,
              conversationId,
              message.id,
              senderUuid,
              recipientUuid,
              result
          )
        )
        .flatMap(topic.publish1)
    } yield ()
  }

  private def handleUndeliveredMessagesRequest(
      topic: Topic[F, ChatStreamResponse],
      value: GetUndeliveredMessagesRequestProto
  )(correlationId: String): F[Unit] = {
    for {
      senderId <- parseUuid(value.senderUuid)
      recipientId <- parseUuid(value.recipientUuid)
      conversation <- messagesHandler.getUndeliveredMessages(ConversationId.of(senderId, recipientId))
      _ <- topic.publish1(mapToReceiveMessageStreamResponseProto(correlationId, conversation))
    } yield ()
  }

  private def handleConfirmDeliveryRequest(
      topic: Topic[F, ChatStreamResponse],
      value: ConfirmDeliveryRequestProto
  )(correlationId: String): F[Unit] = {
    for {
      _ <- messagesHandler
        .markMessageAsDelivered(value.messageId)
        .map(maybeMessageNotFoundHandlingResult)
        .map(result => mapToConfirmDeliveryResponse(correlationId, value.messageId, result))
        .flatMap(topic.publish1)
    } yield ()
  }

  private def parseUuid(raw: String): F[UUID] =
    MonadThrow[F].catchOnly[IllegalArgumentException](UUID.fromString(raw))

  private def maybeMessageNotFoundHandlingResult(result: Boolean): HandlingResult =
    createResultHandler(result, NOT_FOUND, "Message not found")

  private def maybeMessageFailedToSendHandlingResult(result: Boolean): HandlingResult =
    createResultHandler(result, UNKNOWN, "Message failed to send")

  private def createResultHandler(
      result: Boolean,
      status: Status,
      errorMessage: String
  ): HandlingResult = {
    if (result) {
      HandlingResult(HandlingResult.Result.Success(Success()))
    } else {
      HandlingResult(
        HandlingResult.Result.Failure(
          Failure()
            .withCode(status.getCode.value.toString)
            .withMessage(errorMessage)
        )
      )
    }
  }

  private def errorHandler(
      throwable: Throwable,
      topic: Topic[F, ChatStreamResponse],
      correlationId: String
  ) = {
    throwable match {
      case e: IllegalArgumentException =>
        Stream.eval(
          topic.publish1(
            mapToErrorResponse(
              correlationId,
              INVALID_ARGUMENT,
              "Invalid argument: " + e.getLocalizedMessage
            )
          )
        )
      case e: Throwable =>
        Stream.eval(
          topic.publish1(
            mapToErrorResponse(
              correlationId,
              INTERNAL,
              "Internal server error: " + e.getLocalizedMessage
            )
          )
        )
    }
  }
}

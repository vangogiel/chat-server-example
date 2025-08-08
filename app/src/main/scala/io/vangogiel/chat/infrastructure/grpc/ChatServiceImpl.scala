package io.vangogiel.chat.infrastructure.grpc

import cats.effect.kernel.Async
import cats.implicits._
import fs2.Stream
import fs2.concurrent.Topic
import io.grpc.Status._
import io.grpc.{Metadata, Status, StatusRuntimeException}
import io.vangogiel.chat.application.MessageHandler
import io.vangogiel.chat.chat_message_request.ChatStreamRequest.Payload.{ConfirmDeliveryRequest, GetUndeliveredMessagesRequest, SendMessageRequest}
import io.vangogiel.chat.chat_message_request.{ChatStreamRequest, ConfirmDeliveryRequest => ConfirmDeliveryRequestProto, GetUndeliveredMessagesRequest => GetUndeliveredMessagesRequestProto, SendMessageRequest => SendMessageRequestProto}
import io.vangogiel.chat.chat_message_response.ChatStreamResponse
import io.vangogiel.chat.chat_service.ChatServiceFs2Grpc
import io.vangogiel.chat.handling_result.HandlingResult
import io.vangogiel.chat.handling_result.HandlingResult.{Failure, Success}
import io.vangogiel.chat.infrastructure.grpc.ChatProtocolMapper.{mapMessageFromProto, mapToConfirmDeliveryResponse, mapToReceiveMessageStreamResponseProto, mapToSendMessageResponse}

import java.util.UUID

class ChatServiceImpl[F[_]: Async](
    messagesHandler: MessageHandler[F]
) extends ChatServiceFs2Grpc[F, Metadata] {

  override def chatStream(
      request: Stream[F, ChatStreamRequest],
      ctx: Metadata
  ): Stream[F, ChatStreamResponse] = {
    Stream
      .eval(Topic[F, ChatStreamResponse])
      .flatMap { topic =>
        val handleIncoming = request.evalMap { req =>
          req.payload match {
            case SendMessageRequest(value)       => handleSendMessageRequest(topic, value)
            case GetUndeliveredMessagesRequest(value) => handleUndeliveredMessagesRequest(topic, value)
            case ConfirmDeliveryRequest(value)   => handleConfirmDeliveryRequest(topic, value)
            case _                               => Async[F].unit
          }
        }
        topic.subscribe(1000).concurrently(handleIncoming)
      }
  }

  private def handleSendMessageRequest(
      topic: Topic[F, ChatStreamResponse],
      value: SendMessageRequestProto
  ): F[Unit] = {
    for {
      message <- mapMessageFromProto(value).pure[F]
      _ <- messagesHandler
        .addMessage(message)
        .map(maybeMessageFailedToSendHandlingResult)
        .map(result => mapToSendMessageResponse(message.id, result))
        .flatMap(topic.publish1)
    } yield ()
  }

  private def handleUndeliveredMessagesRequest(
      topic: Topic[F, ChatStreamResponse],
      value: GetUndeliveredMessagesRequestProto
  ): F[Unit] = {
    for {
      senderId <- UUID.fromString(value.senderUuid).pure[F]
      recipientId <- UUID.fromString(value.recipientUuid).pure[F]
      messages <- messagesHandler.getUndeliveredMessages(senderId, recipientId)
      _ <- topic.publish1(mapToReceiveMessageStreamResponseProto(messages))
    } yield ()
  }

  private def handleConfirmDeliveryRequest(
      topic: Topic[F, ChatStreamResponse],
      value: ConfirmDeliveryRequestProto
  ): F[Unit] = {
    for {
      messageId <- UUID.fromString(value.messageUuid).pure[F]
      _ <- messagesHandler
        .markMessageAsDelivered(messageId)
        .map(maybeMessageNotFoundHandlingResult)
        .map(result => mapToConfirmDeliveryResponse(messageId, result))
        .flatMap(topic.publish1)
    } yield ()
  }

  private def maybeMessageNotFoundHandlingResult(result: Boolean): HandlingResult =
    createResultHandler(result, NOT_FOUND, "Message not found")

  private  def maybeMessageFailedToSendHandlingResult(result: Boolean): HandlingResult =
    createResultHandler(result, UNKNOWN, "Message failed to send")

  private def createResultHandler(result: Boolean, status: Status, errorMessage: String): HandlingResult = {
    result match {
      case true => HandlingResult(HandlingResult.Result.Success(Success()))
      case false =>
        HandlingResult(
          HandlingResult.Result.Failure(
            Failure()
              .withCode(status.getCode.value.toString)
              .withMessage(errorMessage)
          )
        )
    }
  }
}

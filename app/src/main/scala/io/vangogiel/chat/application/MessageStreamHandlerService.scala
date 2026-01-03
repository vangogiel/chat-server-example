package io.vangogiel.chat.application

import cats.effect.{ Async, Concurrent, Sync }
import cats.implicits._
import fs2.Stream
import fs2.concurrent.Topic
import io.vangogiel.chat.domain.CommandRequest.{
  ConfirmDeliveryRequest,
  GetUndeliveredMessagesRequest,
  SendMessageRequest
}
import io.vangogiel.chat.application.AppResponseMapper._
import io.vangogiel.chat.domain.{ AppRequest, AppResponse, CommandRequest }
import io.vangogiel.chat.domain.conversation.{ Conversation, ConversationId }
import io.vangogiel.chat.domain.message.Message

class MessageStreamHandlerService[F[_]: Async](messagesRepository: MessageRepository[F])
    extends MessageStreamHandler[F] {

  override def processStream(
      in: fs2.Stream[F, AppRequest]
  ): fs2.Stream[F, AppResponse] = {
    for {
      topic <- Stream.eval(Topic[F, AppResponse])
      incoming = in.flatMap { body =>
        handleRequests(body.payload, topic)(body.correlationId)
      }
      out <- topic.subscribe(1000).concurrently(incoming)
    } yield out
  }

  private def handleRequests(
      payload: CommandRequest,
      topic: Topic[F, AppResponse]
  )(correlationId: String) = {
    Stream
      .eval {
        payload match {
          case request: SendMessageRequest =>
            handleSendMessageRequest(topic, request)(correlationId)
          case request: GetUndeliveredMessagesRequest =>
            handleUndeliveredMessagesRequest(topic, request)(correlationId)
          case request: ConfirmDeliveryRequest =>
            handleConfirmDeliveryRequest(topic, request)(correlationId)
          case _ => Async[F].unit
        }
      }
  }

  private def handleSendMessageRequest(
      topic: Topic[F, AppResponse],
      request: SendMessageRequest
  )(correlationId: String): F[Unit] = {
    for {
      conversationId <- Sync[F].delay(ConversationId.of(request.senderUuid, request.recipientUuid))
      message = Message(
        senderId = request.senderUuid,
        recipientId = request.recipientUuid,
        sentAt = request.sentAt,
        content = request.content
      )
      _ <- messagesRepository
        .addMessage(conversationId, message)
        .map(mapSendMessageResponseToAppResponse(conversationId, message, request)(correlationId))
        .flatMap(topic.publish1)
    } yield ()
  }

  private def handleUndeliveredMessagesRequest(
      topic: Topic[F, AppResponse],
      request: GetUndeliveredMessagesRequest
  )(correlationId: String): F[Unit] = {
    for {
      conversationId <- Async[F].delay(ConversationId.of(request.senderUuid, request.recipientUuid))
      _ <- messagesRepository
        .getUndeliveredMessages(conversationId)
        .map(mapUndeliveredMessagesRequestToAppResponse(_)(correlationId))
        .flatMap(topic.publish1)
    } yield ()
  }

  private def handleConfirmDeliveryRequest(
      topic: Topic[F, AppResponse],
      value: ConfirmDeliveryRequest
  )(correlationId: String): F[Unit] = {
    for {
      _ <- messagesRepository
        .markMessageAsDelivered(value.messageId)
        .map(mapConfirmDeliveryRequestToAppResponse(value.messageId)(correlationId))
        .flatMap(topic.publish1)
    } yield ()
  }
}

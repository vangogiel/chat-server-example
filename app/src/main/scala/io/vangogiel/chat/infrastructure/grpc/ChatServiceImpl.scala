package io.vangogiel.chat.infrastructure.grpc

import cats.effect.Concurrent
import cats.effect.kernel.Async
import cats.implicits._
import com.google.protobuf.timestamp.Timestamp
import fs2.Stream
import fs2.concurrent.Topic
import io.grpc.Metadata
import io.grpc.Status._
import io.vangogiel.chat.application.MessageHandler
import io.vangogiel.chat.chat_message_request.ChatStreamRequest
import io.vangogiel.chat.chat_message_request.ChatStreamRequest.Payload.{
  ConfirmDeliveryRequest,
  GetUnreadMessagesRequest
}
import io.vangogiel.chat.chat_message_response.ChatStreamResponse.Payload.{
  ConfirmDeliveryResponse,
  GetUnreadMessagesResponse => GetUnreadMessagesResponsePayloadType
}
import io.vangogiel.chat.chat_message_response.{
  ChatStreamResponse,
  GetUnreadMessagesResponse,
  ConfirmDeliveryResponse => ConfirmDeliveryResponseProto
}
import io.vangogiel.chat.chat_service.ChatServiceFs2Grpc
import io.vangogiel.chat.domain.message.Message
import io.vangogiel.chat.handling_result.HandlingResult
import io.vangogiel.chat.handling_result.HandlingResult.{ Failure, Success }
import io.vangogiel.chat.message.{ Message => MessageProto }

import java.util.UUID

class ChatServiceImpl[F[_]: Async: Concurrent](
    messagesHandler: MessageHandler[F]
) extends ChatServiceFs2Grpc[F, Metadata] {

  private def mapToReceiveMessageStreamResponseProto(messages: List[Message]) = {
    ChatStreamResponse(
      GetUnreadMessagesResponsePayloadType(
        GetUnreadMessagesResponse(
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

  override def chatStream(
      request: Stream[F, ChatStreamRequest],
      ctx: Metadata
  ): Stream[F, ChatStreamResponse] = {
    Stream
      .eval(Topic[F, ChatStreamResponse])
      .flatMap { topic =>
        val handleIncoming = request.evalMap { req =>
          req.payload match {
            case GetUnreadMessagesRequest(value) =>
              for {
                senderId <- UUID.fromString(value.senderUuid).pure[F]
                recipientId <- UUID.fromString(value.recipientUuid).pure[F]
                messages <- messagesHandler.getUndeliveredMessages(senderId, recipientId)
                _ <- topic.publish1(mapToReceiveMessageStreamResponseProto(messages))
              } yield ()
            case ConfirmDeliveryRequest(value) =>
              for {
                messageId <- UUID.fromString(value.messageUuid).pure[F]
                _ <- messagesHandler
                  .markMessageAsDelivered(messageId)
                  .flatMap {
                    case true =>
                      HandlingResult(HandlingResult.Result.Success(Success())).pure[F]
                    case false =>
                      HandlingResult(
                        HandlingResult.Result.Failure(
                          Failure()
                            .withCode(NOT_FOUND.getCode.value.toString)
                            .withMessage("Message not found")
                        )
                      ).pure[F]
                  }
                  .map { result =>
                    ChatStreamResponse(
                      ConfirmDeliveryResponse(
                        ConfirmDeliveryResponseProto(messageId.toString, result.some)
                      )
                    )
                  }
                  .flatMap(topic.publish1)
              } yield ()
            case _ => Async[F].unit
          }
        }
        topic.subscribe(1000).concurrently(handleIncoming)
      }
  }
}

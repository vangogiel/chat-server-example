package io.vangogiel.chat.infrastructure.grpc

import cats.effect.Concurrent
import cats.effect.kernel.Async
import cats.implicits._
import com.google.protobuf.timestamp.Timestamp
import fs2.Stream
import fs2.concurrent.Topic
import io.grpc.Metadata
import io.vangogiel.chat.application.MessageHandler
import io.vangogiel.chat.chat_service.ChatServiceFs2Grpc
import io.vangogiel.chat.domain.message.Message
import io.vangogiel.chat.message.{Message => MessageProto}
import io.vangogiel.chat.receive_message_request.ChatStreamRequest
import io.vangogiel.chat.receive_message_request.ChatStreamRequest.Payload.GetUnreadMessagesRequest
import io.vangogiel.chat.receive_message_stream_response.ChatStreamResponse.Payload.{GetUnreadMessagesResponse => GetUnreadMessagesResponsePayloadType}
import io.vangogiel.chat.receive_message_stream_response.{ChatStreamResponse, GetUnreadMessagesResponse}

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
                _ <- Stream
                  .eval(
                    messagesHandler.getUndeliveredMessages(senderId, recipientId).flatMap {
                      messages =>
                        topic.publish1(mapToReceiveMessageStreamResponseProto(messages))
                    }
                  )
                  .compile
                  .drain
              } yield ()
          }
        }
        topic.subscribe(1000).concurrently(handleIncoming)
      }
  }
}

package io.vangogiel.chat.infrastructure.grpc

import cats.effect.kernel.{ Async, Sync }
import fs2.Stream
import io.grpc.Metadata
import io.grpc.Status._
import io.vangogiel.chat.application.MessageStreamHandler
import io.vangogiel.chat.chat_message_request.{ ChatStreamRequest => ProtoChatStreamRequest }
import io.vangogiel.chat.chat_message_response.{ ChatStreamResponse => ProtoChatStreamResponse }
import io.vangogiel.chat.chat_service.ChatServiceFs2Grpc
import io.vangogiel.chat.infrastructure.grpc.ChatProtocolMapper.{
  mapAppResponseToProto,
  mapProtoRequestToAppRequest,
  mapToErrorResponse
}

class ChatGrpcAdapter[F[_]: Async](
    messageStreamHandler: MessageStreamHandler[F]
) extends ChatServiceFs2Grpc[F, Metadata] {

  override def chatStream(
      in: Stream[F, ProtoChatStreamRequest],
      ctx: Metadata
  ): Stream[F, ProtoChatStreamResponse] = {
    messageStreamHandler
      .processStream(convert(in))
      .map(mapAppResponseToProto)
      .handleErrorWith(e => errorHandler(e))
  }

  private def convert(in: Stream[F, ProtoChatStreamRequest]) = {
    in.evalMap { request =>
      Sync[F]
        .fromEither(mapProtoRequestToAppRequest(request))
    }
  }

  private def errorHandler(
      throwable: Throwable
  ) = {
    Stream.eval(
      Async[F].pure(
        mapToErrorResponse(
          "",
          INTERNAL,
          "Internal server error: " + throwable.getLocalizedMessage
        )
      )
    )
  }
}

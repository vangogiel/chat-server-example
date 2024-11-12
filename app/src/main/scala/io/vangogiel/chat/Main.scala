package io.vangogiel.chat

import cats.effect.kernel.{ Async, Resource }
import cats.effect.{ ExitCode, IO, IOApp }
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.vangogiel.chat.application.{ MessageHandler, UserHandler }
import io.vangogiel.chat.chat_service.ChatServiceFs2Grpc
import io.vangogiel.chat.infrastructure.db.{ InMemoryMessagesRepository, InMemoryUserRepository }
import io.vangogiel.chat.infrastructure.grpc.ChatServiceImpl

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    createServerAndAddServices[IO]()
      .evalMap(server => IO(server.build().start()))
      .useForever
      .map(_ => ExitCode.Success)
  }

  private def createServerAndAddServices[F[_]: Async]() = {
    for {
      messagesRepo <- Resource.eval(InMemoryMessagesRepository[F]())
      usersRepo <- Resource.eval(InMemoryUserRepository[F]())
      messagesHandler = new MessageHandler[F](usersRepo, messagesRepo)
      userHandler = new UserHandler[F](usersRepo)
      service <- ChatServiceFs2Grpc.bindServiceResource(
        new ChatServiceImpl[F](userHandler, messagesHandler)
      )
      serverBuilder = NettyServerBuilder
        .forPort(9999)
        .addService(service)
        .addService(ProtoReflectionService.newInstance())
    } yield serverBuilder
  }
}

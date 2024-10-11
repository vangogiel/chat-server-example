package io.vangogiel.chat

import cats.effect.kernel.Async
import cats.effect.{ ExitCode, IO, IOApp }
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.vangogiel.chat.chat_service.ChatServiceFs2Grpc

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    createServerAndAddServices[IO]()
      .evalMap(server => IO(server.build().start()))
      .useForever
      .map(_ => ())
      .as(ExitCode.Success)
  }

  private def createServerAndAddServices[F[_]: Async]() = {
    val serverBuilder = NettyServerBuilder.forPort(9999)
    val usersStorage: UsersStorage = new InMemoryUsersRepository
    val userHandler = new UserHandler[F](usersStorage)
    for {
      chatService <- ChatServiceFs2Grpc.bindServiceResource(new ChatServiceImpl[F](userHandler))
      _ = serverBuilder.addService(chatService)
      _ = serverBuilder.addService(ProtoReflectionService.newInstance())
    } yield serverBuilder
  }
}

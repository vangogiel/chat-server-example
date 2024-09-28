package io.vangogiel.chat

import cats.effect.{ ExitCode, IO, IOApp }
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.vangogiel.chat.chat_service.ChatServiceFs2Grpc

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    createServerAndAddServices()
      .evalMap(server => IO(server.build().start()))
      .useForever
      .map(_ => ())
      .as(ExitCode.Success)
  }

  private def createServerAndAddServices() = {
    val serverBuilder = NettyServerBuilder.forPort(9999)
    val usersStorage = new InMemoryUsersRepository
    for {
      chatService <- ChatServiceFs2Grpc.bindServiceResource(new ChatServiceImpl[IO](usersStorage))
      _ = serverBuilder.addService(chatService)
      _ = serverBuilder.addService(ProtoReflectionService.newInstance())
    } yield serverBuilder
  }
}

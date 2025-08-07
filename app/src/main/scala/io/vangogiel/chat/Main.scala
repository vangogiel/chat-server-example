package io.vangogiel.chat

import cats.effect.kernel.{Async, Resource}
import cats.effect.{ExitCode, IO, IOApp}
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.vangogiel.chat.application.MessageHandler
import io.vangogiel.chat.chat_service.ChatServiceFs2Grpc
import io.vangogiel.chat.infrastructure.db.{DbConfig, DbConnectionFactory, DbMigration, PostgresqlMessageRepository}
import io.vangogiel.chat.infrastructure.grpc.ChatServiceImpl

object Main extends IOApp with DbMigration {
  def run(args: List[String]): IO[ExitCode] = {
    createServerAndAddServices[IO]()
      .evalMap(server => IO(server.build().start()))
      .useForever
      .map(_ => ExitCode.Success)
  }

  private def createServerAndAddServices[F[_]: Async]() = {
    for {
      _ <- Resource.eval(migrate("jdbc:postgresql://localhost:5432/communicationDb?currentSchema=communication", "admin", "myPassword"))
      transactor <- DbConnectionFactory.createTransactor(DbConfig("admin", "myPassword", "jdbc:postgresql://localhost:5432/communicationDb?currentSchema=communication", 2))
      messagesRepo = new PostgresqlMessageRepository[F](transactor)
      messagesHandler = new MessageHandler[F](messagesRepo)
      service <- ChatServiceFs2Grpc.bindServiceResource(
        new ChatServiceImpl[F](messagesHandler)
      )
      serverBuilder = NettyServerBuilder
        .forPort(9999)
        .addService(service)
        .addService(ProtoReflectionService.newInstance())
    } yield serverBuilder
  }
}

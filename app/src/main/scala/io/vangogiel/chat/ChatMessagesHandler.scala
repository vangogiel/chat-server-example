package io.vangogiel.chat

import cats.effect.kernel.Async
import cats.implicits.toFlatMapOps

class ChatMessagesHandler[F[_]: Async](
    usersStorage: UsersStorage[F],
    messagesStorage: MessageStorage[F]
) {
  def getMessages(fromUsername: String, toUsername: String): F[Option[Vector[Message]]] = {
    usersStorage.findUser(fromUsername).flatMap { fromUser =>
      usersStorage.findUser(toUsername).flatMap { toUser =>
        (fromUser, toUser) match {
          case (Some(from), Some(to)) => messagesStorage.getUsersMessages(ChatId(Seq(from, to)))
          case _                      => Async[F].delay(None)
        }
      }
    }
  }
}

package io.vangogiel.chat

import cats.effect.kernel.Async
import cats.implicits.{ toFlatMapOps, toFunctorOps }

class ChatMessagesHandler[F[_]: Async](
    usersStorage: UsersStorage[F],
    messagesStorage: MessageStorage[F]
) {
  def getMessages(senderUuid: String, recipientUuid: String): F[Option[Vector[Message]]] = {
    usersStorage.findUser(senderUuid).flatMap { senderUser =>
      usersStorage.findUser(recipientUuid).flatMap { recipientUser =>
        (senderUser, recipientUser) match {
          case (Some(senderUser), Some(recipientUser)) =>
            messagesStorage.getUsersMessages(ChatId(Seq(senderUser, recipientUser)))
          case _ => Async[F].delay(None)
        }
      }
    }
  }

  def addMessageAndMaybeUpdateUserList(
      senderUuid: String,
      recipientUuid: String,
      message: Message
  ): F[Boolean] = {
    usersStorage.findUser(senderUuid).flatMap { senderUser =>
      usersStorage.findUser(recipientUuid).flatMap { recipientUser =>
        (senderUser, recipientUser) match {
          case (Some(senderUser), Some(recipientUser)) =>
            messagesStorage
              .addMessage(ChatId(Seq(senderUser, recipientUser)), message)
              .flatMap { _ =>
                usersStorage.addUserChat(senderUser, recipientUser).map(_ => true)
              }
          case _ => Async[F].delay(false)
        }
      }
    }
  }
}

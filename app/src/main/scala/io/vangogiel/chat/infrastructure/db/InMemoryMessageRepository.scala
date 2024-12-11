package io.vangogiel.chat.infrastructure.db

import cats.effect.kernel.{ Async, Ref }
import cats.implicits.toFunctorOps
import io.vangogiel.chat.domain.chat.ChatId
import io.vangogiel.chat.domain.message.{ Message, MessageStorage }

class InMemoryMessageRepository[F[_]: Async](messagesRef: Ref[F, Map[ChatId, Vector[Message]]])
    extends MessageStorage[F] {
  override def getUsersMessages(chatId: ChatId): F[Option[List[Message]]] = {
    messagesRef.modify { current =>
      current.get(chatId) match {
        case Some(messages) => (current, Some(messages.toList))
        case None => (current.updated(chatId, Vector()), None)
      }
    }
  }

  override def addMessage(chatId: ChatId, message: Message): F[Unit] = {
    messagesRef.update { map =>
      map.get(chatId) match {
        case Some(messages) => map.updated(chatId, messages :+ message)
        case None           => map.updated(chatId, Vector(message))
      }
    }
  }
}

object InMemoryMessageRepository {
  def apply[F[_]: Async](): F[InMemoryMessageRepository[F]] = {
    Ref.of[F, Map[ChatId, Vector[Message]]](Map.empty).map { ref =>
      new InMemoryMessageRepository[F](ref)
    }
  }
}

package io.vangogiel.chat.infrastructure.db

import cats.effect.kernel.{ Async, Ref }
import cats.implicits.toFunctorOps
import io.vangogiel.chat.domain.chat.ChatId
import io.vangogiel.chat.domain.message.{ Message, MessageStorage }

class InMemoryMessagesRepository[F[_]: Async](messagesRef: Ref[F, Map[ChatId, Vector[Message]]])
    extends MessageStorage[F] {
  override def getUsersMessages(chatId: ChatId): F[Option[Vector[Message]]] = {
    messagesRef.get.map { map =>
      map.get(chatId)
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

object InMemoryMessagesRepository {
  def apply[F[_]: Async](): F[InMemoryMessagesRepository[F]] = {
    Ref.of[F, Map[ChatId, Vector[Message]]](Map.empty).map { ref =>
      new InMemoryMessagesRepository[F](ref)
    }
  }
}

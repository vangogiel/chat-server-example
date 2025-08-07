package io.vangogiel.chat.application

import cats.effect.kernel.Async
import io.vangogiel.chat.domain.message.{ Message, MessageRepository }

import java.util.UUID

class MessageHandler[F[_]: Async](messagesRepository: MessageRepository[F]) {
  def getUndeliveredMessages(user1: UUID, user2: UUID): F[List[Message]] = {
    messagesRepository.getUndeliveredMessages(user1, user2)
  }
}

package io.vangogiel.chat.domain.message

import java.util.UUID

trait MessageRepository[F[_]] {
  def getUndeliveredMessages(user1: UUID, user2: UUID): F[List[Message]]
  def addMessage(message: Message): F[Unit]
  def markMessageAsDelivered(messageId: UUID): F[Boolean]
}

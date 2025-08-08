package io.vangogiel.chat.domain.message

import java.util.UUID

trait MessageRepository[F[_]] {
  def addMessage(message: Message): F[Boolean]
  def getUndeliveredMessages(user1: UUID, user2: UUID): F[List[Message]]
  def markMessageAsDelivered(messageId: UUID): F[Boolean]
}

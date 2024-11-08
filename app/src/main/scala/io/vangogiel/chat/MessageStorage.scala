package io.vangogiel.chat

trait MessageStorage[F[_]] {
  def getUsersMessages(chat: ChatId): F[Option[Vector[Message]]]
  def addMessage(chat: ChatId, message: Message): F[Unit]
}

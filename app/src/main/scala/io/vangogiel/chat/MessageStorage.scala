package io.vangogiel.chat

trait MessageStorage[F[_]] {
  def getUsersMessages(chat: ChatId): F[Option[Vector[Message]]]
}

package io.vangogiel.chat.domain.message

import io.vangogiel.chat.domain.chat.ChatId

trait MessageStorage[F[_]] {
  def getUsersMessages(chat: ChatId): F[Option[Vector[Message]]]
  def addMessage(chat: ChatId, message: Message): F[Unit]
}

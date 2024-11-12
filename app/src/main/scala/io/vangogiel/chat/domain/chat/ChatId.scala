package io.vangogiel.chat.domain.chat

import io.vangogiel.chat.domain.user.User

case class ChatId(users: Seq[User]) {
  override def hashCode(): Int = {
    users.map(_.id).sorted.mkString.hashCode
  }

  override def equals(obj: Any): Boolean = obj match {
    case that: ChatId => this.users.map(_.id).sorted == that.users.map(_.id).sorted
    case _            => false
  }
}

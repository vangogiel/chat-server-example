package io.vangogiel.chat

case class ChatId(users: Seq[User]) {
  override def hashCode(): Int = {
    users.map(_.id).sorted.mkString.hashCode
  }
}

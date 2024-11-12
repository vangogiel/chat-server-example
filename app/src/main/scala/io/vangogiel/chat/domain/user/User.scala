package io.vangogiel.chat.domain.user

case class User(id: String, username: String, chats: List[User] = List.empty)

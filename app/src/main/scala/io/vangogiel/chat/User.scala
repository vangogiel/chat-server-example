package io.vangogiel.chat

case class User(id: String, username: String, chats: List[User] = List.empty)

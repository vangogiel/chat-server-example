package io.vangogiel.chat

case class User(username: String, chats: List[Chat] = List.empty)

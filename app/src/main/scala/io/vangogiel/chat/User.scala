package io.vangogiel.chat

import java.util.UUID

case class User(id: UUID, username: String, chats: List[Chat] = List.empty)

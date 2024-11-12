package io.vangogiel.chat.domain.message

case class Message(senderUuid: String, recipientUuid: String, timestamp: Long, content: String)

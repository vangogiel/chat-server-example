package io.vangogiel.chat

case class Message(senderUuid: String, recipientUuid: String, timestamp: Long, content: String)

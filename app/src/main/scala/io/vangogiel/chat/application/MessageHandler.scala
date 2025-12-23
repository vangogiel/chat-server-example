package io.vangogiel.chat.application

import io.vangogiel.chat.domain.message.{Conversation, ConversationId, Message, MessageRepository}

class MessageHandler[F[_]](messagesRepository: MessageRepository[F]) {
  def addMessage(conversationId: ConversationId, message: Message): F[Boolean] = {
    messagesRepository.addMessage(conversationId, message)
  }

  def getUndeliveredMessages(conversationId: ConversationId): F[Conversation] = {
    messagesRepository.getUndeliveredMessages(conversationId)
  }

  def markMessageAsDelivered(messageId: String): F[Boolean] = {
    messagesRepository.markMessageAsDelivered(messageId)
  }
}

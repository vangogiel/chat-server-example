package io.vangogiel.chat.application

import io.vangogiel.chat.domain.conversation.{ Conversation, ConversationId }
import io.vangogiel.chat.domain.message.Message

trait MessageRepository[F[_]] {
  def addMessage(conversationId: ConversationId, message: Message): F[Boolean]
  def getUndeliveredMessages(conversationId: ConversationId): F[Conversation]
  def markMessageAsDelivered(messageId: String): F[Boolean]
}

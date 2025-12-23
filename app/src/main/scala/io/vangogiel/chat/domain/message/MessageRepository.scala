package io.vangogiel.chat.domain.message

trait MessageRepository[F[_]] {
  def addMessage(conversationId: ConversationId, message: Message): F[Boolean]
  def getUndeliveredMessages(conversationId: ConversationId): F[Conversation]
  def markMessageAsDelivered(messageId: String): F[Boolean]
}

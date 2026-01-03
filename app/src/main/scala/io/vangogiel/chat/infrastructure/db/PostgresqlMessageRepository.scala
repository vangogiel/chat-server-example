package io.vangogiel.chat.infrastructure.db

import cats.effect.Async
import cats.implicits._
import doobie.{Fragment, Transactor}
import doobie.implicits._
import doobie.postgres.implicits._
import io.vangogiel.chat.application.MessageRepository
import io.vangogiel.chat.domain.conversation.{Conversation, ConversationId}
import io.vangogiel.chat.domain.message.Message
import io.vangogiel.chat.infrastructure.db.PostgresqlMessageRepository.messageTable

class PostgresqlMessageRepository[F[_]: Async](transactor: Transactor[F])
    extends MessageRepository[F] {

  override def addMessage(conversationId: ConversationId, message: Message): F[Boolean] = {
    sql"""insert into $messageTable (conversation_id, message_id, sender_id, recipient_id, sent_at, content)
          values (
            ${conversationId.value},
            ${message.id},
            ${message.senderId},
            ${message.recipientId},
            ${message.sentAt},
            ${message.content}
          )"""
      .update.run
      .transact(transactor)
      .map {
        case 0 => false
        case _ => true
      }
  }

  override def getUndeliveredMessages(conversationId: ConversationId): F[Conversation] = {
    sql"""select message_id, sender_id, recipient_id, sent_at, content
          from $messageTable
          where conversation_id = ${conversationId.value}
            and delivered = false
          order by sent_at desc"""
      .query[Message]
      .to[List]
      .transact(transactor)
      .map(messages => Conversation(conversationId, messages))
  }

  override def markMessageAsDelivered(messageId: String): F[Boolean] =
    sql"""update $messageTable
           set delivered = true
          where message_id = $messageId"""
      .update.run
      .transact(transactor)
      .map {
        case 0 => false
        case _ => true
      }
}


object PostgresqlMessageRepository {
  val messageTable: Fragment = fr"message"
}

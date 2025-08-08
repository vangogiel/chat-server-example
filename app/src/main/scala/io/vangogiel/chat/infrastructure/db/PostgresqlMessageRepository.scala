package io.vangogiel.chat.infrastructure.db

import cats.effect.Async
import cats.implicits._
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import io.vangogiel.chat.domain.message.{ Message, MessageRepository }

import java.util.UUID

class PostgresqlMessageRepository[F[_]: Async](transactor: Transactor[F])
    extends MessageRepository[F] {

  override def addMessage(message: Message): F[Boolean] = {
    sql"""insert into direct_message (id, sender_id, recipient_id, sent_at, content)
          values (
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

  override def getUndeliveredMessages(user1: UUID, user2: UUID): F[List[Message]] = {
    sql"""select id, sender_id, recipient_id, sent_at, content
          from direct_message
          where ((sender_id = $user1 and recipient_id = $user2)
             or (sender_id = $user2 and recipient_id = $user1))
            and delivered = false
          order by sent_at desc"""
      .query[Message]
      .to[List]
      .transact(transactor)
  }

  override def markMessageAsDelivered(messageId: UUID): F[Boolean] =
    sql"""update direct_message
           set delivered = true
          where id = $messageId"""
      .update.run
      .transact(transactor)
      .map {
        case 0 => false
        case _ => true
      }
}

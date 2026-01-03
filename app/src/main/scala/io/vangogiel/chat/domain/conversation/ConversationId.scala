package io.vangogiel.chat.domain.conversation

import java.util.UUID
import scala.util.hashing.MurmurHash3

case class ConversationId(value: Long)

object ConversationId {
  def of(user1: UUID, user2: UUID): ConversationId = {
    ConversationId(
      MurmurHash3
        .stringHash(Seq(user1.toString, user2.toString).sorted.mkString(":"))
        .toLong
    )
  }
}

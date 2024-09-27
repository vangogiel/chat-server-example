package io.vangogiel.chat

import cats.effect.Sync
import cats.effect.kernel.Async
import fs2.Stream
import io.grpc.Metadata
import io.vangogiel.chat.chat_service.ChatServiceFs2Grpc
import io.vangogiel.chat.chats_list_request.ChatsListRequest
import io.vangogiel.chat.chats_list_stream_response.{Chat => ChatProto, ChatsListStreamResponse => ChatsListStreamResponseProto}
import io.vangogiel.chat.handling_result.HandlingResult
import io.vangogiel.chat.handling_result.HandlingResult.Result.Success
import io.vangogiel.chat.incoming_conversation_request.IncomingConversationRequest
import io.vangogiel.chat.incoming_conversation_stream_response.{ IncomingConversationStreamResponse => IncomingConversationStreamResponseProto }
import io.vangogiel.chat.outgoing_conversation_stream_request.OutgoingConversationStreamRequest
import io.vangogiel.chat.users_list_request.UsersListRequest
import io.vangogiel.chat.users_list_stream_response.{UsersListStreamResponse => UsersListStreamResponseProto}
import io.vangogiel.chat.users_list_stream_response.{User => UserProto}

import java.time.Instant
import scala.concurrent.duration.DurationInt
import scala.util.Random

class ChatServiceImpl[F[_] : Async] extends ChatServiceFs2Grpc[F, Metadata] {

  override def streamUsersList(request: UsersListRequest, ctx: Metadata): fs2.Stream[F, UsersListStreamResponseProto] = {
    Stream
      .awakeEvery[F](10.seconds)
      .evalMap(_ => Sync[F].delay {
        UsersListStreamResponseProto(
          Seq(
            UserProto(
              username = generateRandomUserName,
              lastActiveTimestamp = Instant.now().getEpochSecond
            )
          )
        )
      })
  }

  override def streamChatsList(request: ChatsListRequest, ctx: Metadata): fs2.Stream[F, ChatsListStreamResponseProto] = {
    Stream
      .awakeEvery[F](10.seconds)
      .evalMap(_ => Sync[F].delay {
        ChatsListStreamResponseProto(
          Seq(
            ChatProto(
              fromUsername = generateRandomUserName,
              numberOfUnreadMessages = 1,
            )
          )
        )
      })
  }

  override def getOutgoingConversationStream(request: fs2.Stream[F, OutgoingConversationStreamRequest], ctx: Metadata): fs2.Stream[F, HandlingResult] =
    Stream
      .awakeEvery[F](10.seconds)
      .evalMap(_ => Sync[F].delay {
        HandlingResult(result = Success(value = HandlingResult.Success()))
      })

  override def getIncomingConversationStream(request: IncomingConversationRequest, ctx: Metadata): fs2.Stream[F, IncomingConversationStreamResponseProto] =
    Stream
      .awakeEvery[F](10.seconds)
      .evalMap(_ => Sync[F].delay {
        IncomingConversationStreamResponseProto()
      })

  private def generateRandomUserName = {
    val listOfNames = Seq("John", "Michael", "Jack")
    listOfNames(Random.nextInt(2)) + Random.nextInt(999)
  }
}

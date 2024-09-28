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
import io.vangogiel.chat.handling_result.HandlingResult.Result.Failure
import io.vangogiel.chat.incoming_conversation_request.IncomingConversationRequest
import io.vangogiel.chat.incoming_conversation_stream_response.{IncomingConversationStreamResponse => IncomingConversationStreamResponseProto}
import io.vangogiel.chat.new_user.NewUser
import io.vangogiel.chat.outgoing_conversation_stream_request.OutgoingConversationStreamRequest
import io.vangogiel.chat.users_list_request.UsersListRequest
import io.vangogiel.chat.users_list_stream_response.{UsersListStreamResponse => UsersListStreamResponseProto}
import io.vangogiel.chat.users_list_stream_response.{User => UserProto}

import java.time.Instant
import scala.concurrent.duration.DurationInt
import scala.util.Random

class ChatServiceImpl[F[_] : Async](usersStorage: UsersStorage) extends ChatServiceFs2Grpc[F, Metadata] {

  override def addNewUser(request: NewUser, ctx: Metadata): F[HandlingResult] = {
    Sync[F].delay {
      if (usersStorage.usernameExists(request.username)) {
        HandlingResult(result = Failure(HandlingResult.Failure("Username already exists", "6")))
      } else {
        usersStorage.addUser(User(None, request.username))
        HandlingResult(result = Success(value = HandlingResult.Success()))
      }
    }
  }

  override def getUsersList(request: UsersListRequest, ctx: Metadata): F[UsersListStreamResponseProto] = {
    Sync[F].delay {
      UsersListStreamResponseProto(
        usersStorage
          .getListOfUsers()
          .map(user => UserProto(user.username, Instant.now().getEpochSecond))
      )
    }
  }

  override def getChatsList(request: ChatsListRequest, ctx: Metadata): F[ChatsListStreamResponseProto] = {
    Sync[F].delay {
      ChatsListStreamResponseProto(
        Seq(
          ChatProto(
            fromUsername = generateRandomUserName,
            numberOfUnreadMessages = 1
          )
        )
      )
    }
  }

  override def getOutgoingConversationStream(
                                              request: fs2.Stream[F, OutgoingConversationStreamRequest],
                                              ctx: Metadata
                                            ): fs2.Stream[F, HandlingResult] =
    Stream
      .awakeEvery[F](10.seconds)
      .evalMap(_ =>
        Sync[F].delay {
          HandlingResult(result = Success(value = HandlingResult.Success()))
        }
      )

  override def getIncomingConversationStream(
                                              request: IncomingConversationRequest,
                                              ctx: Metadata
                                            ): fs2.Stream[F, IncomingConversationStreamResponseProto] =
    Stream
      .awakeEvery[F](10.seconds)
      .evalMap(_ =>
        Sync[F].delay {
          IncomingConversationStreamResponseProto()
        }
      )

  private def generateRandomUserName = {
    val listOfNames = Seq("John", "Michael", "Jack")
    listOfNames(Random.nextInt(2)) + Random.nextInt(999)
  }
}

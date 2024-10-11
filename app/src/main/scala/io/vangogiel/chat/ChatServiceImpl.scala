package io.vangogiel.chat

import cats.effect.Sync
import cats.effect.kernel.Async
import cats.implicits._
import fs2.Stream
import io.grpc.Metadata
import io.vangogiel.chat.chat_service.ChatServiceFs2Grpc
import io.vangogiel.chat.chats_list_request.ChatsListRequest
import io.vangogiel.chat.chats_list_response.{
  Chat => ChatProto,
  ChatsListResponse => ChatsListResponseProto
}
import io.vangogiel.chat.handling_result.HandlingResult
import io.vangogiel.chat.handling_result.HandlingResult.Result.Success
import io.vangogiel.chat.handling_result.HandlingResult.Result.Failure
import io.vangogiel.chat.incoming_conversation_request.IncomingConversationRequest
import io.vangogiel.chat.incoming_conversation_stream_response.{
  IncomingConversationStreamResponse => IncomingConversationStreamResponseProto
}
import io.vangogiel.chat.new_user.NewUser
import io.vangogiel.chat.outgoing_conversation_stream_request.OutgoingConversationStreamRequest
import io.vangogiel.chat.users_list_request.UsersListRequest
import io.vangogiel.chat.users_list_response.{ UsersListResponse => UsersListResponseProto }
import io.vangogiel.chat.users_list_response.{ User => UserProto }

import scala.concurrent.duration.DurationInt

class ChatServiceImpl[F[_]: Async](userHandler: UserHandler[F])
    extends ChatServiceFs2Grpc[F, Metadata] {

  override def addNewUser(request: NewUser, ctx: Metadata): F[HandlingResult] = {
    userHandler
      .addNewUser(User(request.username))
      .map {
        case true =>
          HandlingResult(result = Success(value = HandlingResult.Success()))
        case false =>
          HandlingResult(result = Failure(HandlingResult.Failure("Username already exists", "6")))
      }
  }

  override def getUsersList(
      request: UsersListRequest,
      ctx: Metadata
  ): F[UsersListResponseProto] = {
    userHandler.getUsersList
      .map(users => users.map(user => UserProto(user.username)))
      .map(userProtoList => UsersListResponseProto(userProtoList))
  }

  override def getChatsList(
      request: ChatsListRequest,
      ctx: Metadata
  ): F[ChatsListResponseProto] = {
    userHandler
      .getUserChats(User(request.username))
      .map { listOfChats =>
        ChatsListResponseProto(
          listOfChats.map(chat => ChatProto(username = chat.user.username))
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
}

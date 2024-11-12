package io.vangogiel.chat.infrastructure.grpc

import cats.effect.kernel.Async
import cats.implicits._
import fs2.Stream
import io.grpc.Status.{ FAILED_PRECONDITION, INVALID_ARGUMENT }
import io.grpc.{ Metadata, Status }
import io.vangogiel.chat.chat_service.ChatServiceFs2Grpc
import io.vangogiel.chat.chats_list_request.ChatsListRequest
import io.vangogiel.chat.chats_list_response.{
  Chat => ChatProto,
  ChatsListResponse => ChatsListResponseProto
}
import io.vangogiel.chat.domain.message.Message
import io.vangogiel.chat.domain.user.User
import io.vangogiel.chat.handling_result.HandlingResult
import io.vangogiel.chat.handling_result.HandlingResult.Result.{ Failure, Success }
import io.vangogiel.chat.message.{ Message => MessageProto }
import io.vangogiel.chat.new_user.NewUser
import io.vangogiel.chat.receive_message_request.ReceiveMessageRequest
import io.vangogiel.chat.receive_message_stream_response.ReceiveMessageStreamResponse
import io.vangogiel.chat.send_message_stream_request.SendMessageStreamRequest
import io.vangogiel.chat.users_list_request.UsersListRequest
import io.vangogiel.chat.users_list_response.{
  User => UserProto,
  UsersListResponse => UsersListResponseProto
}
import io.vangogiel.chat.application.{ MessageHandler, UserHandler }

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.hashing.MurmurHash3

class ChatServiceImpl[F[_]: Async](
    userHandler: UserHandler[F],
    messagesHandler: MessageHandler[F]
) extends ChatServiceFs2Grpc[F, Metadata] {

  override def createUser(request: NewUser, ctx: Metadata): F[HandlingResult] = {
    userHandler
      .addNewUser(User(UUID.randomUUID().toString, request.username))
      .map {
        case true =>
          HandlingResult(result = Success(value = HandlingResult.Success()))
        case false =>
          HandlingResult(result = Failure(HandlingResult.Failure("Username already exists", "6")))
      }
  }

  override def listUsers(
      request: UsersListRequest,
      ctx: Metadata
  ): F[UsersListResponseProto] = {
    userHandler.getUsersList
      .map(users => users.map(user => UserProto(username = user.username, uuid = user.id)))
      .map(userProtoList => UsersListResponseProto(userProtoList))
  }

  override def listChats(
      request: ChatsListRequest,
      ctx: Metadata
  ): F[ChatsListResponseProto] = {
    userHandler
      .listUserChats(request.userUuid)
      .map { listOfChats =>
        ChatsListResponseProto(
          listOfChats.map(chat => ChatProto(userUuid = chat.id))
        )
      }
  }

  override def sendMessageStream(
      request: fs2.Stream[F, SendMessageStreamRequest],
      ctx: Metadata
  ): Stream[F, HandlingResult] =
    request
      .evalMap { req =>
        messagesHandler
          .addMessageAndMaybeUpdateUserList(
            req.senderUuid,
            req.recipientUuid,
            mapMessageFromProto(req.senderUuid, req.recipientUuid, req.content)
          )
          .map {
            case true =>
              HandlingResult(Success(HandlingResult.Success()))
            case false =>
              getFailureHandlingResult("user not found", FAILED_PRECONDITION)
          }
      }

  override def receiveMessageStream(
      request: ReceiveMessageRequest,
      ctx: Metadata
  ): fs2.Stream[F, ReceiveMessageStreamResponse] = {
    var currentHash = Option.empty[Long]
    Stream
      .awakeEvery(500.millis)
      .evalMap { _ =>
        messagesHandler
          .getMessages(request.senderUuid, request.recipientUuid)
          .map { maybeMessages =>
            maybeMessages.flatMap { messages =>
              messages.lastOption match {
                case Some(lastMessage) if !currentHash.contains(hashFunction(lastMessage)) =>
                  currentHash = Some(hashFunction(lastMessage))
                  Some(lastMessage)
                case _ => None
              }
            }
          }
      }
      .flatMap {
        case Some(message) => Stream(mapToReceiveMessageStreamResponseProto(message))
        case None          => Stream.empty
      }
  }

  private def hashFunction(message: Message): Long = {
    MurmurHash3
      .seqHash(message.senderUuid + message.recipientUuid + message.timestamp + message.content)
      .toLong
  }

  private def mapToReceiveMessageStreamResponseProto(message: Message) = {
    ReceiveMessageStreamResponse(
      senderUuid = message.senderUuid,
      recipientUuid = message.recipientUuid,
      message = Some(MessageProto(message.timestamp, message.content))
    )
  }

  private def mapMessageFromProto(
      senderUuid: String,
      recipientUuid: String,
      content: String
  ): Message = {
    Message(senderUuid, recipientUuid, System.currentTimeMillis(), content)
  }

  private def getFailureHandlingResult(message: String, code: Status): HandlingResult = {
    HandlingResult(Failure(HandlingResult.Failure(message, code.getCode.value().toString)))
  }
}

package io.vangogiel.chat

import cats.effect.kernel.{ Ref, Sync }
import cats.implicits.toFunctorOps

class InMemoryUsersRepository[F[_]: Sync](usersStorageRef: Ref[F, List[User]])
    extends UsersStorage[F] {
  override def getListOfUsers: F[List[User]] = {
    usersStorageRef.get
  }

  override def addUser(user: User): F[Unit] = {
    usersStorageRef.update(listOfUsers => user :: listOfUsers)
  }

  override def usernameExists(username: String): F[Boolean] =
    usersStorageRef.get.map(listOfUsers => listOfUsers.map(_.username).contains(username))

  override def findUser(username: String): F[Option[User]] =
    usersStorageRef.get.map(listOfUsers => listOfUsers.find(_.username == username))

  override def addUserChat(userA: User, userB: User): F[Unit] = {
    usersStorageRef.update(listOfUsers => {
      listOfUsers.map {
        case user if user.username == userA.username && !user.chats.contains(userB) =>
          user.copy(chats = user.chats :+ userB)
        case user if user.username == userB.username && !user.chats.contains(userA) =>
          user.copy(chats = user.chats :+ userA)
        case user => user
      }
    })
  }
}

object InMemoryUsersRepository {
  def apply[F[_]: Sync](): F[InMemoryUsersRepository[F]] = {
    Ref.of[F, List[User]](List.empty).map { ref =>
      new InMemoryUsersRepository[F](ref)
    }
  }
}

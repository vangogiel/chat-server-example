package io.vangogiel.chat.infrastructure.db

import cats.effect.kernel.{ Ref, Sync }
import cats.implicits.toFunctorOps
import cats.implicits._
import io.vangogiel.chat.domain.user.{ User, UserStorage }

class InMemoryUserRepository[F[_]: Sync](usersStorageRef: Ref[F, List[User]])
    extends UserStorage[F] {
  override def getListOfUsers: F[List[User]] = {
    usersStorageRef.get
  }

  override def addUser(user: User): F[Unit] = {
    usersStorageRef.update(listOfUsers => user :: listOfUsers)
  }

  override def usernameExists(username: String): F[Boolean] =
    usersStorageRef.get.map(listOfUsers => listOfUsers.map(_.username).contains(username))

  override def findUser(userUuid: String): F[Option[User]] =
    usersStorageRef.get.map(listOfUsers => listOfUsers.find(_.id == userUuid))

  override def addUserChat(users: User*): F[Unit] = {
    users.toList.traverse_{ user =>
      usersStorageRef.update(listOfUsers => {
        listOfUsers.map {
          case foundUser if foundUser.id == user.id && !foundUser.chats.map(_.id).contains(user.id) =>
            foundUser.copy(chats = foundUser.chats :+ user)
          case foundUser => foundUser
        }
      })
    }
  }
}

object InMemoryUserRepository {
  def apply[F[_]: Sync](): F[InMemoryUserRepository[F]] = {
    Ref.of[F, List[User]](List.empty).map { ref =>
      new InMemoryUserRepository[F](ref)
    }
  }
}

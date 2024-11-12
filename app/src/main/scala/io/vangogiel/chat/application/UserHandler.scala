package io.vangogiel.chat.application

import cats.effect.Sync
import cats.effect.kernel.Async
import cats.implicits.{ toFlatMapOps, toFunctorOps }
import io.vangogiel.chat.domain.user.{ User, UserStorage }

class UserHandler[F[_]: Async](usersStorage: UserStorage[F]) {
  def addNewUser(user: User): F[Boolean] = {
    usersStorage.usernameExists(user.username).flatMap {
      case true  => Sync[F].delay(false)
      case false => usersStorage.addUser(user).map(_ => true)
    }
  }

  def getUsersList: F[List[User]] = {
    usersStorage.getListOfUsers
  }

  def listUserChats(userUuid: String): F[List[User]] = {
    usersStorage.findUser(userUuid).map {
      case Some(user) => user.chats
      case None       => List.empty
    }
  }
}

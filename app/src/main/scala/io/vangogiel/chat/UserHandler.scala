package io.vangogiel.chat

import cats.effect.Sync
import cats.effect.kernel.Async
import cats.implicits.{ toFlatMapOps, toFunctorOps }

class UserHandler[F[_]: Async](usersStorage: UsersStorage[F]) {
  def addNewUser(user: User): F[Boolean] = {
    usersStorage.usernameExists(user.username).flatMap {
      case true  => Sync[F].delay(false)
      case false => usersStorage.addUser(user).map(_ => true)
    }
  }

  def getUsersList: F[List[User]] = {
    usersStorage.getListOfUsers
  }

  def getUserChats(user: String): F[List[Chat]] = {
    usersStorage.findUser(user).map {
      case Some(user) => user.chats
      case None       => List.empty
    }
  }
}

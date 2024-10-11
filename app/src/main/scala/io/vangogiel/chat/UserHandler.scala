package io.vangogiel.chat

import cats.effect.Sync
import cats.effect.kernel.Async

class UserHandler[F[_]: Async](usersStorage: UsersStorage) {
  def addNewUser(user: User): F[Boolean] = {
    Sync[F].delay {
      if (usersStorage.usernameExists(user.username)) {
        false
      } else {
        usersStorage.addUser(User(user.username))
        true
      }
    }
  }

  def getUsersList: F[List[User]] = {
    Sync[F].delay {
      usersStorage.getListOfUsers
    }
  }

  def getUserChats(mainUser: User): F[List[Chat]] = {
    Sync[F].delay {
      usersStorage.findUser(mainUser.username) match {
        case Some(user) => user.chats
        case None       => List.empty
      }
    }
  }
}

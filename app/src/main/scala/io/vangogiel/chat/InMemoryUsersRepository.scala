package io.vangogiel.chat

import cats.effect.kernel.Sync

class InMemoryUsersRepository[F[_]: Sync] extends UsersStorage[F] {
  private var listOfUsers: List[User] = List.empty

  override def getListOfUsers: F[List[User]] = {
    Sync[F].delay(listOfUsers)
  }

  override def addUser(user: User): F[Unit] = {
    Sync[F].delay((listOfUsers = user :: listOfUsers))
  }

  override def usernameExists(username: String): F[Boolean] =
    Sync[F].delay(listOfUsers.map(_.username).contains(username))

  override def findUser(username: String): F[Option[User]] =
    Sync[F].delay((listOfUsers.find(_.username == username)))
}

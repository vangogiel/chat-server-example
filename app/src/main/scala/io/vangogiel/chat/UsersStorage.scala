package io.vangogiel.chat

trait UsersStorage[F[_]] {
  def addUser(user: User): F[Unit]

  def findUser(username: String): F[Option[User]]

  def getListOfUsers: F[List[User]]

  def usernameExists(username: String): F[Boolean]
}

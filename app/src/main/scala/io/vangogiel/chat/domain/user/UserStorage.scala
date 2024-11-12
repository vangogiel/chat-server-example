package io.vangogiel.chat.domain.user

trait UserStorage[F[_]] {
  def addUser(user: User): F[Unit]

  def findUser(userUuid: String): F[Option[User]]

  def getListOfUsers: F[List[User]]

  def addUserChat(userA: User, userB: User): F[Unit]

  def usernameExists(username: String): F[Boolean]
}

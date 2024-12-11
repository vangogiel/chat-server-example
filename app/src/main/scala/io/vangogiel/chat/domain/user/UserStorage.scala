package io.vangogiel.chat.domain.user

trait UserStorage[F[_]] {
  def addUser(user: User): F[Unit]

  def findUser(userUuid: String): F[Option[User]]

  def findTwoUsers(userOneUuid: String, userTwoUuid: String): F[(Option[User], Option[User])]

  def getListOfUsers: F[List[User]]

  def addUserChat(users: User*): F[Unit]

  def usernameExists(username: String): F[Boolean]
}

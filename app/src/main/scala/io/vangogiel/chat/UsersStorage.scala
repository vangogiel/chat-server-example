package io.vangogiel.chat

trait UsersStorage {
  def addUser(user: User): Unit

  def findUser(username: String): Option[User]

  def getListOfUsers: List[User]

  def usernameExists(username: String): Boolean
}

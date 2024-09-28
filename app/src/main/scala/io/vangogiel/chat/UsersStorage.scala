package io.vangogiel.chat

trait UsersStorage {
  def addUser(user: User): Unit

  def getListOfUsers(): List[User]

  def usernameExists(username: String): Boolean
}

package io.vangogiel.chat

class InMemoryUsersRepository extends UsersStorage {
  private var listOfUsers: List[User] = List.empty

  override def getListOfUsers(): List[User] = {
    listOfUsers
  }

  override def addUser(user: User): Unit = {
    listOfUsers = user :: listOfUsers
  }

  override def usernameExists(username: String): Boolean = listOfUsers.map(_.username).contains(username)
}

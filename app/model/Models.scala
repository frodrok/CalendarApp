package model

case class User(id: Long, username: String, password: String,
                admin: Option[Boolean] = None,
                groupId: Option[Int] = None)

case class Group(id: Option[Int], groupName: String)

case class Event(id: Option[Int], eventName: String, from: Long, to: Long, groupId: Option[Int])

case class UserNotFoundException(s: String) extends Exception

case class UserHasNoGroupException(s: String) extends Exception

case class JsonUser(username: String, password: Option[String])
case class JsonUsers(users: List[JsonUser])


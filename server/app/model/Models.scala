package model

case class User(id: Long, username: String, password: String,
                admin: Option[Boolean] = None,
                groupId: Option[Int] = None)

case class Group(id: Option[Int], groupName: String, active: Boolean) {
  def toJsonGroup(): JsonGroup = {
    JsonGroup(id, groupName, Some(active))
  }
}

case class Event(id: Option[Int], eventName: String, from: Long, to: Long, groupId: Option[Int])

case class UserNotFoundException(s: String) extends Exception

case class UserHasNoGroupException(s: String) extends Exception

case class JsonUser(id: Option[Int], username: String, password: Option[String], admin: Option[Boolean], groupId: Option[Int])
case class JsonEvent(id: Option[Int], eventName: String, from: String, to: Option[String], groupId: Int)
case class JsonGroup(id: Option[Int], groupName: String, active: Option[Boolean])


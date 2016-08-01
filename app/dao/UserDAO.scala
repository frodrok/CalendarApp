package dao

import javax.inject.Inject


import model.{Event, Group, User}
import model.{UserHasNoGroupException, UserNotFoundException}
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.driver.JdbcProfile
import slick.driver.H2Driver.api._

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global



class UserDAO @Inject()(@NamedDatabase("msql") val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val Users = TableQuery[UsersTable]
  private val Groups = TableQuery[GroupsTable]
  private val Events = TableQuery[EventsTable]

  def setup: Unit = {
    db.run(Events.schema.create).onFailure{ case ex => println(ex) }
  }

  def addEvent(event: Event): Future[Option[Int]] = {
    db.run(
      (Events returning Events.map(_.id)) += event
    )
  }

  def getEventsForGroup(groupId: Int): Future[Seq[Event]] = {
    /* val q = for {
      e <- Events if e.groupId === groupId
    } yield (e) unused, could be used */

    val extraquery = Events.filter(_.groupId === groupId).result

    db.run(extraquery)
  }

  def getEventsForUser(userId: Int): Future[Seq[Event]] = {
    val user = Await.result(getUserById(userId), 3.seconds)
    user match {
      case Some(user) => {
        /* getEventsForGroup(user.groupId.get) */
        user.groupId match {
          case Some(groupId) => getEventsForGroup(groupId)
          case None => throw new UserHasNoGroupException("User with id: " + userId + " has no group")
        }
      }
      case None => throw new UserNotFoundException("User with id: " + userId + " not found")
    }
  }

  def allGroups: Future[Seq[Group]] = {
    db.run(Groups.result)
  }

  def getUserById(id: Long): Future[Option[User]] = {
    val idQuery = for {
      u <- Users if u.id === id
    } yield (u)

    db.run(idQuery.result).map {
      case user => user.headOption
    }
  }

  def updateUser(newUser: User): Future[Option[Long]] = db.run {
    Users.filter(_.id === newUser.id).update(newUser).map {
      case 0 => None
      case _ => Some(newUser.id)
    }

  }

  def add(user: User): Future[Long] = {
    db.run(
      (Users returning Users.map(_.id)) += user
    )
  }

  def addGroup(group: Group): Future[Option[Int]] = {
    db.run(
      (Groups returning Groups.map(_.id)) += group
    )
  }

  def allUsers: Future[Seq[User]] = db.run(Users.result)

  def getUserByGroup(retrievedGroupName: String): Future[Seq[User]] = {
    val userByGroupName = for {
      (user, group) <- Users join Groups on (_.groupId === _.id) if group.groupName === retrievedGroupName
    } yield user

    db.run(userByGroupName.result)
  }

  def getUserByUsername(retrievedUsername: String): Future[User] = {
    val q = for {
      q <- Users if q.username === retrievedUsername
    } yield (q)

    db.run(q.result).map {
      seq => seq.head
    }
  }

  def getUserByGroup(retrievedGroupId: Int): Future[Seq[User]] = {
    val userByGroupName = for {
      (user, group) <- Users join Groups on (_.groupId === _.id) if group.id === retrievedGroupId
    } yield user

    db.run(userByGroupName.result)
  }



  private class UsersTable(tag: Tag) extends Table[User](tag, "user") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username", O.SqlType("VARCHAR(50)"))
    def password = column[String]("password")
    def admin = column[Option[Boolean]]("isadmin")

    def groupId = column[Option[Int]]("group_id")
    def group = foreignKey("group_id", groupId, Groups)(_.id)

    override def * = (id, username, password, admin, groupId) <> (User.tupled, User.unapply)

    def idxUsername = index("idx_username", username, unique = true)
  }

  private class GroupsTable(tag: Tag) extends Table[Group](tag, "group") {
    def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
    def groupName = column[String]("group_name")

    override def * = (id, groupName) <> (Group.tupled, Group.unapply)
  }

  private class EventsTable(tag: Tag) extends Table[Event](tag, "event") {
    def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
    def eventName = column[String]("event_name")

    def from = column[Long]("from")
    def to = column[Long]("to")

    def groupId = column[Option[Int]]("group_id")
    def group = foreignKey("group_id", groupId, Groups)(_.id)

    override def * = (id, eventName, from, to, groupId) <> (Event.tupled, Event.unapply)
  }


}
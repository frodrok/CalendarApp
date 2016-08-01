package controllers


import javax.inject.Inject

import dao.UserDAO
import model.{Event, User, UserHasNoGroupException, UserNotFoundException}
import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import play.api.libs.json._

/* import play.api.Play.current */
import play.api.i18n.Messages.Implicits._
import scala.concurrent.duration._

case class EventWithTimeStamp(id: Option[Int], eventName: String, from: DateTime, to: DateTime, groupId: Option[Int])

class UserController @Inject()(val messagesApi: MessagesApi, userDao: UserDAO) extends Controller with I18nSupport{

  implicit val eventWrites = new Writes[EventWithTimeStamp] {
    def writes(event: EventWithTimeStamp) = Json.obj(
      "eventName" -> event.eventName,
      "from" -> event.from.toString,
      "to" -> event.to.toString,
      "groupId" -> event.groupId
    )
  }

  val addEventform: Form[addEventFormData] = Form(
    mapping(
      "eventName" -> text,
      "from" -> jodaDate("yyyy-mm-dd"),
      "to" -> jodaDate("yyyy-mm-dd"),
      "fromClock" -> number,
      "toClock" -> number,
      "groupId" -> number
    )(addEventFormData.apply)(addEventFormData.unapply)
  )

  val allGroups = Await.result(userDao.allGroups, 3.seconds)

  def userPage = Action { request =>
    val username = request.session.get("connected").headOption

    if (username.isDefined) {
      val user = Await.result(userDao.getUserByUsername(username.get), 3.seconds)
      Ok(views.html.user.base(user, allGroups)(addEventform))
    } else {
      BadRequest(views.html.error("go log in bitch"))
    }

  }

  def addEvent = Action { implicit request =>

    addEventform.bindFromRequest.fold(
      formErrors => {
        val username = request.session.get("connected").headOption

        username match {
          case None => Ok("wat")
          case Some(username) => {
            val user = Await.result(userDao.getUserByUsername(username), 3.seconds)
            Ok(views.html.user.base(user, allGroups)(formErrors))
          }
        }
      },
      data => {

        val event = Event(Some(0), data.eventName, data.from.getMillis, data.to.getMillis, Some(data.groupId))
        val eventid = Await.result(userDao.addEvent(event), 3.seconds)

        Redirect("/user")
      }
    )

  }

  def setGroup = Action.async { implicit request =>

    val groupId = request.body.asFormUrlEncoded.get.head._2.head

    val username = request.session.get("connected").headOption

    username match {
      case Some(username) => {
        val user = Await.result(userDao.getUserByUsername(username), 3.seconds)
        val newUser = User(user.id, user.username, user.password, user.admin, Some(groupId.toInt))
        userDao.updateUser(newUser).map {
          /* long => Ok("userid " + long.head + " updated, set group to " + groupId) */
          long => Redirect("/user")
        }
      }
    }

  }

  def logout = Action { request =>
    Redirect("/").withNewSession
  }

  def eventsForUserJson(userId: Int) = Action {
    try {
      val eventsSeq = Await.result(userDao.getEventsForUser(userId), 3.seconds)

      val withTimeStamps = eventsSeq.seq.map {
        event => new EventWithTimeStamp(event.id, event.eventName, new DateTime(event.from), new DateTime(event.to), event.groupId)
      }

      val asJson = Json.toJson(withTimeStamps)
      Ok(asJson).withHeaders("Content-Type" -> "application/json; charset=utf-8", "Access-Control-Allow-Credentials" -> "true", "Access-Control-Allow-Origin" -> "http://yourdomain.com")

    } catch {
      case userNotFound: UserNotFoundException => NotFound("No user with id: " + userId)
      case userNoGroup: UserHasNoGroupException => BadRequest("User with id: " + userId + " has no group")
    }

    // Ok("dropped out of catch")

    }
}

case class addEventFormData(eventName: String, from: DateTime, to: DateTime, fromClock: Int, toClock: Int, groupId: Int)


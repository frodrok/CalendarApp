package controllers


import javax.inject.Inject

import dao.UserDAO
import model._
import org.joda.time.DateTime
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, BodyParsers, Controller, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}

/* import play.api.Play.current */
import play.api.i18n.Messages.Implicits._
import play.api.libs.functional.syntax._
import scala.concurrent.duration._

case class EventWithTimeStamp(id: Option[Int], eventName: String, from: DateTime, to: Option[DateTime], groupId: Option[Int])
case class TempEvent(eventName: String, from: String, groupId: Int)

class UserController @Inject()(val messagesApi: MessagesApi, userDao: UserDAO,
                               ws: WSClient) extends Controller with I18nSupport{

  val URL = "http://83.227.85.94:9000"

  implicit val eventWrites = new Writes[EventWithTimeStamp] {
    def writes(event: EventWithTimeStamp) = Json.obj(
      "eventName" -> event.eventName,
      "from" -> event.from.toString,
      "to" -> event.to.toString,
      "groupId" -> event.groupId
    )
  }

  implicit val eventReads: Reads[TempEvent] = (
    (JsPath \ "title").read[String] and
      (JsPath \ "from").read[String] and
      (JsPath \ "groupId").read[Int]
  )(TempEvent.apply _)

  /* implicit val placeReads: Reads[Place] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "location").read[Location]
    )(Place.apply _) */

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
      // val user = Await.result(userDao.getUserByUsername(username.get), 3.seconds)
      val jsonUser: JsonUser = getJsonUserFromServer(username.get)

      val user = User(0, jsonUser.username, jsonUser.password.get, jsonUser.admin, jsonUser.groupId)

      Ok(views.html.user.base(user, allGroups))
    } else {
      BadRequest(views.html.error("no session found, go log in"))
    }
  }

  implicit val jsonUserRead: Reads[JsonUser] = (
    (JsPath \ "username").read[String] and
      (JsPath \ "password").readNullable[String] and
      (JsPath \ "admin").readNullable[Boolean] and
      (JsPath \ "groupId").readNullable[Int]
    )(JsonUser.apply _)

  private def getJsonUserFromServer(username: String): JsonUser = {
    val response: Future[WSResponse] = ws.url(URL + "/users/username/" + username).withMethod("GET").get()

    val jsonUserList = Json.parse(Await.result(response, 5.seconds).json.toString())
    jsonUserList.validate[JsonUser].fold(
      errors => {
        throw new JsonException("wadafaka")
      },
      jsonUser => {
        // JsonUser(jsonUser.username, jsonUser.password, jsonUser.admin, jsonUser.groupId)
        jsonUser
      }
    )
  }




  def addEvent = Action { implicit request =>

    /* addEventform.bindFromRequest.fold(
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
    ) */
    Ok("not implemented")

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
      case None => {
        Logger.debug("lolwut")
        Future(Ok("i am completely out of the loop"))
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
        event => new EventWithTimeStamp(event.id, event.eventName, new DateTime(event.from), Some(new DateTime(event.to)), event.groupId)
      }

      val asJson = Json.toJson(withTimeStamps)
      Ok(asJson).withHeaders("Content-Type" -> "application/json; charset=utf-8") /* , "Access-Control-Allow-Credentials" -> "true", "Access-Control-Allow-Origin" -> "http://yourdomain.com") */

    } catch {
      case userNotFound: UserNotFoundException => NotFound("No user with id: " + userId)
      case userNoGroup: UserHasNoGroupException => BadRequest("User with id: " + userId + " has no group")
    }

    // Ok("dropped out of catch")

    }

  def saveEvent(userId: Int) = Action(BodyParsers.parse.json) { request =>
    val eventResult = request.body.validate[TempEvent]
    eventResult.fold(
      errors => {
        BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toJson(errors)))
      },
      event => {
        val middleMan = EventWithTimeStamp(None, event.eventName, new DateTime(event.from), None, Some(event.groupId))
        println(middleMan)

        val to = middleMan.to.map(
          value => value.getMillis
        ).getOrElse(0L)

        val dbEvent = Event(Some(0), middleMan.eventName, middleMan.from.getMillis, to, middleMan.groupId)

        val eventIdOption = Await.result(userDao.addEvent(dbEvent), 3.seconds)

        if (eventIdOption.isDefined) {
          Ok(Json.obj("status" ->"OK", "message" -> ("Event '" + event.eventName + "' saved with id: " + eventIdOption.get) ))
        } else {
          BadRequest(Json.obj("status" -> "KO", "message" -> ("Could not persist event with name: " + event.eventName) ))
        }


      }

    )
  }

  case class JsonException(s: String) extends Exception

}

case class addEventFormData(eventName: String, from: DateTime, to: DateTime, fromClock: Int, toClock: Int, groupId: Int)


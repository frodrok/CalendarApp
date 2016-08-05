package controllers

import javax.inject.Inject

import dao.UserDAO
import model._
import org.joda.time.DateTime
import play.api.{Environment, Logger}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.libs.ws.{WSBody, WSClient, WSRequest, WSResponse}
import play.api.mvc.{Action, Controller}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import play.api.libs.functional.syntax._


class BaseController @Inject()(val messagesApi: MessagesApi,
                               userDao: UserDAO,
                               ws: WSClient,
                               implicit val environment: Environment) extends Controller with I18nSupport {

  val TITLE = "YHC3L kalender app"
  val URL = "http://83.227.85.94:9000"

  val registerUserForm: Form[UserRegisterData] = Form(
    mapping(
      "username" -> nonEmptyText(minLength = 5).verifying("Username is already taken", username => checkUsernameAvailable(username)),
      "password" -> nonEmptyText(minLength = 8),
      "isadmin" -> boolean,
      "groupid" -> optional(number)
    )(UserRegisterData.apply)(UserRegisterData.unapply)
  )

  val loginUserForm: Form[UserFormData] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(UserFormData.apply)(UserFormData.unapply) verifying("Username or password incorrect", userFormData => checkLoginWithServer(userFormData.username, userFormData.password))
  )

  def index = Action { implicit request =>
    Ok(views.html.index(TITLE)(registerUserForm)(loginUserForm))
  }

  def register = Action.async { implicit request =>
    registerUserForm.bindFromRequest.fold(
      formWithErrors => {
        Future(BadRequest(views.html.index(TITLE)(formWithErrors)(loginUserForm)))
      },
      userData => {
        /* dbcode val newUser = User(0, userData.username, userData.password, Some(userData.isAdmin), userData.groupId)
        userDao.add(newUser).onFailure { case ex => println("could not save user: " + ex.getMessage)}
        Redirect("/") */

        //val ar: Future[WSResponse] = ws.url(URL + "/validateuser").withMethod("GET").withBody(data).get()

        val payload = Json.obj(
          "username" -> userData.username,
          "password" -> userData.password,
          "admin" -> userData.isAdmin
        )

        var send: Future[WSResponse] = ws.url(URL + "/users").post(payload)

        send.map {
          response => {
            Logger.debug(response.toString)

            if (response.status == 201) {
              Redirect("/user").withSession("connected" -> userData.username)
            } else {
              Ok(response.status.toString)
            }
          }
        }

      }

    )

  }

  /* login with json */
  def login = Action.async { implicit request =>
    loginUserForm.bindFromRequest.fold(
      formWithErrors => {
        Future(BadRequest(views.html.index(TITLE)(registerUserForm)(formWithErrors)))
      },
      userData => {
          Future(Redirect("/user").withSession("connected" -> userData.username))
      }
    )
  }

  /* old reads switch if new one doesnt work
  implicit val jsonUserReads: Reads[JsonUser] = (
    (JsPath \ "username").read[String] and
      (JsPath \ "password").readNullable[String]
    )(JsonUser.apply _) */

  /* case class JsonUser(id: Option[Int], username: String, password: Option[String], admin: Option[Boolean], groupId: Option[Int]) */
  implicit val jsonUserRead: Reads[JsonUser] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "username").read[String] and
      (JsPath \ "password").readNullable[String] and
      (JsPath \ "admin").readNullable[Boolean] and
      (JsPath \ "groupId").readNullable[Int]
    )(JsonUser.apply _)

  private def checkUsernameAvailable(username: String): Boolean = {
    val response = ws.url(URL + "/users").get()

    /* val userList: List[JsonUser] = Await.result(response.map {
      response => {
        val json = Json.parse(response.json.toString())

        json.validate[List[JsonUser]]
      }, 3.seconds)
    } */

    /* wait for result for 5 seconds because it crashed on first run */
    val jsonUserList = Json.parse(Await.result(response, 5.seconds).json.toString())
    jsonUserList.validate[List[JsonUser]].fold(
      error => {
        throw JsonException("Could not parse retrieved json in basecontroller.checkusernameavailable")
      },
      two => {
        val userOption = two.filter(_.username == username).headOption
        userOption match {
          case Some(user) => {
            false
          }
          case None => {
            true
          }
        }
      }
    )

  }

  def getURL: String = {
    URL
  }

  case class JsonException(s: String) extends Exception

  private def checkLoginWithServer(username: String, password: String): Boolean = {
    val data = Json.obj(
      "username" -> username,
      "password" -> password
    )

    val ar: Future[WSResponse] = ws.url(URL + "/validateuser").withMethod("GET").withBody(data).get()

    Await.result(ar, 5.seconds).status == 200
  }

  private def loginUser(user: User, dbUser: User): Boolean = {
    dbUser.username == user.username && dbUser.password == user.password
  }

  def getUsers = Action.async {
    userDao.allUsers.map(
      users => Ok(views.html.allusers(users))
    )
  }

  def setup = Action {
    // userDao.setup
    Ok("db initiated")
  }

  def test = Action {
    Ok("test")

  }

}

case class UserRegisterData(username: String, password: String, isAdmin: Boolean, groupId: Option[Int])
case class UserFormData(username: String, password: String)


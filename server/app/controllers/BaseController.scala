package controllers

import javax.inject.Inject

import model._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{Action, Controller}
import play.api.{Environment, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


class BaseController @Inject()(val messagesApi: MessagesApi,
                               ws: WSClient,
                               implicit val environment: Environment,
                               config: play.api.Configuration) extends Controller with I18nSupport {

  val TITLE = "YHC3L kalender app"
  // val URL = "http://83.227.85.94:9000"
  val URL: String = config.getString("custom.restAPIURL") match {
    case None => throw new Exception("String 'restAPIURL' not found in conf")
    case Some(string) => string
  }

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
    Ok(views.html.index.index()(registerUserForm)(loginUserForm))
  }

  def register = Action.async { implicit request =>
    registerUserForm.bindFromRequest.fold(
      formWithErrors => {
        Future(BadRequest(views.html.index.index()(formWithErrors)(loginUserForm)))
      },
      userData => {

        val payload = Json.obj(
          "username" -> userData.username,
          "password" -> userData.password,
          "admin" -> userData.isAdmin
        )

        val send: Future[WSResponse] = ws.url(URL + "/users").post(payload)

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
        Future(Ok(views.html.index.index()(registerUserForm)(formWithErrors)))
      },
      userData => {
          Future(Redirect("/user").withSession("connected" -> userData.username))
      }
    )
  }

  implicit val jsonUserRead: Reads[JsonUser] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "username").read[String] and
      (JsPath \ "password").readNullable[String] and
      (JsPath \ "admin").readNullable[Boolean] and
      (JsPath \ "groupId").readNullable[Int] and
        (JsPath \ "superAdmin").readNullable[Boolean]
    )(JsonUser.apply _)

  implicit val jsonGroupRead: Reads[JsonGroup] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "groupName").read[String] and
      (JsPath \ "active").readNullable[Boolean]
    )(JsonGroup.apply _)

  private def checkUsernameAvailable(username: String): Boolean = {
    val response = ws.url(URL + "/users").get()

    /* wait for result for 5 seconds because it crashed on first run */
    val jsonUserList = Json.parse(Await.result(response, 5.seconds).json.toString())
    jsonUserList.validate[List[JsonUser]].fold(
      error => {
        throw JsonException("Could not parse retrieved json in basecontroller.checkusernameavailable")
      },
      two => {
        val userOption = two.find(_.username == username)
        userOption match {
          case Some(user) => false
          case None => true
          }
        }
    )

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


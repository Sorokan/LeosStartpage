package controllers

import play.api._
import play.api.Play.current
import play.api.mvc._
import play.api.libs.ws.WS
import play.api.libs.iteratee.Enumerator
import com.google.gdata.client.calendar.CalendarService
import java.net.URL
import com.google.gdata.client.calendar.CalendarQuery
import org.joda.time.LocalDate
import com.google.gdata.data.calendar.CalendarEventFeed
import java.text.SimpleDateFormat
import scala.collection.JavaConversions._
import org.joda.time.DateTime
import play.api.libs.json.Json
import javax.mail.Folder
import javax.mail.search.FlagTerm
import com.sun.mail.imap.protocol.FLAGS
import javax.mail.Flags
import java.util.Date
import play.api.cache.Cached
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.HttpWebConnection
import com.gargoylesoftware.htmlunit.WebResponse
import com.gargoylesoftware.htmlunit.WebRequest
import com.gargoylesoftware.htmlunit.html.HtmlPage
import be.roam.hue.doj.Doj
import org.apache.commons.io.IOUtils
import java.io.File
import org.apache.commons.io.FileUtils
import scala.util.control.Breaks._
import com.google.gson.JsonObject
import scala.util.matching.Regex
import org.joda.time.format.DateTimeFormat
import org.apache.commons.lang3.StringUtils
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.JsNull
import play.api.libs.json.JsValue
import play.api.cache.Cache
import scala.concurrent.Future
import model.HooolpConcerts
import model.WrongPassword
import model.Users
import model.UnknownUser
import model.InvalidJson
import model.GoogleCalendar
import model.GoogleMail
import model.SimfyPlaylist
import org.joda.time.LocalDateTime

case class UserPassword(name: String, password: String)
case class Config(json: String)

class NoCookie extends Exception {}

object Application extends Controller {

  val maxAgeSeconds = 60*2
    
  val userForm = Form(
    mapping(
      "name" -> text,
      "password" -> text)(UserPassword.apply)(UserPassword.unapply))

  val configForm = Form(
    mapping(
      "json" -> text)(Config.apply)(Config.unapply))

  object UserPasswordCookie {
    val cookieName = "UserPassword"
    val delimiter = "#" //\u0000	

    def get(request: Request[_]): UserPassword = {
      val cookie = request.cookies.get(cookieName).getOrElse(null)
      if (cookie == null) {
        throw new NoCookie
      } else {
        val str = cookie.value
        val parts = str.split(delimiter)
        UserPassword(parts(0), parts(1))
      }
    }

    def cookie(name: String, password: String) = Cookie(cookieName, name + delimiter + password)

  }

  def index = Action { request =>
    {
      try {
        val user = UserPasswordCookie.get(request)
        val config = Users.getConfig(user.name, user.password)
        Ok(views.html.index(user.name,Json.stringify(config)))
      } catch {
        case e: UnknownUser => Redirect(routes.Application.showLogin("Unknown User"))
        case e: NoCookie => Redirect(routes.Application.showLogin("No Cookie"))
        case e: WrongPassword => Redirect(routes.Application.showLogin("Wrong Password"))
      }
    }
  }

  def logout() = Action { implicit request =>
    {
      Redirect(routes.Application.showLogin("")).discardingCookies(new DiscardingCookie(UserPasswordCookie.cookieName)).withNewSession
    }
  }

  def showLogin(message: String) = Action {
    val user = userForm.bind(Map("name" -> "", "password" -> ""))
    Ok(views.html.login(user, message))
  }

  def login() = Action { implicit request =>
    {
      val user = userForm.bindFromRequest.get
      try {
        val config = Users.getConfig(user.name, user.password)
        Redirect(routes.Application.index).withCookies(UserPasswordCookie.cookie(user.name, user.password))
      } catch {
        case e: UnknownUser => Redirect(routes.Application.editConfig("Please set your initial config")).withCookies(UserPasswordCookie.cookie(user.name, user.password))
        case e: WrongPassword => Redirect(routes.Application.showLogin("Wrong Password"))
      }
    }
  }

  def editConfig(message: String) = Action { request =>
    {
      try {
        val user = UserPasswordCookie.get(request)
        var config: JsValue = JsNull
        try {
          config = Users.getConfig(user.name, user.password)
        } catch {
          case e: UnknownUser => Redirect(routes.Application.showLogin("Unknown User"))
        }
        val cForm = configForm.bind(Map("json" -> Json.prettyPrint(config)))
        Ok(views.html.config(cForm, message))
      } catch {
        case e: NoCookie => Redirect(routes.Application.showLogin("No Cookie"))
      }
    }
  }

  def setConfig() = Action { implicit request =>
    {
      val user = UserPasswordCookie.get(request)
      val config = configForm.bindFromRequest.get
      try {
        Users.setConfig(user.name, user.password, config.json)
        Redirect(routes.Application.index())
      } catch {
        case e: InvalidJson => Redirect(routes.Application.editConfig(e.getMessage()))
        case e: WrongPassword => Redirect(routes.Application.showLogin("Wrong Password"))
        case e: UnknownUser => Redirect(routes.Application.showLogin("Unknown User"))
      }
    }
  }

  def getGoogleCalendarEntries(userName: String, userPassword: String, days: Int) = Cached("GoogleCalendar." + userName, maxAgeSeconds) {
    Action {
      Ok(GoogleCalendar.getGoogleCalendarEntries(userName, userPassword, days))
    }
  }

  def getMailEntries(userName: String, userPassword: String, maxItems: Int, numOfDays: Int) = Cached("Mail." + userName, maxAgeSeconds) {
    Action {
      Ok(GoogleMail.getMailEntries(userName, userPassword, maxItems, numOfDays))
    }
  }

  def getConcerts(userName: String, userPassword: String) = Cached("concerts/" + userName, maxAgeSeconds) {
    Action {
      val concerts = HooolpConcerts.getConcerts()
      val df = DateTimeFormat.forPattern("dd.MM HH:mm")

      def normalize(s: String) = s.toLowerCase().replaceAll("[^A-Za-z0-9]", "")

      case class Artist(name: String, url: String)
      
      val playlists = SimfyPlaylist.getSimfyPlaylists(userName, userPassword)
      val artists = playlists
        .flatMap(p => p.tracks.map(t => (t.artist->Artist(t.artist,t.artistUrl)))).toMap
        // ohne klassische Orchesterbesetzungen...
        .filterNot(s => s._1.contains('[') && s._1.contains('&'))
        // einzelne Interpreten
        .flatMap(s => s._1.split("feat.")
        .map(n=>(normalize(s._1)->s._2)))

      val concertsInPlaylist = concerts.allEvents.flatMap(t => t._2).map(c => 
        (c.artist.split("feat.").map(x=>normalize(x)).filter(x=>artists.containsKey(x)).map(x=>artists(x))->c)
      ).filter(x=> !(x._1.isEmpty)).flatMap(x=>x._1.map(y=>(y->x._2)))

      implicit def dateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

      val concertJson = Json.toJson(concertsInPlaylist.groupBy(t => t._1).map(x=>(x._1,x._2.map(y=>y._2))).toList.sortBy(x=>x._1.name).map(group => Json.toJson(Map(
        "artist" -> Json.toJson(Map(
            "name" -> Json.toJson(group._1.name),
            "url" -> Json.toJson(group._1.url))),
        "events" -> Json.toJson(group._2.toList.sortBy(c=>c.time).map(c => Json.toJson(Map(
          "time" -> Json.toJson(df.print(c.time)),
          "club" -> Json.toJson(c.club),
          "city" -> Json.toJson(c.city),
          "url" -> Json.toJson(c.url)))))))))

      Ok(concertJson)
    }
  }

  def proxyGet(url: String) = {
    
    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    val result = Cache.getOrElse[(Int, Map[String, String], Array[Byte])](url)(null)

    val future =
      if (result == null) {
        WS.url(url).get().map { response =>
          val headers = response.getAHCResponse.getHeaders()
          val headerMap = headers.iterator().map(entry => entry.getKey() -> entry.getValue().mkString(",")).toMap
            .filterKeys(k => (!List(
              "Transfer-Encoding",
              "Referer",
              "Set-Cookie",
              "Cache-Control",
              "Last-Modified",
              "ETag",
              "Expires").contains(k))).+(("Cache-Control"->("public,max-age="+maxAgeSeconds)))
          val r = Tuple3(response.status, headerMap, response.ahcResponse.getResponseBodyAsBytes())
          Cache.set(url, r, maxAgeSeconds)
          r
        }
      } else {
        Future {
          result
        }
      }

    Action.async(
      future.map(t => SimpleResult(
        header = ResponseHeader(t._1, t._2),
        body = Enumerator(t._3))))

  }

}

package model
import scala.collection.JavaConversions._
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.html.HtmlPage
import be.roam.hue.doj.Doj
import scala.util.control.Breaks._
import org.joda.time.LocalDateTime
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Json

case class Track(title: String, artist: String, artistUrl: String)
case class PlayList(title: String, tracks: Seq[Track])

object SimfyPlaylist {

  case class UserPlaylist(val userName: String, var playlists: List[PlayList], var lastRuntime: LocalDateTime)

  import JsonFormatters._

  implicit val trackFormat = Json.format[Track]
  implicit val playListFormat = Json.format[PlayList]
  implicit val userPlayListFormat = Json.format[UserPlaylist]

  var userStoreMap: Map[String, SimpleJsonStore[UserPlaylist]] = Map()

  def getSimfyPlaylists(userName: String, userPassword: String): Seq[PlayList] = {

    var store = userStoreMap.getOrElse(userName, null)
    if (store == null) {
      store = new SimpleJsonStore[UserPlaylist]("playlists/" + userName.replaceAll("[^A-Za-z0-9]", "_"))
      userStoreMap = userStoreMap + (userName -> store)
    }
    store.synchronized {
      var userPlaylist = store.get()
      var needUpdate = false;
      if (userPlaylist == null) {
        userPlaylist = UserPlaylist(userName, List(), new LocalDateTime)
        needUpdate = true;
      }
      if (userPlaylist.lastRuntime.plusDays(1).isBefore(new LocalDateTime)) {
        userPlaylist.lastRuntime = new LocalDateTime
        needUpdate = true;
      }
      if (needUpdate) {
        userPlaylist.playlists = callAndGetSimfyPlaylists(userName, userPassword)
        store.set(userPlaylist)
      }
      userPlaylist.playlists
    }
  }

  def callAndGetSimfyPlaylists(userName: String, userPassword: String) = {
    val webClient = new WebClient(BrowserVersion.FIREFOX_17);

    webClient.getOptions().setThrowExceptionOnScriptError(false);
    /*
    webClient.setWebConnection(new HttpWebConnection(webClient) {
      override def getResponse(request: WebRequest): WebResponse = {
	     System.out.println("request--------------------------------------------");
	     System.out.println(request.getHttpMethod()+" "+request.getUrl());
	     System.out.println(request.getRequestParameters());
	     System.out.println(request.getRequestBody());         
	     super.getResponse(request);
      }
    })
*/
    val loginPage: HtmlPage = webClient.getPage("http://www.simfy.de/start");

    val afterLoginButtonPage: HtmlPage = Doj.on(loginPage).get("#login_link").click().asInstanceOf[HtmlPage];

    // Get the inputs in the login form through simple CSS selectors
    val form = Doj.on(afterLoginButtonPage).get("#new_user_session");
    form.getById("user_session_email").value(userName);
    form.getById("user_session_password").value(userPassword);

    val startPage: HtmlPage = form.get("#login_control .btn.blue_OL").click().asInstanceOf[HtmlPage];
    val error = Doj.on(startPage).get("#login_error").text();

    if (error != null && error != "") {
      throw new Exception(error)
    }

    webClient.getOptions().setJavaScriptEnabled(false);

    //FileUtils.writeStringToFile(new File("/tmp/simfy-start.html"), startPage.getWebResponse().getContentAsString())

    val playlistsLink = Doj.on(startPage).get("a").filter(a => a.attribute("href").equals("/profiles/sorokan/playlists")).head

    def getAllPlaylistPages(playlistPage: HtmlPage, pagesSoFar: List[HtmlPage]): List[HtmlPage] = {
      val playlistElements = Doj.on(playlistPage).get("li.playlist a")
      val playlistPages = pagesSoFar ++ playlistElements.map(link => link.click().asInstanceOf[HtmlPage])
      val playlistNextPageLink = Doj.on(playlistPage).get(".pagination .next a")
      if (playlistNextPageLink.isEmpty()) {
        playlistPages
      } else {
        val newpage = playlistNextPageLink.first().click().asInstanceOf[HtmlPage];
        getAllPlaylistPages(newpage, playlistPages)
      }
    }

    var playlistsPage: HtmlPage = playlistsLink.click().asInstanceOf[HtmlPage];
    val allPlaylistPages = getAllPlaylistPages(playlistsLink.click().asInstanceOf[HtmlPage], List())

    val playlists = allPlaylistPages.map(page =>
      PlayList(title = Doj.on(page).get(".playlist_title").text(),
        tracks = Doj.on(page).get(".track").map(track =>
          Track(
            title = track.get(".track_title_and_version_title").text(),
            artist = track.get(".artist_name a").attribute("title"),
            artistUrl = "http://www.simfy.de"+track.get(".artist_name a").attribute("href"))).toSeq))

    playlists
  }

}
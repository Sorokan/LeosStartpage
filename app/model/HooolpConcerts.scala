package model
import org.joda.time.LocalDate
import scala.collection.JavaConversions._
import play.api.libs.json.Json
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.HttpWebConnection
import com.gargoylesoftware.htmlunit.WebResponse
import com.gargoylesoftware.htmlunit.WebRequest
import com.gargoylesoftware.htmlunit.html.HtmlPage
import scala.util.control.Breaks._
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import scala.util.Random
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import JsonFormatters.formatLocalDate

case class Concert(artist: String, time: LocalDateTime, city: String, club: String, url: String)

case class Concerts(lastRuntime: LocalDateTime, allEvents: Map[LocalDate, List[Concert]], datesToGo: List[LocalDate]) {
  def needsUpdate = lastRuntime.plusDays(4).isBefore(new LocalDateTime)
}

object HooolpConcerts {

  import JsonFormatters._

  implicit val formatConcert = Json.format[Concert]
  implicit val formatMyMap =
    new Format[Map[LocalDate, List[Concert]]] {
      val fmt = DateTimeFormat.forPattern("dd.MM.yyyy")
      def reads(js: JsValue): JsResult[Map[LocalDate, List[Concert]]] = JsSuccess(
        js.as[JsObject].fieldSet.map(t => fmt.parseLocalDate(t._1) -> t._2.as[List[Concert]]).toMap)
      def writes(o: Map[LocalDate, List[Concert]]): JsValue =
        Json.obj(o.map(t => fmt.print(t._1) -> Json.toJsFieldJsValueWrapper(t._2)).toSeq: _*)
    }
  implicit val formatConcerts = Json.format[Concerts]

  val store = new SimpleJsonStore[Concerts]("concerts")

  def writeResponseHtml(request: WebRequest, response: WebResponse) {
    def requestAsFileName(request: WebRequest) = (request.getHttpMethod().name() + " " + request.getUrl().toString()).replaceAll("[^A-Za-z0-9]", "_").take(100) + ".html"
    //println(requestAsFileName(request))
    //FileUtils.writeStringToFile(new File("dump", requestAsFileName(request)), response.getContentAsString())
  }

  def getConcerts(): Concerts = {
    this.synchronized {
      val concerts = store.get
      if (concerts == null || concerts.needsUpdate) {
        def datesUntil(dat: LocalDate, end: LocalDate): List[LocalDate] = if (dat.equals(end)) List(dat) else dat +: datesUntil(dat.plusDays(1), end)
        val newConcerts = new Concerts(
          new LocalDateTime,
          if (concerts == null) Map() else concerts.allEvents,
          datesUntil(new LocalDate(), new LocalDate().plusMonths(12).plusDays(1)))
        store.set(newConcerts)
        newConcerts
      } else {
        concerts
      }
    }
  }

  def updateConcerts(date: LocalDate) {
    this.synchronized {
      val concerts = getConcerts()
      val newConcerts = new Concerts(
        concerts.lastRuntime,
        concerts.allEvents,
        concerts.datesToGo diff List(date))
      store.set(newConcerts)
    }
  }

  def updateConcerts(events: List[Concert], date: LocalDate) {
    this.synchronized {
      val concerts = getConcerts()
      val newConcerts = new Concerts(
        concerts.lastRuntime,
        concerts.allEvents - date + (date -> events),
        concerts.datesToGo diff List(date))
      store.set(newConcerts)
    }
  }

  val getterThread = new Thread() {
    {
      this.setDaemon(true)
      this.setName("HooolpConcerts")
    }
    override def run() {
      val rnd = new Random()
      while (true) {
        val concerts = getConcerts()
        if (concerts.datesToGo.isEmpty) {
          sleep(60 * 60 * 24)
        } else {
          triggerGetDate(concerts.datesToGo.head)
          sleep(rnd.nextInt(60 * 5))
        }
      }
    }

    def sleep(secs: Int) {
      println("Hooolp Thread sleeps for " + secs + " secs")
      Thread.sleep(secs * 1000)
    }
  }.start()

  def triggerGetDate(date: LocalDate) {
    updateConcerts(date)
    val webClient = new WebClient(BrowserVersion.FIREFOX_17);
    var jsonUrls = Map[String, LocalDate]()
    def parseEvents(jsonString: String, date: LocalDate) = {
      val df = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm");
      def parseDate(dateStr: String, timeStr: String) = {
        df.parseDateTime(if (timeStr == "tba") {
          dateStr + " 00:00"
        } else {
          dateStr + " " + timeStr
        }).toLocalDateTime()
      }
      val json = Json.parse(jsonString)
      val events = (json \\ "event").flatMap(event => (event \\ "band").map(band => Concert(
        artist = (band \ "name").asOpt[String].getOrElse(null),
        time = parseDate((event \ "startDate").asOpt[String].getOrElse(null), (event \ "startTime").asOpt[String].getOrElse(null)),
        city = (event \ "location" \ "city").asOpt[String].getOrElse(null),
        club = (event \ "location" \ "name").asOpt[String].getOrElse(null),
        url = "http://www.hooolp.com/" + (event \ "alias").asOpt[String].getOrElse(null)
        ))).toList
      updateConcerts(events, date)
    }

    webClient.getOptions().setThrowExceptionOnScriptError(false);
    webClient.setWebConnection(new HttpWebConnection(webClient) {
      override def getResponse(request: WebRequest): WebResponse = {
        val response = super.getResponse(request);
        val url = request.getUrl().toString()
        if (jsonUrls.contains(url)) {
          parseEvents(response.getContentAsString(), jsonUrls.getOrElse(url, null))
        }
        response
      }
    })

    val firstPage: HtmlPage = webClient.getPage("http://www.hooolp.com");

    def doEventsRequest(date: LocalDate) = {
      val formattedDate = date.toString("dd-MM-yyyy")
      println("hooolp abfrage für " + formattedDate)
      val plz = "44267"
      val url = "http://www.hooolp.com/acp/lib/api2.0/response.php?restRequestSubject=events&restRequestParam=events&restRequestType=json&q="+plz+"&range=100&date=" + formattedDate + "&ordering=ranking"
      jsonUrls = jsonUrls + (url -> date)
      firstPage.executeJavaScript("$.getJSON(\"" + url + "\",function(data){console.log(data);})");
    }

    doEventsRequest(date)
  }

}
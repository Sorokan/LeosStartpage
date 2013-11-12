package model

import com.google.gdata.client.calendar.CalendarService
import com.google.gdata.client.calendar.CalendarQuery
import org.joda.time.LocalDate
import java.text.SimpleDateFormat
import java.net.URL
import org.joda.time.DateTime
import com.google.gdata.data.calendar.CalendarEventFeed
import play.api.libs.json.Json
import scala.collection.JavaConversions._
import com.google.gdata.data.calendar.CalendarFeed
import org.apache.commons.lang3.StringUtils
import com.google.gdata.data.calendar.CalendarEventFeed
import com.google.gdata.data.calendar.CalendarFeed
import play.api.libs.json.Json.toJsFieldJsValueWrapper

object GoogleCalendar {

  def getGoogleCalendarEntries(userName: String, userPassword: String, days: Int) = {
    val APP_ID = "myhomepage-getGoogleCalendarEntries"
    val METAFEED_URL_BASE = "https://www.google.com/calendar/feeds/"
    val EVENT_FEED_URL_SUFFIX = "/private/full"

    val service = new CalendarService(APP_ID);
    service.setUserCredentials(userName, userPassword);

    val start = LocalDate.now();
    val until = start.plusDays(days);
    val dayf = new SimpleDateFormat("EE");
    val df = new SimpleDateFormat("dd.MM");
    val tf = new SimpleDateFormat("HH:mm");

    val feedUrl = new URL("https://www.google.com/calendar/feeds/default/allcalendars/full")
    val feeds = service.getFeed(feedUrl, classOf[CalendarFeed]).getEntries().flatMap(entry => {
      val id = StringUtils.substringAfterLast(entry.getId(), "/")
      val eventFeedUrl = new URL("http://www.google.com/calendar/feeds/" + id + "/private/full")
      val myQuery = new CalendarQuery(eventFeedUrl)
      myQuery.setMinimumStartTime(new com.google.gdata.data.DateTime(start.toDate()))
      myQuery.setMaximumStartTime(new com.google.gdata.data.DateTime(until.toDate()))
      val resultFeed = service.query(myQuery, classOf[CalendarEventFeed])
      resultFeed.getEntries()
    })

    case class Entry(val timestamp: DateTime, val text: String)

    implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

    Json.arr(
      feeds.map(entry => Entry(
        new DateTime(entry.getTimes().get(0).getStartTime().getValue()),
        entry.getTitle().getPlainText()))
        .filter(e => e.timestamp.isAfter(start.toDateMidnight()))
        .sortBy(e => e.timestamp)
        .map(e => Json.obj(
          "day" -> dayf.format(e.timestamp.toDate()),
          "date" -> df.format(e.timestamp.toDate()),
          "time" -> tf.format(e.timestamp.toDate()),
          "text" -> e.text)))
  }
}
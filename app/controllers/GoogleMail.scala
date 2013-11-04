package controllers

import java.text.SimpleDateFormat
import org.joda.time.LocalDate
import javax.mail.search.FlagTerm
import javax.mail.Folder
import javax.mail.Flags
import play.api.libs.json.Json

object GoogleMail {

  def getMailEntries(userName: String, userPassword: String, maxItems: Int, numOfDays: Int) = {
    val props = System.getProperties();
    props.setProperty("mail.store.protocol", "imaps");
    val session = javax.mail.Session.getDefaultInstance(props, null);
    val store = session.getStore("imaps");
    store.connect("imap.gmail.com", userName, userPassword);
    try {
      val folder = store.getFolder("inbox");
      folder.open(Folder.READ_ONLY);
      try {
        val dayf = new SimpleDateFormat("EE");
        val df = new SimpleDateFormat("dd.MM");
        val tf = new SimpleDateFormat("HH:mm");
        val messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        val start = LocalDate.now().minusDays(numOfDays-1).toDate();
        Json.arr(
          messages.reverse
            .take(30)
            .filter(m => m.getSentDate().after(start))
            .take(maxItems)
            .map(message =>
              Json.obj(
                "day" -> dayf.format(message.getSentDate()),
                "date" -> df.format(message.getSentDate()),
                "time" -> tf.format(message.getSentDate()),
                "from" -> message.getFrom()(0).toString(),
                "subject" -> message.getSubject())))
      } finally {
        folder.close(true);
      }
    } finally {
      store.close();
    }
  }

}
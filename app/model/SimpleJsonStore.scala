package model
import play.api.libs.json._
import play.api.libs.functional.syntax._
import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import play.api.libs.json.Json.toJsFieldJsValueWrapper

object JsonFormatters {
  implicit val formatLocalDate = new Format[LocalDate] {
    val fmt = DateTimeFormat.forPattern("dd.MM.yyyy")
    def reads(js: JsValue): JsResult[LocalDate] = JsSuccess(fmt.parseLocalDate((js \ "value").as[String]))
    def writes(o: LocalDate): JsValue = Json.obj("value" -> fmt.print(o))
  }
  implicit val formatLocalDateTime = new Format[LocalDateTime] {
    val fmt = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm")
    def reads(js: JsValue): JsResult[LocalDateTime] = JsSuccess(fmt.parseLocalDateTime((js \ "value").as[String]))
    def writes(o: LocalDateTime): JsValue = Json.obj("value" -> fmt.print(o))
  }
}

class SimpleJsonStore[T](val name: String)(implicit val reads: OFormat[T]) {

  private var value: T = null.asInstanceOf[T]
  private val store = new FileStore(name + ".json")

  def get()(implicit ev: Manifest[T]): T = {
    this.synchronized {
      if (value == null) {
        value = read()
      }
      value
    }
  }

  def set(v: T) {
    this.synchronized {
      value = v
      write(value)
    }
  }

  private def read()(implicit ev: Manifest[T]): T = {
    val jsonStr = store.read
    if (jsonStr == null) {
      null.asInstanceOf[T]
    } else {
      val json = Json.parse(jsonStr)
      val jsResult: JsResult[T] = Json.fromJson(json)
      return jsResult.get
    }
  }

  private def write(value: T) {
    val json = Json.toJson(value)
    val jsonStr = Json.prettyPrint(json)
    store.write(jsonStr)
  }

}
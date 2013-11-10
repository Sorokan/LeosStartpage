package model

import play.api.libs.json.Json
import org.jasypt.util.text.BasicTextEncryptor
import play.api.libs.json._

class WrongPassword extends Exception {}
class UnknownUser extends Exception {}
case class InvalidJson(message: String) extends Exception(message)

object Users {

  def getConfig(userName: String, userPassword: String): JsValue = {
    val value = store(userName).read
    if (value == null)
      throw new UnknownUser
    else
      try {
        Json.parse(decrypt(value, userPassword))
      } catch {
        case e: Exception => throw new WrongPassword
      }
  }

  def setConfig(userName: String, userPassword: String, jsonConfig: String) {
    try {
      // check if user is authorized
      getConfig(userName, userPassword)
    } catch {
      case e: UnknownUser => ;
    }
    try {
      Json.parse(jsonConfig)
    } catch {
      case e: Exception => throw new InvalidJson(e.getMessage())
    }
    store(userName).write(encrypt(jsonConfig, userPassword))
  }

  def encryptor(key: String) = {
    val textEncryptor = new BasicTextEncryptor()
    textEncryptor.setPassword(key)
    textEncryptor
  }

  def encrypt(content: String, key: String) = encryptor(key).encrypt(content)
  def decrypt(content: String, key: String) = encryptor(key).decrypt(content)

  def store(userName: String) = new FileStore("users/" + userName.replaceAll("[^A-Za-z0-9]", "_") + ".json")

}
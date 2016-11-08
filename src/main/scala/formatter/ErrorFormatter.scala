package formatter

import play.api.libs.functional.syntax._
import play.api.libs.json.{Writes, JsPath}

case class Error(code: Int, message: String)

object ErrorFormatter {

  val errorWriter: Writes[Error] = (
    (JsPath \ "code").write[Int] and
      (JsPath \ "message").write[String]
    )(unlift(Error.unapply))

}

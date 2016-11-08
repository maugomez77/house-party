package controllers

import javax.inject.Inject

import formatter._
import play.api.http.HttpEntity
import play.api.{Logger, Configuration}
import play.api.libs.ws.{StreamedResponse, WSResponse, WSClient}
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}

class FriendsController @Inject()(implicit context: ExecutionContext,
                                  config: Configuration,
                                  wsClient: WSClient) extends Controller {

  implicit val error = ErrorFormatter.errorWriter

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  case class FriendNotFriend(fromId: String, fromName: String,
                             toId: String, toName: String,
                             areFriends: Boolean)

  implicit val friendInboundReader: Reads[FriendNotFriend] = (
    (JsPath \ "from" \ "id").read[String] and
      (JsPath \ "from" \ "name").read[String] and
      (JsPath \ "to" \ "id").read[String] and
      (JsPath \ "to" \ "name").read[String] and
      (JsPath \ "areFriends").read[Boolean]
    )(FriendNotFriend.apply _)

  private def callService: Future[WSResponse] = {
    //val remoteServiceURL = "http://localhost/friends/data.html"//config.getString("houseparty.basepath").getOrElse("https://immense-river-17982.herokuapp.com/?since=1352102565590")
    val remoteServiceURL = "https://immense-river-17982.herokuapp.com/?since=1352102565590"
    Logger.debug("Remote Service call " + remoteServiceURL)
    wsClient.url(remoteServiceURL).get
  }

  /**
   * This solution was intended to provide for bigger chunks
   * but still unable to read the big chunk response
   *
   * @param userId
   * @return
   */
  def downloadFile(userId: String) = Action.async {

    // Make the request
    wsClient.url("http://localhost/friends/data.html").withMethod("GET").stream().map {
      case StreamedResponse(response, body) =>

        // Check that the response was successful
        if (response.status == 200) {

          // Get the content type
          val contentType = response.headers.get("Content-Type").flatMap(_.headOption)
            .getOrElse("application/octet-stream")

          // If there's a content length, send that, otherwise return the body chunked
          response.headers.get("Content-Length") match {
            case Some(Seq(length)) =>

              Ok.sendEntity(HttpEntity.Streamed(body, Some(length.toLong), Some(contentType)))
            case _ =>
              Ok.chunked(body).as(contentType)
          }
        } else {
          BadGateway
        }
    }
  }

  /**
   * This is the solution that is running
   *
   * I cannot parse to json cause is not a json the output
   * of the service, so i needed to do some splits
   * and then convert line by line into a json
   *
   * @param userId
   * @return
   */
  def countFriends(userId: String) = Action.async { request =>

    val futureTemplate = callService
    futureTemplate.map {
      wsResponse => {

        val wellFormedJson: Array[java.lang.String] = wsResponse.body.split("\n")
        val map = scala.collection.mutable.HashMap.empty[String,Boolean]
        wellFormedJson.map { ele =>

          val jsonRead: JsValue = Json.parse(ele)

          val fromFriendId: String = (jsonRead \ "from" \ "id").asOpt[String].getOrElse("")
          //val fromFriendName: String = (jsonRead \ "from" \ "name").asOpt[String].getOrElse("")

          val toFriendId: String = (jsonRead \ "to" \ "id").asOpt[String].getOrElse("")
          val toFriendName: String = (jsonRead \ "to" \ "name").asOpt[String].getOrElse("")

          val areOrNotFriends: Boolean = (jsonRead \ "areFriends").asOpt[Boolean].getOrElse(false)

          //if the friends we are looking is interested
          if (!fromFriendId.equals("") && fromFriendId.equals(userId)) {
            map += (toFriendId + " " + toFriendName -> areOrNotFriends)
          }
        }

        val list: List[String] = map.map { display =>
          val str1: String = "This guy/lady: " + display._1 + " is "
          val str2: String = if (display._2) {
            "your friend"
          } else {
            "not your friend"
          }
          str1 + str2
        }.toList

        Ok(list.mkString("\n\r"))
      }
    }.recover {
      case e: Exception => RequestTimeout(Json.toJson(Error(REQUEST_TIMEOUT, "Error : " + e.toString)))
    }
  }
}

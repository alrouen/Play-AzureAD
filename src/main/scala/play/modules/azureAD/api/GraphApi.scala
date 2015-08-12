package play.modules.azureAD.api

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.Play.current
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.libs.ws._
import play.api.cache._

import play.modules.azureAD.{Tenant, Client, Group, AzureADConfiguration, AzureADEndpoints}

/**
 * Created by alain on 10/08/15.
 */

class GraphApiUnauthorized(message: String = null) extends RuntimeException(s"AzureAD GraphAPI: ${message}")
class GraphApiException(message: String = null, cause: Throwable = null) extends RuntimeException(s"AzureAD GraphAPI: ${message}", cause)

class GraphApi(val azureADConfiguration: AzureADConfiguration) {

  lazy val cacheApi = current.injector.instanceOf[CacheApi]

  case class GraphTokenResponse(token_type:String, expires_in: String, expires_on: String, not_before: String, resource: String, access_token: String)
  object GraphTokenResponse { implicit val graphTokenResponseFormat = Json.format[GraphTokenResponse] }

  private def tokenUrl(tenant: Tenant): String = s"${AzureADEndpoints.authority}/${tenant.id}/oauth2/token?api-version=1.5"

  private def mkValidationError(error: Seq[(JsPath, Seq[ValidationError])]) = {
    error.foldLeft("") { (acc, item) =>
      val errors = item._2.map( e => e.message).mkString("\t- ", "\n", "")
      s"${acc}\n ${item._1.toString()}: \n${errors}"
    }
  }

  private def getAccessToken(tenant: Tenant, client: Client): Future[String] = {
    WS.url(tokenUrl(tenant)).post(client.graphApiPayload).map { r =>
      r.status match {
        case ok if ok < 400 => {
          r.json.validate[GraphTokenResponse].fold(
            invalid => throw new Exception(s"getAccessToken invalid response: ${r.status} ${r.body}\n json errors:${mkValidationError(invalid)}"),
            graphTokenResponse => graphTokenResponse.access_token
          )
        }
        case unauthorized if unauthorized == 401 => throw new GraphApiUnauthorized(s"getAccessToken unauthorized: ${r.body}")
        case _ => throw new GraphApiException(s"getAccessToken unexpected error: [${r.status}}] ${r.body}")
      }
    }
  }

  /*private def apiPost[A, B](url: String, payload: B, tenant: Tenant, client : Client)(implicit reads:Reads[A]): Future[A] = {
    WS.url(url).post(payload).map { r => r.json.validate[A].fold[A](
      invalid => throw new Exception(s"getAccessToken invalid response: ${r.status} ${r.body}\n json errors:${mkValidationError(invalid)}"),
      response => response
    )}
  }*/


  private def apiGet[A](url: String, tenant: Tenant, client : Client)(implicit reads:Reads[A]): Future[A] = {
    for {

        accessToken <- getAccessToken(tenant, client)

        response <- WS.url(url).withHeaders(("Authorization", s"Bearer ${accessToken}")).get()
          .recover { case e:Throwable => throw new GraphApiException(s"error will requesting API ${url}", e)}

    } yield {

      response.status match {
        case ok if ok < 400 => {
          response.json.validate[A].fold[A](
            invalid => throw new GraphApiException(s"invalid response error, response: ${response.body}\n json errors:${mkValidationError(invalid)}"),
            response => response
          )
        }
        case unauthorized if unauthorized == 401 => {
          throw new GraphApiException(s"api [${url}}] response error: [${response.status}] ${response.body}")
        }
        case _ => throw new GraphApiException(s"api [${url}}] response error: [${response.status}] ${response.body}")
      }

    }
  }

  /*** API ***/

  private case class GroupResponse(value: List[Group])
  private object GroupResponse {
    implicit val groupResponseFormat = Json.format[GroupResponse]
  }

  /**
   * To list all users
   * ie: https://graph.windows.net/mycloud.onmicrosoft.com/users?api-version=1.5
   */
  private def usersUrl(tenant: Tenant): String = s"${AzureADEndpoints.graphAPI}/${tenant.id}/users?api-version=1.5"

  /**
   * To list all groups
   * ie : https://graph.windows.net/mycloud.onmicrosoft.com/groups?api-version=1.5
   */
  private def groupsUrl(tenant: Tenant): String = s"${AzureADEndpoints.graphAPI}/${tenant.id}/groups?api-version=1.5"

  def allGroups(tenant: Tenant, client: Client): Future[List[Group]] = {
    apiGet[GroupResponse](groupsUrl(tenant), tenant, client).map { groups =>
      groups.value
    }
  }

  /**
   * To generate url for memberOf API request
   * ie: https://graph.windows.net/mycloud.onmicrosoft.com/users/foo.bar@mycloud.onmicrosoft.com/memberOf?api-version=1.5"
   * @param upn
   * @return url: String
   */
  private def memberOfUrl(upn: String, tenant: Tenant): String = s"${AzureADEndpoints.graphAPI}/${tenant.id}/users/${upn}/memberOf?api-version=1.5"

  def memberOf(upn: String)(tenant: Tenant, client: Client): Future[List[Group]] = {
    apiGet[GroupResponse](memberOfUrl(upn, tenant), tenant, client).map { groups =>
      groups.value
    }
  }

  private def isMemberOfCacheKey = (upn: String, groupId: String) => s"azureAD.isMemberOf.${upn}_${groupId}"

  def isMemberOf(upn: String, groupId: String)(tenant: Tenant, client: Client): Future[Boolean] = {

    cacheApi.getOrElse[Future[Boolean]](isMemberOfCacheKey(upn, groupId), DurationInt(azureADConfiguration.groupMembershipLifeTime).seconds) {
      memberOf(upn)(tenant, client).map { groups =>
        groups.exists(g => g.objectId == groupId)
      }
    }
  }

}

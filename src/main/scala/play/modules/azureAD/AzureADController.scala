package play.modules.azureAD

import play.modules.azureAD.api.{GraphApiUnauthorized, InvalidClaimsToken}

import scala.concurrent.{Future, ExecutionContext}

import play.api.mvc.{
  ActionBuilder,
  Controller,
  Request,
  WrappedRequest,
  Result
}

import play.api.Logger

/**
 * Created by alain on 10/08/15.
 */
trait AzureADController extends Controller {
  self: AzureADComponents =>

  class AzureADRequest[A](val username: String, request: Request[A]) extends WrappedRequest[A](request)

  def AzureADAction()(implicit tenant: Tenant, client: Client, appGroupId: String) = new ActionBuilder[AzureADRequest] {

    import ExecutionContext.Implicits.global

    def invokeBlock[A](request: Request[A], block: (AzureADRequest[A]) => Future[Result]) = {

      request.headers.get("Authorization") match {
        case Some(authorizationHeader) => {

          val jwt = authorizationHeader.substring(7) //To remove the "Bearer" prefix

          (for {
            claims <- azureADApi.claimsApi.jwtToClaims(jwt)(tenant)
            result <- {
              val upn = claims.getStringClaimValue("upn")
              azureADApi.graphApi.isMemberOf(upn, appGroupId)(tenant, client).flatMap {
                case true => block(new AzureADRequest(upn, request))
                case false => Future.successful(Unauthorized)
              }
            }
          } yield {
            result
          }).recover {

            case e:InvalidClaimsToken => {
              Logger.warn(s"AzureADAction invalid token: ${e.getMessage}")
              Unauthorized
            }

            case e:GraphApiUnauthorized => {
              Logger.warn(s"AzureADAction graphAPI authorized access: ${e.getMessage}")
              Unauthorized
            }

            case e:Throwable => {
              Logger.error("AzureADAction error", e)
              InternalServerError
            }

          }
        }
        case None => Future.successful(Unauthorized)
      }

    }
  }

}

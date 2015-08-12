package play.modules.azureAD.api

import java.security.cert.X509Certificate

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.keys.X509Util
import org.jose4j.keys.resolvers.X509VerificationKeyResolver

import play.api.Play.current
import play.api.cache._
import play.api.libs.json.Json
import play.api.libs.ws._

import play.modules.azureAD.{Tenant, AzureADConfiguration}

/**
 * Created by alain on 10/08/15.
 */

class InvalidClaimsToken(message: String = null, cause: Throwable = null) extends RuntimeException(s"AzureAD ClaimsAPI: ${message}", cause)
class ClaimsApiException(message: String = null, cause: Throwable = null) extends RuntimeException(s"AzureAD ClaimsAPI: ${message}", cause)

class ClaimsApi(val azureADConfiguration: AzureADConfiguration) {

  lazy val cacheApi = current.injector.instanceOf[CacheApi]

  private val x509 = new X509Util()

  private case class AadOpenIdConfiguration(issuer: String, authorization_endpoint: String, token_endpoint: String, jwks_uri: String)

  private object AadOpenIdConfiguration {
    implicit val aadOpenIdConfigurationFormat = Json.format[AadOpenIdConfiguration]
  }

  private case class AadKey(kty: String, use: String, kid: String, x5t: String, n: String, e: String, x5c: List[String])

  private object AadKey {
    implicit val aadKey = Json.format[AadKey]
  }

  private def cacheKey = (id: String) => s"azureAD.pubkeys.${id}"

  private def getJWTPublicKeys(tenant: Tenant): Future[List[AadKey]] = {

    cacheApi.getOrElse[Future[List[AadKey]]](cacheKey(tenant.id), DurationInt(azureADConfiguration.openIdPubKeyLifetime).seconds) {
      for {
        authorityConf <- WS.url(tenant.authorityUrlOpenIdConfiguration).get().map { r => r.json.as[AadOpenIdConfiguration] }
        authorityPubKeys <- WS.url(authorityConf.jwks_uri).get.map { r =>
          (r.json \ "keys").as[List[AadKey]]
        }
      } yield {
        authorityPubKeys
      }
    }
  }

  def jwtToClaims(jwt: String)(tenant: Tenant): Future[JwtClaims] = {

    getJWTPublicKeys(tenant).map { publicKeys =>

      val publicCerts = publicKeys.foldLeft(List[X509Certificate]())((certs, aadKey) => {
        aadKey.x5c.map {
          x509.fromBase64Der(_)
        } ::: certs
      })

      val certResolver = new X509VerificationKeyResolver(publicCerts)
      val jwtConsumer = new JwtConsumerBuilder()
        .setVerificationKeyResolver(certResolver)
        .setRequireExpirationTime()
        .setAllowedClockSkewInSeconds(30)
        .setRequireSubject()
        .setExpectedAudience(tenant.audience: _*)
        .build()

      try {
        jwtConsumer.processToClaims(jwt)
      } catch {
        case e: org.jose4j.jwt.consumer.InvalidJwtException => throw new InvalidClaimsToken(e.getMessage)
        case e: Throwable => throw new ClaimsApiException("error while processing claims", e)
      }

    }

  }

}

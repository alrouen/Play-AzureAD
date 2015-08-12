package play.modules.azureAD

/**
 * Created by alain on 10/08/15.
 */

case class Tenant(id: String, clientSecret: Map[String, Client]) {
  val tenantAuthorityUrl = s"${AzureADEndpoints.authority}/${id}"
  val authorityUrlOpenIdConfiguration = s"${tenantAuthorityUrl}/.well-known/openid-configuration"
  lazy val audience = clientSecret.keySet.toList
}


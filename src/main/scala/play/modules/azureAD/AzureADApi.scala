package play.modules.azureAD

import javax.inject.Inject
import scala.collection.JavaConversions._
import akka.actor.ActorSystem

import play.api.inject.ApplicationLifecycle
import play.api.{ Configuration, Logger }
import play.modules.azureAD.api.{GraphApi, ClaimsApi}

/**
 * Created by alain on 10/08/15.
 */
trait AzureADApi {
  def apiConfiguration: AzureADConfiguration
  def claimsApi: ClaimsApi
  def graphApi: GraphApi
}

final class DefaultAzureADApi @Inject() (
  actorSystem: ActorSystem,
  configuration: Configuration,
  applicationLifecycle: ApplicationLifecycle) extends AzureADApi {

  lazy override val apiConfiguration = {
    Logger.info("AzureAD Module -- reading configuration")

    val apiCfg = DefaultAzureADApi.parseConf(configuration)
    if(apiCfg.tenants.size < 1) throw configuration.globalError("AzureAD module need at least one tenant!, check the configuration key 'azureAD.tenants'")

    Logger.info(s"AzureAD Module -- found ${apiCfg.tenants.size} tenant(s)")
    apiCfg
  }

  lazy override val claimsApi = new ClaimsApi(apiConfiguration)
  lazy override val graphApi = new GraphApi(apiConfiguration)

}

private[azureAD] object DefaultAzureADApi {

  def parseConf(configuration: Configuration): AzureADConfiguration = {

    val cfgTenants = configuration.getConfigList("azureAD.tenants").getOrElse(throw configuration.globalError("Missing configuration key 'azureAD.tenants'!"))
    val tenants = cfgTenants.map { tenant =>

      val tenantId = tenant.getString("id").getOrElse(throw configuration.globalError("Missing configuration key 'id' for one tenant!"))

      val clients = tenant.getConfigList("clients")
        .getOrElse(throw configuration.globalError("Missing configuration key 'clients' for one tenant!"))
        .map { client =>
          val clientId = client.getString("id").getOrElse(throw configuration.globalError(s"Missing configuration key 'id' for one client, in tenant ${tenantId}!"))
          val secretKey = client.getString("secretKey")
          (clientId, Client(clientId, secretKey))
        }.toMap

      (tenantId -> Tenant(tenantId, clients))

    }.toMap

    val openIdPubKeyLifetime = configuration.getInt("azureAD.openIdPubKeyLifetime").getOrElse(1800) // 30mns
    val groupMembershipLifeTime = configuration.getInt("azureAD.groupMembershipLifeTime").getOrElse(900) // 15mns

    AzureADConfiguration(tenants, openIdPubKeyLifetime, groupMembershipLifeTime)
  }

}

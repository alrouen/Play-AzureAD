package play.modules.azureAD

/**
 * Created by alain on 11/08/15.
 */
case class AzureADConfiguration (
  tenants: Map[String, Tenant],
  openIdPubKeyLifetime: Int,
  groupMembershipLifeTime: Int
)

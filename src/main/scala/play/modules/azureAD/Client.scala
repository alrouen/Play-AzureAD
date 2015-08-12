package play.modules.azureAD

/**
 * Created by alain on 10/08/15.
 */
case class Client(id: String, clientSecret: Option[String]) {
  lazy val graphApiPayload = Map(
    "client_id" -> Seq(id),
    "client_secret" -> Seq(clientSecret.getOrElse("")),
    "grant_type" -> Seq("client_credentials"),
    "resource" -> Seq(AzureADEndpoints.graphAPI)
  )
}



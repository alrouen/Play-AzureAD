package play.modules.azureAD

import play.api.libs.json.Json

/**
 * Created by alain on 10/08/15.
 */
case class Group(objectId: String, displayName: String)
object Group {
  implicit val groupFormat = Json.format[Group]
}

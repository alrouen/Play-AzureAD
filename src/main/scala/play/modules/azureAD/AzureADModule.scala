package play.modules.azureAD

import javax.inject._

import akka.actor.ActorSystem
import play.api.ApplicationLoader.Context
import play.api._
import play.api.inject.{ ApplicationLifecycle, Binding, Module }

/**
 * AzureAD module.
 */
@Singleton
final class AzureADModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(bind[AzureADApi].to[DefaultAzureADApi].in[Singleton])
}

/**
 * Cake pattern components.
 */
trait AzureADComponents {
  def azureADApi: AzureADApi
}
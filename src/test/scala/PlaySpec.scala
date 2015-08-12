import com.google.inject

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeApplication
import play.api.test.Helpers._

import play.modules.azureAD.{ DefaultAzureADApi, AzureADApi }

object PlaySpec extends org.specs2.mutable.Specification {

  "Play integration" title

  "AzureAD API" should {
    "not be resolved if the module is not enabled" in running(
      FakeApplication()) {
      val appBuilder = new GuiceApplicationBuilder().build

      appBuilder.injector.instanceOf[AzureADApi].
        aka("resolution") must throwA[inject.ConfigurationException]
    }

    //TODO: find a way to avoid configuration file conflict between tests
    /*"not to be resolved if there's any mistake in the configuration" in {
      System.setProperty("config.resource", "bad_configuration.conf")

      running(FakeApplication()) {
        configuredAppBuilder.injector.instanceOf[AzureADApi].
          aka("AzureAD API") must throwA[inject.ProvisionException]
      }
    }*/

    "be resolved if the module is enabled" in {
      System.setProperty("config.resource", "test.conf")

      running(FakeApplication()) {

        configuredAppBuilder.injector.instanceOf[AzureADApi].
          aka("AzureAD API") must beLike {
            case api: DefaultAzureADApi => ok
          }
      }
    }
  }

  def configuredAppBuilder = {
    import scala.collection.JavaConversions.iterableAsScalaIterable

    val env = play.api.Environment.simple(mode = play.api.Mode.Test)
    val config = play.api.Configuration.load(env)
    val modules = config.getStringList("play.modules.enabled").fold(
      List.empty[String])(l => iterableAsScalaIterable(l).toList)

    new GuiceApplicationBuilder().
      configure("play.modules.enabled" -> (modules :+
      "play.modules.azureAD.AzureADModule")).build
  }
}
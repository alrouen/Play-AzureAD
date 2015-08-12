# Azure Active Directory Support to Play! Framework 2.4

This is a module for Play 2.4, enabling support for [Azure Active Directory](https://azure.microsoft.com/en-us/documentation/articles/active-directory-whatis/) - Microsoft’s multi-tenant cloud based directory and identity management service.

## Installation

So far you have to build manually :

- clone from this source repository
- ./activator or ./sbt
- publish-local

> As for [Play Framework](http://playframework.com/) 2.4, a JDK 1.8+ is required to build this plugin.

## Usage

###1) In your play project, add the local dependency in build.sbt file :

    libraryDependencies ++= Seq(
     ...
     "org.freesp" %% "play-azuread" % "1.0",
     ...
    )

###2) Enable & configure the plugin (application.conf) :

    play.modules.enabled += "play.modules.azureAD.AzureADModule"

    azureAD.tenants = [
      {
        id:"tenant name or id",
        clients:[ { id:"application client id", secretKey:"api secret key" } ]
      }
    ]

    // cache duration in seconds for openID public key (for JWT session token validation)
    azureAD.openIdPubKeyLifetime = 300
    // group membership cache duration in seconds (cache entry = user principal name + group Id)
    azureAD.groupMembershipLifeTime = 30


###3) Protect your API with Azure Active Directory :

The goal of AzureADAction is to restrict access to any actions to requests that follow these requirements :

 - a request header Authorization, with as value : "Bearer xxxx" (xxxx is an Azure Active Directory JWT coming for example from single page app got that enforced user authentication against an AAD instance, as with [ADAL.js](https://github.com/AzureAD/azure-activedirectory-library-for-js))
 - a valid JWT : not expired, signature valid
 - the upn extracted from the JWT is a member of a specified security group

To use it, create a controller like that :

    ...
    import play.modules.azureAD.{AzureADComponents, AzureADApi, AzureADController}
    ...

    class Application @Inject() (val azureADApi: AzureADApi) extends AzureADController with AzureADComponents {

      // implicit reference
      implicit val tenant = "my api tenant id" // an ID already declared in the application conf
      implicit val client = "my api client id" // an ID already declared in the application conf
      implicit val appGroupId = "security group id any user must be member of the get access to the API"

      def myApiAction = AzureADAction().async { request =>
        Future.successful( Ok( Json.obj("user" -> request.username) ) )
      }

    }

###4) Use the AzureAD GraphAPI :

In any object :

    ...
    import play.modules.azureAD.{Client, Tenant, AzureADApi, Group}
    import play.api.Play.current
    ...

    object myAPIHelper {
        val azureADApi = current.injector.instanceOf[AzureADApi]

        def memberOf(upn: String)(implicit tenant: Tenant, client: Client): Future[List[Group]] = azureADApi.graphApi.memberOf(upn)(tenant, client)
    }


## What's next ?

- Complete the GraphAPI methods, a lot are missing
- simplify the configuration (tenant/clients....)
- test/allow multi-tenant scenarios
- ?


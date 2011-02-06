package fbgl1.http

/**
 * Garrick Evans
 * 29 Dec 2010
 */

import akka.http._
import akka.actor._
import akka.config.Supervision._

class Service extends Actor with Endpoint
{
  def hook(uri:String) = true
  def provide(uri:String) =
    {
      log.slf4j.info("URI {}",uri)
      if (uri contains ".txt") {
        val id = {
          val res = uri split '/' last

          res split '.' head
        }

        //
        // return the requested user actor
        //
        (Service.cache !! User(id, "")).get.asInstanceOf[ActorRef]
      }
      else {
        val actor = Actor.actorOf[PageActor].start
        self.link(actor)
        actor
      }
    }

  override def preStart =
  {
    Actor.registry.actorsFor(classOf[RootEndpoint]).head ! Endpoint.Attach(hook, provide)
    self.link(Service.cache)
  }

  def receive = handleHttpRequest
}

object Service
{
  val GraphAPI = "https://graph.facebook.com"
  val MapAPI = "http://maps.googleapis.com/maps/api/geocode/json?address="
  val Server = "http://localhost:9998"
  val Token = "access_token"
  val User = "user_id"
  val Friends = "/friends"
  val Picture = "/picture"

  val FBClientID = ""
  val FBClientSecret = ""

  val IndexHTML = io.Source.fromFile("root/index.html").mkString

  val cache = Actor.actorOf[CacheActor].start
}


class PageActor extends Actor
{
  import net.smartam.leeloo.client._
  import net.smartam.leeloo.client.request.OAuthClientRequest
  import net.smartam.leeloo.client.response. {OAuthAuthzResponse, GitHubTokenResponse}
  import net.smartam.leeloo.common.message.types.GrantType


	self.lifeCycle = Permanent;

  def default(a:Any) = ""

  def receive =
  {
    case get:Get => {

      def handle(user:String) =
      {
        val oauthreq = OAuthClientRequest
                        .authorizationLocation(Service.GraphAPI+"/oauth/authorize")
                        .setClientId(Service.FBClientID)
                        .setRedirectURI(Service.Server+"/auth.html?"+Service.User+"="+user)
                        .setScope("user_location,user_hometown,friends_location,friends_hometown")
                        .buildQueryMessage

        get.response.sendRedirect(oauthreq.getLocationUri)
      }

      get.request.getRequestURI match {

          //
          // index page - requires access token
          //
        case page if page startsWith "/index.html" => {

          (get getParameterOrElse(Service.User, default),
           get getParameterOrElse(Service.Token, default)) match {

            case ("", _) => get BadRequest "Missing user"
            case (user, "") => handle(user)
            case (user, _) => get OK Service.IndexHTML
          }
        }

        case page if page startsWith "/auth.html" => {

          val user = get getParameterOrElse(Service.User, default)
          val code = OAuthAuthzResponse.oauthCodeAuthzResponse(get.request).getCode
          //log.slf4j.info("Got auth code: {}", code)

          val oauthreq = OAuthClientRequest
                          .tokenLocation(Service.GraphAPI+"/oauth/"+Service.Token)
                          .setGrantType(GrantType.AUTHORIZATION_CODE)
                          .setClientId(Service.FBClientID)
                          .setClientSecret(Service.FBClientSecret)
                          .setRedirectURI(Service.Server+"/auth.html?"+Service.User+"="+user)
                          .setCode(code)
                          .buildBodyMessage

          //create OAuth client that uses custom http client under the hood
          val client = new OAuthClient(new URLConnectionClient)

          //Facebook is not fully compatible with OAuth 2.0 draft 10, access token response is
          //application/x-www-form-urlencoded, not json encoded so we use dedicated response class for that
          //Custom response classes are an easy way to deal with oauth providers that introduce modifications to
          //OAuth 2.0 specification
          val oatuhresp = client.accessToken(oauthreq, classOf[GitHubTokenResponse])

          val token = oatuhresp getAccessToken
          //val expiry = oatuhresp getExpiresIn

          //log.slf4j.info("Got token {} expiry {}", token, expiry)

          get.response sendRedirect(Service.Server+"/index.html?"+Service.User+"="+user+"&"+Service.Token+"="+token)

            //
            // pass token to user actor
            //
            Service.cache ! User(user, token)
        }

        case resource => Service.cache ! Resource(get, resource)

      }
    }
  }
}




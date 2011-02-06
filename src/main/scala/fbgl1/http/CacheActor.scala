package fbgl1.http

import akka.http._
import akka.actor._
import akka.config.Supervision._


case class User(id:String, token:String)
case class Resource(get:Get, name:String)


class CacheActor extends Actor
{
  import collection.mutable.Map

  self.lifeCycle = Permanent
  
  val resources = Map.empty[String, String]
  val users = Map.empty[String, ActorRef]

  def make(id:String) = {
    val ref = Actor.actorOf{new UserActor(id)}.start
    users += (id -> ref)
    self.link(ref)
    ref
  }

  def receive =
  {
    case Resource(get, name) if name contains ".js" => {

      def slurp = {
        val payload = scala.io.Source.fromFile("root/"+name).mkString
        resources += (name -> payload)
        payload
      }

      get OK resources.getOrElse(name, slurp)
    }

    case User(id, token) => token match {
      case "" => self.reply_? (users.getOrElse(id, make(id)))
      case _ => users.getOrElse(id, make(id)) ! Token(token)
    }

    case _ => {}
  }
}
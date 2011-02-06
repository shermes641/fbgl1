package fbgl1

import http.Service
import akka.actor.Actor._
import akka.actor.SupervisorFactory
import akka.config._
import akka.config.Supervision._
import akka.http.RootEndpoint


/**
 * Created by IntelliJ IDEA.
 * User: evansg
 * Date: 12/27/10
 * Time: 12:20 
 * To change this template use File | Settings | File Templates.
 */

class Boot
{
  val factory = SupervisorFactory(
    SupervisorConfig(
        //
        // OneForOneStrategy - restart strategy indicating if 1 child goes down it alone will be restarted
        // 3 - number of retry attempts
        // 1000 - time (in ms) window to complete restart
        //
      OneForOneStrategy(List(classOf[Exception]), 3, 1000),
      Supervise(
        actorOf[RootEndpoint],
        Permanent) ::
      Supervise(
        actorOf[Service],
        Permanent) ::
      Nil))

val supervisor = factory.newInstance
supervisor.start

}
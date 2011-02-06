package fbgl1.http

import akka.http._
import akka.actor._

case class Token(token:String)
case class Subnet(get:Get)

/**
 * represents a session user 
 */
class UserActor(id:String) extends Actor
{
  import collection.mutable.HashMap
  import java.net.URLEncoder
  import org.apache.http.client.methods.HttpGet
  import org.apache.http.impl.client. {DefaultHttpClient, BasicResponseHandler}
  import org.codehaus.jackson._
  import akka.routing._
  import akka.routing.Routing._

  self.id = id

  var token = ""
  var location = Location("", "")
  var locations = HashMap.empty[String, ActorRef]
  var fcount = 0
  var flocated = 0
  
  val vcount = 90;
  val verts = new Array[Double](vcount*3)
  
  def addLocation(loc:String) = 
  {
    val actor = Actor.actorOf{new LocationActor(id, loc)}.start  
    locations += (loc -> actor)
    //if (!location.id.isEmpty) computeDrawables(actor)
    actor
  }
  
  	//
  	// compute the polyline for the arc
	// not safe - invoke from receive only
	//
  def computeDrawables(lactor:ActorRef) =
  {
  	 import scala.math._
  	 
  	 val llrad = 0.017453277
  	 
  		//
  		// converts theta/phi (lat/long) to model-space cartesian
  		//
    def TPtoXYZ(theta:Double, phi:Double) = (-1*cos(theta)*cos(phi), sin(theta), cos(theta)*sin(phi))

	 def dot(u:Tuple3[Double, Double, Double], v:Tuple3[Double, Double, Double]) = u._1*v._1 + u._2*v._2 + u._3*v._3
	 def norm(x:Tuple3[Double, Double, Double]) = sqrt(x._1*x._1 + x._2*x._2 + x._3*x._3)

	 try {
			//
			// compute the angle (at the center of the earth) between us and the location provided
			//
		 var p = (0.0,0.0,0.0)
		 var q = (0.0,0.0,0.0) 	
		 val angle = {
 	 	 val (theta, phi) = {
		 	val l = (lactor !! Locate("", "")).asInstanceOf[Option[Location]].get
			(l.id.toDouble*llrad, l.name.toDouble*llrad)
	 	 }

		 p = TPtoXYZ(location.id.toDouble*llrad, location.name.toDouble*llrad) 	
	 	 q = TPtoXYZ(theta, phi)

	 	 acos(dot(p, q)/(norm(p) * norm(q)))
	  }

	  for (s <- 0 until vcount) {
	 	val t:Double = s/(vcount-1.0);
	 	val a:Double = sin((1.0-t)*angle);
	 	val b:Double = sin(t*angle);
	 	val d:Double = sin(angle);
	 	
	 	verts(s*3) = (a*p._1 + b*q._1)/d
	 	verts(1+(s*3)) = (a*p._2 + b*q._2)/d
	 	verts(2+(s*3)) = (a*p._3 + b*q._3)/d
	  }
	  
	  this.log.slf4j.info("computed")
	} catch {
		case _ => {}
	}
  }
  
  def receive =
  {
        //
        // do a bunch of processing since we're on a non-request thread
        //
    case Token(t) => {

        //
        // update our access token
        //
      token = t
      
        //
        // obtain our location name
        //
      val city = (Actor.actorOf[Locator].start !! Locate(id, token)).get.asInstanceOf[Location].name
      
        //
        // start a location actor for our location, if needed
        //
      locations.getOrElse(city, addLocation(city))
      
        //
        // cache our lat/long info
        //
      location = (locations.get(city).get !! Locate("", "")).get.asInstanceOf[Location]

      //
      // look up friends
      //
      val client = new DefaultHttpClient
      val encoded = URLEncoder.encode(token, "UTF-8")
      val uri = Service.GraphAPI+"/"+id+Service.Friends+"?"+Service.Token+"="+encoded
      val req = new HttpGet(uri)
      val handler = new BasicResponseHandler
      val body = client.execute(req, handler)
      client.getConnectionManager.shutdown

        //
        // reset
        //
      fcount = 0
      flocated = 0

      //
      // using jackson here to parse of json - appears to run much faster than dispatch
      // though this isn't fully confirmed yet
      //
      val f = new JsonFactory
      val jp = f.createJsonParser(body getBytes)
      var current = jp.nextToken
      
      val router = loadBalancerActor(new SmallestMailboxFirstIterator(List(Actor.actorOf[Locator].start, Actor.actorOf[Locator].start)));

        //
        // process the json linearly and for each friend id 
        // spin up a locator actor whose job it is to issue another
        // query to find the friend's city. this will create an actor per
        // friend which currently all use the global dispatcher so the requests will
        // be spread across the default thread pool 
        // (need to check if ~16 simultaneous requests from the same IP get blocked by maps...)
        //
      while (jp.nextToken() != JsonToken.END_OBJECT) {

        var fieldName = jp.getCurrentName
        current = jp.nextToken
        if (fieldName == "data") {
          if (current == JsonToken.START_ARRAY) {
            while (jp.nextToken != JsonToken.END_ARRAY) {
              while (jp.nextToken() != JsonToken.END_OBJECT) {
                fieldName = jp.getCurrentName
                jp.nextToken
                if (fieldName == "id") {
                  val fid = jp.getText
                  
                  //
                  // send msg to locate friend, reply will come back later
                  // 
                  router ! Locate(fid, token)
                  log.slf4j.info("friend {}",fid)
                  //
                  // track total number of friends
                  //
                  fcount += 1
                }
              }
            }
          } else {
            jp.skipChildren
          }
        } else {
          jp.skipChildren
        }
      }
      jp.close // ensure resources get cleaned up timely and properly
      
      log.slf4j.info("{} friends processed; locating...", fcount)
    }

        //
        // we get this back from the location actor once a id is located
        //
    case Location(fid, loc) =>
    {
	        //
   	     // track friends found
      	  //
        flocated += 1;
        if (flocated == fcount) {
          log.slf4j.info("friends locating done; found {}", flocated)
        }
        
        if (loc != "" && loc != "null") {
            //
            // finds or starts new actor for the location
            // then adds the friend
            //
            locations.getOrElse(loc, addLocation(loc)) ! Friend(fid)
        }
   }

        //
        // handle the REST call to gather friend info
        //
    case get:Get =>
    {
        get getParameterOrElse("what", (Any)=>"") match {
            
                //
                // return the user's lat/long delimited by ","
                //
            case "loc" => get OK location.id +","+location.name     // reusing the msg class so the fields look funny...
            
            case "friends" => {
                get getParameterOrElse("loc", (Any)=>"") match {
                        //
                        // return friend locs delimited by "&"
                        //
                    case "" => {
                        var payload = ""
                        locations foreach {payload += _._1 +"&"}
								if (flocated == fcount) {
									payload += "ready&"
								}
                        get OK payload.dropRight(1)
                    }
                        //
                        // return friends for location delimited by ","
                        //
                    case location => {
                    		locations.get(location.replace(" ","+")) match {
                    			case Some(l) => l ! Friends(get)
                    			case None => get OK "" 
                    		}
                    	}
                }
            }
            
            /*
            case "subnet" => {
            	log.slf4j.info("subnet");

					get getParameterOrElse(Service.Token, (Any)=>"") match {
						case "" => get BadRequest "Missing ID"
						case fid => {
							val friend = Actor.actorOf(new UserActor(fid)).start
							friend ! Token(token)
							friend ! Subnet(get)
						}
					}            	
            }
            */
            
            case _ => get BadRequest "Bad or missing parameter"
        }
    }
    
    /*
    case Subnet(get) => 
    {
  	 		var payload = ""
         verts foreach {payload += _ + ","}
         log.slf4j.info("count: "+verts.length+" payload: "+payload);
         get OK payload.dropRight(1)
	 }
	 */

    case other:RequestMethod => other NotAllowed "Unsupported request"
  }
}
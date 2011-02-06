package fbgl1.http

import akka.actor._
import akka.http._

case class Friend(id:String)
case class Friends(get:Get)

/**
 * represents a single location and list of friends for a user
 */
class LocationActor(user:String, name:String) extends Actor
{
    import org.apache.http.client.methods.HttpGet
    import org.apache.http.impl.client. {DefaultHttpClient, BasicResponseHandler}
    import sjson.json._
    import dispatch.json.Js._


    var lat = ""
    var long = ""
    var friends = List[String]()
    
    /**
     * Find our lat/long using Google Map API
     */
    override def preStart =
    {
      var attempts = 1
      
      while (attempts>0) {
	    	try {
			   val client = new DefaultHttpClient
        		var uri = Service.MapAPI+name+"&sensor=false"
        		var req = new HttpGet(uri)
        		val handler = new BasicResponseHandler
        		var body = client.execute(req, handler)
     			client.getConnectionManager.shutdown

        			//
        			// brute force our way thru the json then use sjson & dispatch extractors
        			//
	    		val n = body.indexOf("{",body.indexOf("location",body.indexOf("geometry")))
  	    		val x = 1+ body.indexOf("}",body.indexOf("}",n))
     			val in = Serializer.SJSON.in(body.substring(n,x))
	    		val latx = 'lat ? num
  	    		val latx(location_lat) = in
     			val longx = 'lng ? num
       		val longx(location_long) = in

        		lat = location_lat.toString
        		long = location_long.toString

				if (lat.isEmpty || long.isEmpty) {
					attempts -= 1
		      	log.slf4j.warn("bad data while starting location actor for {}", name+" for user "+user)
				} else {
	    			log.slf4j.info("started location actor for {}", name+" for user "+user)
	    			attempts = 0;
	    		}
	    	}
	    	catch {
	    		case _ => {
	    			attempts -= 1
		      	log.slf4j.warn("bad data while starting location actor for {}", name+" for user "+user)
	    		}
	    	}
	    	if (attempts > 0) Thread.sleep(1000*attempts)
	 	}
    }
    
    def receive = 
    {
            //
            // add friend
            //
        case Friend(fid) => if (!friends.contains(fid)) friends = fid :: friends
        
            //
            // return lat/long (reuse this msg)
            //
        case Locate("", "") => self reply_?(Location(lat, long))
        
            //
            // return the ids and complete the request
            //
        case Friends(get) => {
            var payload = lat+"&" + long+"&"
            friends foreach {payload += _ +","}
				log.slf4j.info("location friends {}", payload)
            get OK payload.dropRight(1)
        }
    }
}
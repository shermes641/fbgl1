package fbgl1.http

import akka.actor._


case class Locate(user:String, token:String)
case class Location(id:String, name:String)
//case class Location(user:String, lat:String, long:String)

class Locator extends Actor
{
  import java.net.URLEncoder
  import org.apache.http.client.methods.HttpGet
  import org.apache.http.impl.client. {DefaultHttpClient, BasicResponseHandler}
  import org.codehaus.jackson._


  def receive =
  {
    case Locate(user, token) => {

   	   var loc = ""
   	   
	      //
	      // obtain the user's location or hometown from fb
	      //
	      val client = new DefaultHttpClient
	      val encoded = URLEncoder.encode(token, "UTF-8")
	      var uri = Service.GraphAPI+"/"+user+"?"+Service.Token+"="+encoded
	      var req = new HttpGet(uri)
	      val handler = new BasicResponseHandler
	      var body = client.execute(req, handler)
      	client.getConnectionManager.shutdown

	      //
   	   // parse and obtain lat/long from locator
      	//
	      val f = new JsonFactory
   	   val jp = f.createJsonParser(body getBytes)
      	var current = jp.nextToken
	      var skip = false


          while (jp.nextToken() != JsonToken.END_OBJECT) {
            var fieldName = jp.getCurrentName
            current = jp.nextToken
            if (fieldName == "location") {
              if (current == JsonToken.START_OBJECT) {
                while (jp.nextToken != JsonToken.END_OBJECT) {
                    fieldName = jp.getCurrentName
                    jp.nextToken
                    if (fieldName == "name") {
                      loc = jp.getText
                      skip = true
                    }
                }
              } else {
                jp.skipChildren
              }
            }
            else if (!skip && fieldName == "hometown") {
              if (current == JsonToken.START_OBJECT) {
                // For each of the records in the array
                while (jp.nextToken != JsonToken.END_OBJECT) {
                    fieldName = jp.getCurrentName
                    jp.nextToken
                    if (fieldName == "name") {
                      loc = jp.getText
                    }
                }
              } else {
                jp.skipChildren
              }
            } else {
              jp.skipChildren
            }
          }
          jp.close
      
      loc = loc.replace(" ", "+")
      loc = loc.replace(",", "+")
      self reply_?(Location(user, loc))
    }

    case _ => {}
  }
}


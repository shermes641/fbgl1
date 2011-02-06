function get(url) {
  try {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, false);
    xhr.send();
    return xhr.responseText;
  } catch (e) {
    return ''; // turn all errors into empty results
  }
}

onmessage = function (event) {
   var data = event.data.split(',')
   var uri = "/data/"+data[2]+'.txt?access_token='+data[3]+"&what="+data[0];
	if (data[1] != "") {
		uri += "&loc="+data[1];
	}
   var res = get(uri);
	postMessage(data[0]+":"+data[1]+":"+res);
};
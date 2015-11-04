function initMap() {
  

  var map = new google.maps.Map(document.getElementById('map'), {
	center: {lat: 20, lng: 150.644},
   	zoom : 2
  });

  airports = [
              ["Chek Lap Kok International Airport",22.3089008331,113.915000916,"HKG"],
              ["Los Angeles International Airport",33.94250107,-118.4079971,"LAX"]
             ]
  
  for (i = 0; i < airports.length; i++) {
	  var airportInfo = airports[i]
	  var position = {lat: airportInfo[1], lng: airportInfo[2]};
	  var marker = new google.maps.Marker({
		    position: position,
		    map: map,
		    title: airportInfo[0],
	  		airportCode: airportInfo[3]
		  });
	  
	  function clickFunction(code) {
		  alert(code)
	  }
	  
	  marker.addListener('click', function() { 
		  alert(this.airportCode) 
		  });
	  //var f = new clickFunction(airportInfo[3])
  }
}


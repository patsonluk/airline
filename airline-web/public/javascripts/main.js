var map
var flightPaths
var activeInput

$( document ).ready(function() {
	flightPaths = []
	activeInput = $("#fromAirport")
	loadAirlines()
})

function initMap() {
  map = new google.maps.Map(document.getElementById('map'), {
	center: {lat: 20, lng: 150.644},
   	zoom : 2
  });
  getAirports()
}

function addMarkers(airports) {
	for (i = 0; i < airports.length; i++) {
		  var airportInfo = airports[i]
		  var position = {lat: airportInfo.latitude, lng: airportInfo.longitude};
		  var marker = new google.maps.Marker({
			    position: position,
			    map: map,
			    title: airportInfo.name,
			    airportName: airportInfo.name,
		  		airportCode: airportInfo.iata,
		  		airportId: airportInfo.id
			  });
		  
		  marker.addListener('click', function() {
			  var airportId = this.airportId
			  if (activeInput.is($("#fromAirport"))) {
				  $("#fromAirport").val(airportId)
				  $("#fromAirportName").text(this.airportName)
				  activeInput = $("#toAirport")
			  } else {
				  $("#toAirport").val(airportId)
				  $("#toAirportName").text(this.airportName)
				  activeInput = $("#fromAirport")
			  }
		  });
	}
}



function updateAllPanels(airlineId) {
	setWebSocketAirlineId(airlineId)
	updateBalance(airlineId)
	updateAirplaneList(airlineId, $("#airplaneList"))
	updateLinksInfo()
}

function updateBalance(airlineId) {
	$.ajax({
		type: 'GET',
		url: "http://localhost:9001/airlines/" + airlineId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airline) {
	    	$("#balance").text(airline.balance)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function appendConsole(message) {
	$('#console').append( message + '<br/>')
}

function getAirports() {
	$.getJSON( "http://localhost:9001/airports?count=100", function( data ) {
		  addMarkers(data)
		});
}
	
	
	
	
	
	
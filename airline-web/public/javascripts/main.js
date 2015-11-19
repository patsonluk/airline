var map
var activeInput
var markers = []

$( document ).ready(function() {
	flightPaths = []
	activeInput = $("#fromAirport")
	loadAirlines()
})

function initMap() {
  map = new google.maps.Map(document.getElementById('map'), {
	center: {lat: 20, lng: 150.644},
   	zoom : 2,
   	minZoom : 2
  });
  google.maps.event.addListener(map, 'zoom_changed', function() {
	    var zoom = map.getZoom();
	    // iterate over markers and call setVisible
	    for (i = 0; i < markers.length; i++) {
	        markers[i].setVisible(isShowMarker(markers[i], zoom));
	    }
  });  
  getAirports()
  
}

function addMarkers(airports) {
	var infoWindow = new google.maps.InfoWindow({
		maxWidth : 400
	})
	currentZoom = map.getZoom()
	for (i = 0; i < airports.length; i++) {
		  var airportInfo = airports[i]
		  var position = {lat: airportInfo.latitude, lng: airportInfo.longitude};
		  var marker = new google.maps.Marker({
			    position: position,
			    map: map,
			    title: airportInfo.name,
			    airportName: airportInfo.name,
		  		airportCode: airportInfo.iata,
		  		airportId: airportInfo.id,
		  		airportSize: airportInfo.size,
		  		airportPopulation: airportInfo.population,
		  		airportIncomeLevel: airportInfo.incomeLevel,
		  		airportCountryCode: airportInfo.countryCode
			  });
		  
		  marker.addListener('click', function() {
			  infoWindow.close();
			  $("#airportPopupName").text(this.airportName)
			  $("#airportPopupIata").text(this.airportCode)
			  $("#airportPopupSize").text(this.airportSize)
			  $("#airportPopupPopulation").text(this.airportPopulation)
			  $("#airportPopupIncomeLevel").text(this.airportIncomeLevel)
			  $("#airportPopupCountryCode").text(this.airportCountryCode)
			  updatePopupAppeal(this.airportId)
			  $("#airportPopupId").val(this.airportId)
			  infoWindow.setContent($("#airportPopup").html())
			  infoWindow.open(map, this);
		  });
		  marker.setVisible(isShowMarker(marker, currentZoom))
		  markers.push(marker)
	}
}
function isShowMarker(marker, zoom) {
	return (zoom + marker.airportSize > 7) //init zoom at 2, so show all that is size > 5
}


function updatePopupAppeal(airportId) {
	//clear the old ones
	$("#airportPopupAwareness").text()
	$("#airportPopupLoyalty").text()
	var airlineId = $("#airlineOption").val()
	$.ajax({
		type: 'GET',
		url: "http://localhost:9001/airports/id/" + airportId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airport) {
	    	var hasMatch = false
	    	$.each(airport.appealList, function( key, appeal ) {
	    		if (appeal.airlineId == airlineId) {
	    			$("#airportPopupAwareness").text(appeal.awareness)
	    			$("#airportPopupLoyalty").text(appeal.loyalty)
	    			hasMatch = true
	    		}
	  		});
	    	if (!hasMatch) {
	    		$("#airportPopupAwareness").text("0")
	    		$("#airportPopupLoyalty").text("0")
	    	}
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}



function updateAllPanels(airlineId) {
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
	$.getJSON( "http://localhost:9001/airports?count=300", function( data ) {
		  addMarkers(data)
		});
}
	
	
	
	
	
	
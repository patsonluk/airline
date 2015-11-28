var map
var markers = []
var activeAirline

$( document ).ready(function() {
	login()
	flightPaths = []
	activeInput = $("#fromAirport")
	loadAirlines()
})

function login()  {
	$.ajax
	({
	  type: "GET",
	  url: "login",
	  async: false,
	  username: "patson",
	  password: "1234",
	  data: '{ "comment" }',
	  success: function(message) {
	    	console.log(message)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

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
		maxWidth : 500
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
		  		airportCity: airportInfo.city,
		  		airportId: airportInfo.id,
		  		airportSize: airportInfo.size,
		  		airportPopulation: airportInfo.population,
		  		airportIncomeLevel: airportInfo.incomeLevel,
		  		airportCountryCode: airportInfo.countryCode,
		  		airportSlots : airportInfo.slots,
		  		airportAvailableSlots: airportInfo.availableSlots
			  });
		  
		  marker.addListener('click', function() {
			  infoWindow.close();
			  
			  var isBase = updateBaseInfo(this.airportId)
			  $("#airportPopupName").text(this.airportName)
			  $("#airportPopupIata").text(this.airportCode)
			  $("#airportPopupCity").text(this.airportCity)
			  $("#airportPopupSize").text(this.airportSize)
			  $("#airportPopupPopulation").text(this.airportPopulation)
			  $("#airportPopupIncomeLevel").text(this.airportIncomeLevel)
			  $("#airportPopupCountryCode").text(this.airportCountryCode)
			  $("#airportPopupSlots").text(this.airportAvailableSlots + " (" + this.airportSlots + ")")
			  updatePopupAppeal(this.airportId)
			  updatePopupSlots(this.airportId)
			  $("#airportPopupId").val(this.airportId)
			  infoWindow.setContent($("#airportPopup").html())
			  infoWindow.open(map, this);
			  
			  if (isBase) {
				  $("#planFromAirportButton").show()
				  $("#planFromAirportButton").click(function() {
					  planFromAirport($('#airportPopupId').val(), $('#airportPopupName').text())
					  infoWindow.close();
				  });
			  } else {
				  $("#planFromAirportButton").hide()
			  }
			  $("#planToAirportButton").click(function() {
				  planToAirport($('#airportPopupId').val(), $('#airportPopupName').text())
				  infoWindow.close();
			  });
		  });
		  marker.setVisible(isShowMarker(marker, currentZoom))
		  markers.push(marker)
	}
}
function isShowMarker(marker, zoom) {
	return (zoom + marker.airportSize > 7) //init zoom at 2, so show all that is size > 5
}
function updateBaseInfo(airportId) {
	$("#buildHeadquarterButton").hide()
	$("#buildBaseButton").hide()
	$("#airportIcons img").hide()
	if (!activeAirline.headquarterAirport) {
	  $("#buildHeadquarterButton").show()
	} else {
	  var baseAirport
	  for (i = 0; i < activeAirline.baseAirports.length; i++) {
		  if (activeAirline.baseAirports[i].airportId == airportId) {
			  baseAirport = activeAirline.baseAirports[i]
			  break
		  }
	  }
	  if (!baseAirport) {
	  	$("#buildBaseButton").show()
	  } else if (activeAirline.headquarterAirport.id == baseAirport.id){ //a HQ
		$("#popupHeadquarterIcon").show() 
	  } else { //a base
		$("#popupBaseIcon").show()
	  }
	}
	return baseAirport
}

function updatePopupAppeal(airportId) {
	//clear the old ones
	$("#airportPopupAwareness").text()
	$("#airportPopupLoyalty").text()
	var airlineId = activeAirline.id
	$.ajax({
		type: 'GET',
		url: "airports/" + airportId,
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
	    	
//	    	hasMatch = false
//	    	$.each(airport.slotAssignmentList, function( key, slotInfo ) {
//	    		if (slotInfo.airlineId == airlineId) {
//	    			$("#airportPopupAssignedSlots").text(slotInfo.slotAssignment)
//	    			hasMatch = true
//	    		}
//	  		});
//	    	if (!hasMatch) {
//	    		$("#airportPopupAssignedSlots").text("0")
//	    	}
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updatePopupSlots(airportId) {
	//clear the old ones
	$("#airportPopupAssignedSlots").text()
	var airlineId = activeAirline.id
	$.ajax({
		type: 'GET',
		url: "airports/" + airportId + "/slots?airlineId=" + airlineId,
	    dataType: 'json',
	    success: function(slotInfo) {
    		$("#airportPopupAssignedSlots").text(slotInfo.assignedSlots + " (" + slotInfo.maxSlots + ")")
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}




function updateAllPanels(airlineId) {
	updateAirlineInfo(airlineId)
}

function updateAirlineInfo(airlineId) {
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airline) {
	    	$("#balance").text(airline.balance)
	    	activeAirline = airline
	    	updateAirplaneList($("#airplaneList"))
	    	updateLinksInfo()
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function setActiveDiv(activeDiv) {
//	activeDiv.siblings().hide(500)
//activeDiv.show(500)
	if (activeDiv.siblings(":visible").length){
		activeDiv.siblings(":visible").fadeOut(200, function() { activeDiv.fadeIn(200) })
	} else {
		activeDiv.fadeIn(200)
	}
}


function appendConsole(message) {
	$('#console').append( message + '<br/>')
}

function getAirports() {
	$.getJSON( "airports?count=600", function( data ) {
		  addMarkers(data)
		});
}
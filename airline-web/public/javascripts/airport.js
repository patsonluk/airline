function showAirportDetails(airportId) {
	$.ajax({
		type: 'GET',
		url: "airports/" + airportId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airport) {
	    	if (airport) {
	    		populateAirportDetails(airport)
	    		setActiveDiv($("#airportCanvas"))
	    		$("#floatBackButton").show()
	    		shimmeringDiv($("#floatBackButton"))
	    		
	    	}
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function populateAirportDetails(airport) {
	 
	var airportMap = new google.maps.Map(document.getElementById('airportMap'), {
		//center: {lat: airport.latitude, lng: airport.longitude},
	   	zoom : 7,
	   	minZoom : 6
//	   	scrollwheel: false,
//	    navigationControl: false,
//	    mapTypeControl: false,
//	    scaleControl: false,
//	    draggable: false
	  });
	if (airport) {
		addCityMarkers(airportMap, airport)
		google.maps.event.addListenerOnce(airportMap, 'idle', function() {
		    google.maps.event.trigger(airportMap, 'resize');
		    airportMap.setCenter({lat: airport.latitude, lng: airport.longitude}); 
		});
		
		var airportMarkerIcon = $("#airportMap").data("airportMarker")
		new google.maps.Marker({
		    position: {lat: airport.latitude, lng: airport.longitude},
		    map: airportMap,
		    title: airport.name,
		    icon : airportMarkerIcon,
		    zIndex : 999
		  });
		
		new google.maps.Circle({
		        center: {lat: airport.latitude, lng: airport.longitude},
		        radius: airport.radius * 1000, //in meter
		        strokeColor: "#32CF47",
		        strokeOpacity: 0.2,
		        strokeWeight: 2,
		        fillColor: "#32CF47",
		        fillOpacity: 0.3,
		        map: airportMap
		    });
		loadAirportStatistics(airport)
	}
}

function loadAirportStatistics(airport) {
	$.ajax({
		type: 'GET',
		url: "airports/" + airport.id + "/link-statistics",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
//	    async: false,
	    success: function(airportStatistics) {
	    	plotPie(airportStatistics.departure, null , $("#departurePie"), "airportName", "passengers")
	    	plotPie(airportStatistics.destination, null, $("#destinationPie"), "airportName", "passengers")
	    	plotPie(airportStatistics.connectionFrom, null, $("#connectionFromPie"), "airportName", "passengers")
	    	plotPie(airportStatistics.connectionTo, null, $("#connectionToPie"), "airportName", "passengers")
	    	plotPie(airportStatistics.airlineDeparture, activeAirline.name, $("#airlineDeparturePie"), "airlineName", "passengers")
	    	plotPie(airportStatistics.airlineArrival, activeAirline.name, $("#airlineArrivalPie"), "airlineName", "passengers")
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function getAirports() {
	$.getJSON( "airports?count=2000", function( data ) {
		  addMarkers(data)
	});
}

function addMarkers(airports) {
	var infoWindow = new google.maps.InfoWindow({
		maxWidth : 500
	})
	currentZoom = map.getZoom()
	var largeAirportMarkerIcon = $("#map").data("largeAirportMarker")
	var mediumAirportMarkerIcon = $("#map").data("mediumAirportMarker")
	var smallAirportMarkerIcon = $("#map").data("smallAirportMarker")
	
	var resultMarkers = {}
	for (i = 0; i < airports.length; i++) {
		  var airportInfo = airports[i]
		  var position = {lat: airportInfo.latitude, lng: airportInfo.longitude};
		  var icon
		  if (airportInfo.size <= 3) {
			  icon = smallAirportMarkerIcon
		  } else if (airportInfo.size <= 6) {
			  icon = mediumAirportMarkerIcon
		  } else {
			  icon = largeAirportMarkerIcon
		  }
		  
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
		  		airportZone : airportInfo.zone,
		  		airportAvailableSlots: airportInfo.availableSlots,
		  		icon: icon
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
			  $("#airportPopupCountryCode").append("<img src='assets/images/flags/" + this.airportCountryCode + ".png' />")
			  updatePopupDetails(this.airportId)
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
		  resultMarkers[airportInfo.id] = marker
	}
	//now assign it to markers to indicate that it's ready
	markers = resultMarkers
}


function addCityMarkers(airportMap, airport) {
	var cities = airport.citiesServed
	var infoWindow = new google.maps.InfoWindow({
		maxWidth : 500
	})
	var cityMarkerIcon = $("#airportMap").data("cityMarker")
	var townMarkerIcon = $("#airportMap").data("townMarker")
	var villageMarkerIcon = $("#airportMap").data("villageMarker")
	
	$.each(cities, function( key, city ) {
		var icon
		if (city.population >= 500000) {
			icon = cityMarkerIcon
		} else if (city.population >= 100000) {
			icon = townMarkerIcon
		} else {
			icon = villageMarkerIcon
		}
		var position = {lat: city.latitude, lng: city.longitude};
		  var marker = new google.maps.Marker({
			    position: position,
			    map: airportMap,
			    title: city.name,
			    cityInfo : city,
			    icon : icon
			  });
		  
		  marker.addListener('click', function() {
			  infoWindow.close();
			  var city = this.cityInfo
			  $("#cityPopupName").text(city.name)
			  $("#cityPopupPopulation").text(city.population)
			  $("#cityPopupIncomeLevel").text(city.incomeLevel)
			  $("#cityPopupCountryCode").text(city.countryCode)
			  $("#cityPopupCountryCode").append("<img src='assets/images/flags/" + city.countryCode + ".png' />")
			  $("#cityPopupId").val(city.id)
			   
			  
			  
///////////////
				 ///////////////////////////!!!!!!!!!!
			$.ajax({
				type: 'GET',
				url: "cities/" + city.id + "/airportShares",
			    contentType: 'application/json; charset=utf-8',
			    dataType: 'json',
//			    async: false,
			    success: function(airportShares) {
			    	plotAirportShares(airportShares, airport.id, $("#cityPie"))
			    },
			    error: function(jqXHR, textStatus, errorThrown) {
			            console.log(JSON.stringify(jqXHR));
			            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
			    }
			});
			
			infoWindow.setContent($("#cityPopup").html())			
			infoWindow.open(airportMap, this);
			  
			  
			/////////////////////!!!!!!!!!!!!!!!
		 ///////////////
		  });
		  //marker.setVisible()
	});
}



function isShowMarker(marker, zoom) {
	return (marker.isBase) || ((zoom >= 4) && (zoom + marker.airportSize / 2 >= 7.5)) //start showing size >= 7 at zoom 4 
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
	  if (baseAirport){ //a base
		  if (baseAirport.headquarter){ //a HQ
			$("#popupHeadquarterIcon").show() 
		  } else { 
			$("#popupBaseIcon").show()
		  }
		}
	}
	return baseAirport
}

function updatePopupDetails(airportId) {
	//clear the old ones
	$("#airportPopupAwareness").text()
	$("#airportPopupLoyalty").text()
	$("#airportPopupSlots").text(this.airportAvailableSlots + " (" + this.airportSlots + ")")
			  
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
	    	
	    	$("#airportPopupSlots").text(airport.availableSlots + " (" + airport.slots + ")")
	    	
	    	$.each(airport.linkCounts, function(withLinksAirlineId, linkCount) {
	    		if (airlineId == withLinksAirlineId) {
	    			updateBuildBaseButton(airport.zone)
	    		}
	  		});
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateBuildBaseButton(airportZone) { //check if the zone already has base
	for (i = 0; i < activeAirline.baseAirports.length; i++) {
	  if (activeAirline.baseAirports[i].airportZone == airportZone) {
		  return //no 2nd basein the zone for now
	  }
	}
	
	$("#buildBaseButton").show()
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

function updateAirportMarkers(airline) { //set different markers for head quarter and bases
	if (!markers) { //markers not ready yet, wait
		setTimeout(function() { updateAirportMarkers(airline) }, 100)
	} else {
		var headquarterMarkerIcon = $("#map").data("headquarterMarker")
		var baseMarkerIcon = $("#map").data("baseMarker")
		$.each(airline.baseAirports, function(key, baseAirport) {
			var marker = markers[baseAirport.airportId]
			if (baseAirport.headquarter) {
				marker.setIcon(headquarterMarkerIcon) 
			} else {
				marker.setIcon(baseMarkerIcon)
			}
			marker.setZIndex(999)
			marker.isBase = true
			marker.setVisible(true)
		})
	}
}




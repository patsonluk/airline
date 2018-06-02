function showAirportDetails(airportId) {
	setActiveDiv($("#airportCanvas"))
	highlightTab($('#airportCanvasTab'))
	
	if (!airportId) {
		airportId = activeAirline.headquarterAirport.airportId
	}
	$.ajax({
		type: 'GET',
		url: "airports/" + airportId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airport) {
	    	if (airport) {
	    		populateAirportDetails(airport)
//	    		$("#floatBackButton").show()
//	    		shimmeringDiv($("#floatBackButton"))
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
	   	zoom : 6,
	   	minZoom : 6,
//	   	scrollwheel: false,
//	    navigationControl: false,
//	    mapTypeControl: false,
//	    scaleControl: false,
//	    draggable: false,
	   	gestureHandling: 'greedy',
	   	gestureHandling: 'none',
	   	fullscreenControl: false,
	   	streetViewControl: false,
        zoomControl: false,
	   	styles: 
	   		[
	   		  {
	   		    "elementType": "geometry",
	   		    "stylers": [
	   		      {
	   		        "color": "#1d2c4d"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "elementType": "labels.text.fill",
	   		    "stylers": [
	   		      {
	   		        "color": "#8ec3b9"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "elementType": "labels.text.stroke",
	   		    "stylers": [
	   		      {
	   		        "color": "#1a3646"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "administrative.country",
	   		    "elementType": "geometry.stroke",
	   		    "stylers": [
	   		      {
	   		        "color": "#4b6878"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "administrative.land_parcel",
	   		    "elementType": "labels",
	   		    "stylers": [
	   		      {
	   		        "visibility": "off"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "administrative.land_parcel",
	   		    "elementType": "labels.text.fill",
	   		    "stylers": [
	   		      {
	   		        "color": "#64779e"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "administrative.province",
	   		    "elementType": "geometry.stroke",
	   		    "stylers": [
	   		      {
	   		        "color": "#4b6878"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "landscape.man_made",
	   		    "elementType": "geometry.stroke",
	   		    "stylers": [
	   		      {
	   		        "color": "#334e87"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "landscape.natural",
	   		    "elementType": "geometry",
	   		    "stylers": [
	   		      {
	   		        "color": "#023e58"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "poi",
	   		    "elementType": "geometry",
	   		    "stylers": [
	   		      {
	   		        "color": "#283d6a"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "poi",
	   		    "elementType": "labels.text",
	   		    "stylers": [
	   		      {
	   		        "visibility": "off"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "poi",
	   		    "elementType": "labels.text.fill",
	   		    "stylers": [
	   		      {
	   		        "color": "#6f9ba5"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "poi",
	   		    "elementType": "labels.text.stroke",
	   		    "stylers": [
	   		      {
	   		        "color": "#1d2c4d"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "poi.business",
	   		    "stylers": [
	   		      {
	   		        "visibility": "off"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "poi.park",
	   		    "elementType": "geometry.fill",
	   		    "stylers": [
	   		      {
	   		        "color": "#023e58"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "poi.park",
	   		    "elementType": "labels.text.fill",
	   		    "stylers": [
	   		      {
	   		        "color": "#3C7680"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "road",
	   		    "stylers": [
	   		      {
	   		        "visibility": "off"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "road",
	   		    "elementType": "geometry",
	   		    "stylers": [
	   		      {
	   		        "color": "#304a7d"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "road",
	   		    "elementType": "labels.icon",
	   		    "stylers": [
	   		      {
	   		        "visibility": "off"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "road",
	   		    "elementType": "labels.text.fill",
	   		    "stylers": [
	   		      {
	   		        "color": "#98a5be"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "road",
	   		    "elementType": "labels.text.stroke",
	   		    "stylers": [
	   		      {
	   		        "color": "#1d2c4d"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "road.highway",
	   		    "elementType": "geometry",
	   		    "stylers": [
	   		      {
	   		        "color": "#2c6675"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "road.highway",
	   		    "elementType": "geometry.stroke",
	   		    "stylers": [
	   		      {
	   		        "color": "#255763"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "road.highway",
	   		    "elementType": "labels.text.fill",
	   		    "stylers": [
	   		      {
	   		        "color": "#b0d5ce"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "road.highway",
	   		    "elementType": "labels.text.stroke",
	   		    "stylers": [
	   		      {
	   		        "color": "#023e58"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "road.local",
	   		    "elementType": "labels",
	   		    "stylers": [
	   		      {
	   		        "visibility": "off"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "transit",
	   		    "stylers": [
	   		      {
	   		        "visibility": "off"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "transit",
	   		    "elementType": "labels.text.fill",
	   		    "stylers": [
	   		      {
	   		        "color": "#98a5be"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "transit",
	   		    "elementType": "labels.text.stroke",
	   		    "stylers": [
	   		      {
	   		        "color": "#1d2c4d"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "transit.line",
	   		    "elementType": "geometry.fill",
	   		    "stylers": [
	   		      {
	   		        "color": "#283d6a"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "transit.station",
	   		    "elementType": "geometry",
	   		    "stylers": [
	   		      {
	   		        "color": "#3a4762"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "water",
	   		    "elementType": "geometry",
	   		    "stylers": [
	   		      {
	   		        "color": "#0e1626"
	   		      }
	   		    ]
	   		  },
	   		  {
	   		    "featureType": "water",
	   		    "elementType": "labels.text.fill",
	   		    "stylers": [
	   		      {
	   		        "color": "#4e6d70"
	   		      }
	   		    ]
	   		  }
	   		]
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
		loadAirportSchedule(airport)
		loadAirportStatistics(airport)
	}
}

var flapperOptions = {
		  width: 30,             // number of digits
		  align: 'left',       // aligns values to the left or right of display
		  chars: null,          // array of characters that Flapper can display
		  chars_preset: 'alphanum',  // 'num', 'hexnum', 'alpha' or 'alphanum'
		  timing: 250,          // the maximum timing for digit animation
		  min_timing: 10,       // the minimum timing for digit animation
		  transform: true,       // Flapper automatically detects the jquery.transform
		  on_anim_start: null,   // Callback for start of animation
		  on_anim_end: null,     // Callback for end of animation
		}

function loadAirportSchedule(airport) {
	var lineCount = 17
	var date = new Date(currentTime)
	$.ajax({
		type: 'GET',
		url: "airports/" + airport.id + "/link-schedule/" + date.getDay() + "/" + date.getHours(),
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(linkSchedules) {
	    	$('#airportSchedule').empty()
	    	//var board = new DepartureBoard(document.getElementById('airportSchedule'), { rowCount: 16, letterCount: 40 });
	    	//var boardValues = []
	    	var counter = 0
	    	var departureBoardLines = []
	    	$.each(linkSchedules, function(index, entry) {
	    		$.each(entry.links, function(index, link) {
	    			counter ++
	    			if (counter < lineCount) {
		    			var text = (entry.timeSlotTime + "  " + padAfter(link.linkCode, " ", 7) + " " + link.destination).toUpperCase()
		    			//departureBoardLines.push($("<input class='XXS'/>").appendTo('#airportSchedule').flapper(flapperOptions).val(text))
		    			departureBoardLines.push({ input : $("<input class='XXS'/>").appendTo('#airportSchedule').flapper(flapperOptions), text : text})
	    			}
	    		})
	    	})
	    	
	    	
	    	for (var i = 0 ; i < (lineCount - counter - 1); i++) { //pad extra lines
	    		console.log(i + " and " + (lineCount - counter))
	    		$("<input class='XXS'/>").appendTo('#airportSchedule').flapper(flapperOptions)
	    	}
	    	
	    	function animateLines() {
	    		if (departureBoardLines.length > 0) {
	    			//departureBoardLines.shift().change()
	    			var element = departureBoardLines.shift()
	    			element.input.val(element.text).change()
		    		setTimeout(function() {
			    		animateLines()
			    	}, 2000)
	    		}
	    	}
	    	animateLines()
	    	//board.setValue(boardValues)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function loadAirportStatistics(airport) {
	$.ajax({
		type: 'GET',
		url: "airports/" + airport.id + "/link-statistics",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airportStatistics) {
	    	var transitTypeData = [
	    	 {"transitType" : "departure/arrival passengers", "passengers" : airportStatistics.departureOrArrivalPassengers},
	    	 {"transitType" : "transit passengers", "passengers" : airportStatistics.transitPassengers}
	    	 ]
	    	plotPie(transitTypeData, null , $("#transitTypePie"), "transitType", "passengers")
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
	$.getJSON( "airports?count=4000", function( data ) {
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
		  
		  var zIndex = airportInfo.size * 10 
		  var sizeAdjust = Math.floor(airportInfo.population / 1000000) //add something extra due to pop
		  if (sizeAdjust > 9) {
			sizeAdjust = 9;
		  }
		  zIndex += sizeAdjust
		  
		  marker.setZIndex(zIndex); //major airport should have higher index
		  
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

function removeMarkers() {
	$.each(markers, function(key, marker) {
		marker.setMap(null)
	});
	markers = {}
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
	$("#airportIcons .baseIcon").hide()
	
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
	$("#airportIcons .feature").hide()		  
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
	    	
	    	relationship = getRelationshipDescription(loadedCountriesByCode[airport.countryCode].mutualRelationship)
	    	
	    	$("#airportPopupRelationship").text(relationship)
	    	
	    	$("#airportPopupSlots").text(airport.availableSlots + " (" + airport.slots + ")")
	    	
	    	$.each(airport.linkCounts, function(withLinksAirlineId, linkCount) {
	    		if (airlineId == withLinksAirlineId) {
	    			updateBuildBaseButton(airport.zone)
	    		}
	  		});
	    	
	    	$.each(airport.features, function(index, feature) {
	    		$("#airportIcons").append("<div class='feature' style='display:inline'><img src='assets/images/icons/airport-features/" + feature.type + ".png' title='" + feature.title + "'><span class='label'>" +  feature.strength + "</span></div>")
	    	})
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




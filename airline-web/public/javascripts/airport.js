var airportLinkPaths = {}
var activeAirport
var activeAirportId
var activeAirportPopupInfoWindow

function showAirportDetails(airportId) {
	setActiveDiv($("#airportCanvas"))
	highlightTab($('#airportCanvasTab'))
	//deselectLink()
	var reload = false
	if (airportId) {
		if (airportId != activeAirportId) {
			activeAirportId = airportId
			reload = true
		}
	} else if (!activeAirportId){
		airportId = activeAirline.headquarterAirport.airportId
		activeAirportId = airportId
		reload = true
	}
	
	if (reload) {
		$.ajax({
			type: 'GET',
			url: "airports/" + airportId,
		    contentType: 'application/json; charset=utf-8',
		    dataType: 'json',
		    success: function(airport) {
	    		populateAirportDetails(airport) //update left panel
	//	    		$("#floatBackButton").show()
	//	    		shimmeringDiv($("#floatBackButton"))
	    		updateAirportDetails(airport) //update right panel
	    		activeAirport = airport
		    },
		    error: function(jqXHR, textStatus, errorThrown) {
		            console.log(JSON.stringify(jqXHR));
		            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
		    }
		});
	}
}

function updateAirportDetails(airport) {
	$('#airportDetailsAirportImage').empty()
	$('#airportDetailsCityImage').empty()
	if (airport.cityImageUrl) {
		$('#airportDetailsCityImage').append('<img src="' + airport.cityImageUrl + '" style="width:100%;"/>')
	}
	if (airport.airportImageUrl && airport.airportImageUrl != airport.cityImageUrl) {
		$('#airportDetailsAirportImage').append('<img src="' + airport.airportImageUrl + '" style="width:100%;"/>')
	}

	
	
	$('#airportDetailsName').text(airport.name)
	if (airport.iata) { 
		$('#airportDetailsIata').text(airport.iata)
	} else {
		$('#airportDetailsIata').text('-')
	}
	
	if (airport.icao) { 
		$('#airportDetailsIcao').text(airport.icao)
	} else {
		$('#airportDetailsIcao').text('-')
	}
	
	$('#airportDetailsPopulation').text(commaSeparateNumber(airport.population))
    $("#airportDetailsCity").text(airport.city)
    $("#airportDetailsSize").text(airport.size)
    $("#airportDetailsIncomeLevel").text(airport.incomeLevel)
	$("#airportDetailsCountry").text(loadedCountriesByCode[airport.countryCode].name)
	var countryFlagUrl = getCountryFlagUrl(airport.countryCode)
	if (countryFlagUrl) {
		$("#airportDetailsCountry").append("<img src='" + countryFlagUrl + "' />")
	}
	$("#airportDetailsZone").text(zoneById[airport.zone])
	
	updateAirportExtendedDetails(airport.id)
	updateAirportSlots(airport.id)
	
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/bases/" + airport.id,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airportBase) {
	    	if (airportBase) {
	    		if (airportBase.scale == 0) {
	    			$('#airportDetailsBaseType').text('-')
	    			$('#airportDetailsBaseScale').text('-')
	    			$('#airportDetailsBaseUpkeep').text('$' + commaSeparateNumber(airportBase.upkeep) + '(if built)')
	    			$('#airportDetailsBaseCost').text('$' + commaSeparateNumber(airportBase.upgradeCost))
	    		} else {
	    			$('#airportDetailsBaseType').text(airportBase.headquarter ? "Headquarter" : "Base")
	    			$('#airportDetailsBaseScale').text(airportBase.scale)
	    			$('#airportDetailsBaseUpkeep').text('$' + commaSeparateNumber(airportBase.upkeep))
	    			$('#airportDetailsBaseCost').text('$' + commaSeparateNumber(airportBase.upgradeCost))
	    		}
	    		
	    		//update buttons and reject reasons
	    		if (airportBase.rejection) {
	    			$('#baseRejectionReason').text(airportBase.rejection)
	    			$('#baseRejection').show()
	    			$('#buildHeadquarterButton').hide()
	    			$('#buildBaseButton').hide()
	    			$('#upgradeBaseButton').hide()
	    		} else{
	    			$('#baseRejection').hide()
	    			if (airportBase.scale == 0) {
	    				if (activeAirline.headquarterAirport) {
		    				$('#buildHeadquarterButton').hide()
		    				$('#buildBaseButton').show()
	    				} else {
	    					$('#buildHeadquarterButton').show()
		    				$('#buildBaseButton').hide()
	    				}
	    				$('#upgradeBaseButton').hide()
	    			} else {
	    				$('#buildHeadquarterButton').hide()
	    				$('#buildBaseButton').hide()
	    				$('#upgradeBaseButton').show()
	    			}
	    			
	    		}
	    		
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
	   	maxZoom : 9,
//	   	scrollwheel: false,
//	    navigationControl: false,
//	    mapTypeControl: false,
//	    scaleControl: false,
//	    draggable: false,
	   	gestureHandling: 'greedy',
	   	fullscreenControl: false,
	   	streetViewControl: false,
        zoomControl: true,
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
		//loadAirportSchedule(airport)
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
	    	
	    	$('#airportDetailsPassengerCount').text(airportStatistics.departureOrArrivalPassengers)
	    	$('#airportDetailsConnectedCountryCount').text(airportStatistics.connectedCountryCount)
	    	$('#airportDetailsConnectedAirportCount').text(airportStatistics.connectedAirportCount)
	    	$('#airportDetailsAirlineCount').text(airportStatistics.airlineCount)
	    	$('#airportDetailsLinkCount').text(airportStatistics.linkCount)
	    	$('#airportDetailsFlightFrequency').text(airportStatistics.flightFrequency)
	    	
	    	updateBaseList(airportStatistics)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateBaseList(statistics) {
	$('#airportDetailsHeadquarterList').children('.table-row').remove()
	$('#airportDetailsBaseList').children('.table-row').remove()
	
	var hasHeadquarters = false
	var hasBases = false
	$.each(statistics.bases, function(index, base) {
		var row = $("<div class='table-row'></div>")
		var countryFlagUrl = getCountryFlagUrl(base.airlineCountryCode)
		if (countryFlagUrl) {
			row.append("<div class='cell'><img src='" + countryFlagUrl + "'/>" + base.airlineName + "</div>")
		} else {
			row.append("<div class='cell'>" + base.airlineName + "</div>")
		}
		row.append("<div class='cell' style='text-align: right;'>" + base.scale + "</div>")
		
		var linkCount = 0;
		$.each(statistics.linkCountByAirline, function(index, entry) {
			if (entry.airlineId == base.airlineId) {
				linkCount = entry.linkCount;
				return false; //break
			}
		});
		var passengers = 0
		$.each(statistics.airlineDeparture, function(index, entry) {
			if (entry.airlineId == base.airlineId) {
				passengers += entry.passengers;
				return false; //break
			}
		});
		$.each(statistics.airlineArrival, function(index, entry) {
			if (entry.airlineId == base.airlineId) {
				passengers += entry.passengers;
				return false; //break
			}
		});
		
		row.append("<div class='cell' style='text-align: right;'>" + linkCount + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(passengers) + "</div>")
		
		if (base.headquarter) {
			$('#airportDetailsHeadquarterList').append(row)
			hasHeadquarters = true
		} else {
			$('#airportDetailsBaseList').append(row)
			hasBases = true
		}
	})
	
	if (!hasHeadquarters) {
		var emtpyRow = $("<div class='table-row'></div>")
		emtpyRow.append("<div class='cell'>-</div>")
		emtpyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emtpyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emtpyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		$('#airportDetailsHeadquarterList').append(emtpyRow)
	}
	if (!hasBases) {
		var emtpyRow = $("<div class='table-row'></div>")
		emtpyRow.append("<div class='cell'>-</div>")
		emtpyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emtpyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emtpyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		$('#airportDetailsBaseList').append(emtpyRow)
	}

	

}


function getAirports() {
	$.getJSON( "airports?count=4000", function( data ) {
		  addMarkers(data)
	});
}

function addMarkers(airports) {
	var infoWindow = new google.maps.InfoWindow({
		maxWidth : 450
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
//			    airportName: airportInfo.name,
//		  		airportCode: airportInfo.iata,
//		  		airportCity: airportInfo.city,
//		  		airportId: airportInfo.id,
//		  		airportSize: airportInfo.size,
//		  		airportPopulation: airportInfo.population,
//		  		airportIncomeLevel: airportInfo.incomeLevel,
//		  		airportCountryCode: airportInfo.countryCode,
//		  		airportZone : airportInfo.zone,
//		  		airportAvailableSlots: airportInfo.availableSlots,
		  		airport : airportInfo,
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
			  
			  activeAirport = this.airport
			  
			  var isBase = updateBaseInfo(this.airport.id)
			  $("#airportPopupName").text(this.airport.name)
			  $("#airportPopupIata").text(this.airport.iata)
			  $("#airportPopupCity").html(this.airport.city + "&nbsp;" + getCountryFlagImg(this.airport.countryCode))
			  $("#airportPopupZone").text(zoneById[this.airport.zone])
			  $("#airportPopupSize").text(this.airport.size)
			  $("#airportPopupPopulation").text(commaSeparateNumber(this.airport.population))
			  $("#airportPopupIncomeLevel").text(this.airport.incomeLevel)
			  updateAirportExtendedDetails(this.airport.id)
			  updateAirportSlots(this.airport.id)
			  
			  $("#airportPopupId").val(this.airport.id)
			  infoWindow.setContent($("#airportPopup").html())
			  infoWindow.open(map, this);
			  
			  activeAirportPopupInfoWindow = infoWindow
			  
			  if (!activeAirline.headquarterAirport) {
				  $("#planToAirportButton").hide()
			  } else if (this.airport.id == activeAirline.headquarterAirport.airportId) {
				  $("#planToAirportButton").hide()
			  } else {
				  $("#planToAirportButton").click(function() {
					  planToAirport($('#airportPopupId').val(), $('#airportPopupName').text())
					  infoWindow.close();
				  });
				  $("#planToAirportButton").show()
			  }
			  
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
	

	cities.sort(sortByProperty("population", false))
	var count = 0
	$.each(cities, function( key, city ) {
		if (++ count > 20) { //do it for top 20 cities only
			return false
		}	
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
	return (marker.isBase) || ((zoom >= 4) && (zoom + marker.airport.size / 2 >= 7.5)) //start showing size >= 7 at zoom 4 
}

function updateBaseInfo(airportId) {
	$("#buildHeadquarterButton").hide()
	//$("#buildBaseButton").hide()
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

function updateAirportExtendedDetails(airportId) {
	//clear the old values
	$(".airportSlots").text('-')
	$(".airportAwareness").text('-')
	$(".airportLoyalty").text('-')
	$(".airportRelationship").text('-')
	$(".airportOpenness").text('-')
	$("#airportIcons .feature").hide()
	
	var airlineId = activeAirline.id
	$.ajax({
		type: 'GET',
		url: "airports/" + airportId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airport) {
	    	if (activeAirline.headquarterAirport) { //if this airline has picked headquarter
	    		var hasMatch = false
		    	$.each(airport.appealList, function( key, appeal ) {
		    		if (appeal.airlineId == airlineId) {
		    			$(".airportAwareness").text(appeal.awareness)
		    			$(".airportLoyalty").text(appeal.loyalty)
		    			hasMatch = true
		    		}
		  		});
		    	if (!hasMatch) {
		    		$(".airportAwareness").text("0")
		    		$(".airportLoyalty").text("0")
		    	}
		    	
		    	relationship = getRelationshipDescription(loadedCountriesByCode[airport.countryCode].mutualRelationship)
		    	
		    	$(".airportRelationship").text(relationship)
		    	$(".airportOpenness").html(getOpennessSpan(loadedCountriesByCode[airport.countryCode].openness))
	    	}
	    	
	    	$(".airportSlots").text(airport.availableSlots + " / " + airport.slots)
	    	
//	    	$.each(airport.linkCounts, function(withLinksAirlineId, linkCount) {
//	    		if (airlineId == withLinksAirlineId) {
//	    			updateBuildBaseButton(airport.zone)
//	    		}
//	  		});
	    	
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

//function updateBuildBaseButton(airportZone) { //check if the zone already has base
//	for (i = 0; i < activeAirline.baseAirports.length; i++) {
//	  if (activeAirline.baseAirports[i].airportZone == airportZone) {
//		  return //no 2nd base in the zone but different country for now
//	  }
//	}
//	
//	//$("#buildBaseButton").show()
//}

function updateAirportSlots(airportId) {
	//clear the old ones
	$(".airportAssignedSlots").text()
	
	var airlineId = activeAirline.id
	$.ajax({
		type: 'GET',
		url: "airports/" + airportId + "/slots?airlineId=" + airlineId,
	    dataType: 'json',
	    success: function(slotInfo) {
	    	var availableSlots = slotInfo.maxSlots - slotInfo.assignedSlots 
    		$(".airportAssignedSlots").text(availableSlots + " / " + slotInfo.maxSlots)
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

//airport links view

function toggleAirportLinksView() {
	clearAirportLinkPaths() //clear previous ones if exist
	deselectLink()
	
	//push here otherwise it's not centered
	if (map.controls[google.maps.ControlPosition.TOP_CENTER].getLength() == 0) {
		map.controls[google.maps.ControlPosition.TOP_CENTER].push(createMapButton(map, 'Exit Airport Flight Map', 'hideAirportLinksView()', 'hideAirportLinksButton')[0]);
	}
	
	
	
	toggleAirportLinks(activeAirport)
}


function toggleAirportLinks(airport) {
	clearAllPaths()
	activeAirportPopupInfoWindow.close(map)
	activeAirportPopupInfoWindow = undefined
	$.ajax({
		type: 'GET',
		url: "airports/" + airport.id + "/link-consumptions",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(passengersByRemoteAirport) {
	    	if (!jQuery.isEmptyObject(passengersByRemoteAirport)) {
	    		$.each(passengersByRemoteAirport, function(index, remoteAirportPassengers) {
	    			drawAirportLinkPath(airport, remoteAirportPassengers.remoteAirport, remoteAirportPassengers.passengers)
	    		})
	    		showAirportLinkPaths()
	    		printConsole('Showing all routes with passenger volume flying from or to this airport')
	    	} else {
	    		printConsole('No routes with passenges yet flying from or to this airport')
	    	}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function drawAirportLinkPath(localAirport, remoteAirport, passengers) {
	var from = new google.maps.LatLng({lat: localAirport.latitude, lng: localAirport.longitude})
	var to = new google.maps.LatLng({lat: remoteAirport.latitude, lng: remoteAirport.longitude})
	var pathKey = remoteAirport.id
	
	var airportLinkPath = new google.maps.Polyline({
			 geodesic: true,
		     strokeColor: "#DC83FC",
		     strokeOpacity: 0.8,
		     strokeWeight: 2,
		     path: [from, to],
		     zIndex : 1100,
		});
		
	var fromAirport = getAirportText(localAirport.city, localAirport.name)
	var toAirport = getAirportText(remoteAirport.city, remoteAirport.name)
	
	
	shadowPath = new google.maps.Polyline({
		 geodesic: true,
	     strokeColor: "#DC83FC",
	     strokeOpacity: 0.0001,
	     strokeWeight: 25,
	     path: [from, to],
	     zIndex : 401,
	     fromAirport : fromAirport,
	     fromCountry : localAirport.countryCode, 
	     toAirport : toAirport,
	     toCountry : remoteAirport.countryCode,
	     passengers : passengers,
	});
	
	airportLinkPath.shadowPath = shadowPath
	
	var infowindow; 
	shadowPath.addListener('mouseover', function(event) {
		$("#airportLinkPopupFrom").html(this.fromAirport + "&nbsp;" + getCountryFlagImg(this.fromCountry))
		$("#airportLinkPopupTo").html(this.toAirport + "&nbsp;" + getCountryFlagImg(this.toCountry))
		$("#airportLinkPopupPassengers").text(this.passengers)
		infowindow = new google.maps.InfoWindow({
             content: $("#airportLinkPopup").html(),
             maxWidth : 600});
		
		infowindow.setPosition(event.latLng);
		infowindow.open(map);
	})		
	shadowPath.addListener('mouseout', function(event) {
		infowindow.close()
	})
	
	airportLinkPaths[pathKey] = airportLinkPath
}

function showAirportLinkPaths() {
	$.each(airportLinkPaths, function(key, airportLinkPath) {
		var totalPassengers = airportLinkPath.shadowPath.passengers
		if (totalPassengers > 5000) {
			airportLinkPath.setOptions({strokeWeight : 3})
		} else if (totalPassengers > 10000) {
			airportLinkPath.setOptions({strokeWeight : 4})
		} else if (totalPassengers < 1000) {
			var newOpacity = 0.2 + totalPassengers / 1000 * (airportLinkPath.strokeOpacity - 0.2)
			airportLinkPath.setOptions({strokeOpacity : newOpacity})
		}
			
		airportLinkPath.setMap(map)
		airportLinkPath.shadowPath.setMap(map)
	})
}
	
function clearAirportLinkPaths() {
	$.each(airportLinkPaths, function(key, airportLinkPath) {
		airportLinkPath.setMap(null)
		airportLinkPath.shadowPath.setMap(null)
	})
	
	airportLinkPaths = {}
}


function hideAirportLinksView() {
	printConsole('')
	clearAirportLinkPaths()
	updateLinksInfo() //redraw all flight paths
		
//	$("#linkHistoryPanel").fadeOut(200);
//	$("#hideAirportLinksButton").hide()
	map.controls[google.maps.ControlPosition.TOP_CENTER].clear()
	
//	map.controls[google.maps.ControlPosition.TOP_CENTER].clear();
//	map.controls[google.maps.ControlPosition.RIGHT_TOP].clear();
	
}


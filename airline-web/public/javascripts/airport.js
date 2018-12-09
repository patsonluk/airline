var airportLinkPaths = {}
var activeAirport
var activeAirportId
var activeAirportPopupInfoWindow

function showAirportDetails(airportId) {
	setActiveDiv($("#airportCanvas"))
	//highlightTab($('#airportCanvasTab'))
	
	$('#main-tabs').children('.left-tab').children('span').removeClass('selected')
	//deselectLink()
	
	activeAirportId = airportId
	
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
	$("#airportDetailsOpenness").html(getOpennessSpan(loadedCountriesByCode[airport.countryCode].openness))
	
	updateAirportExtendedDetails(airport.id)
	updateAirportSlots(airport.id)
	
	if (activeAirline) {
		$('#airportBaseDetails').show()
		$.ajax({
			type: 'GET',
			url: "airlines/" + activeAirline.id + "/bases/" + airport.id,
		    contentType: 'application/json; charset=utf-8',
		    dataType: 'json',
		    success: function(baseDetails) {
		    	var airportBase = baseDetails.base
		    	if (!airportBase) { //new base
	    			$('#airportDetailsBaseType').text('-')
	    			$('#airportDetailsBaseScale').text('-')
	    			$('#airportDetailsBaseUpkeep').text('-')
	    			$('#airportDetailsFacilities').empty()
	    		} else {
	    			$('#airportDetailsBaseType').text(airportBase.headquarter ? "Headquarter" : "Base")
	    			$('#airportDetailsBaseScale').text(airportBase.scale)
	    			$('#airportDetailsBaseUpkeep').text('$' + commaSeparateNumber(airportBase.upkeep))
	    			updateFacilityIcons(airport)
	    		}
		    	$('#airportDetailsLinkLimitBoost').text(baseDetails.linkLimitDomestic + " / " + baseDetails.linkLimitRegional)
		    	var targetBase = baseDetails.targetBase
		    	$('#airportDetailsBaseUpgradeCost').text('$' + commaSeparateNumber(targetBase.value))
    			$('#airportDetailsBaseUpgradeUpkeep').text('$' + commaSeparateNumber(targetBase.upkeep))

	    		
	    		//update buttons and reject reasons
	    		if (baseDetails.rejection) {
	    			$('#baseRejectionReason').text(baseDetails.rejection)
	    			$('#baseRejection').show()
	    			$('#buildHeadquarterButton').hide()
	    			$('#buildBaseButton').hide()
	    			$('#upgradeBaseButton').hide()
	    		} else{
	    			$('#baseRejection').hide()
	    			if (!airportBase) {
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
		    	
		    	if (baseDetails.downgradeRejection) {
		    		$('#downgradeRejectionReason').text(baseDetails.downgradeRejection)
	    			$('#downgradeRejection').show()
	    			$('#downgradeBaseButton').hide()
		    	} else {
		    		$('#downgradeRejection').hide()
		    		if (airportBase) {
		    			$('#downgradeBaseButton').show()
		    		} else {
		    			$('#downgradeBaseButton').hide()
		    		}
		    	}
		    	
	    		
	    		if (!airportBase || airportBase.headquarter) {
	    			$('#deleteBaseButton').hide()
	    		} else {
	    			$('#deleteBaseButton').show()
	    		}
		    	
		    },
		    error: function(jqXHR, textStatus, errorThrown) {
		            console.log(JSON.stringify(jqXHR));
		            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
		    }
		});
	} else {
		$('#airportBaseDetails').hide()
	}
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
	   	styles: getMapStyles()
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
	    success: function(airportStatistics) {
	    	var transitTypeData = [
	    	 {"transitType" : "departure/arrival passengers", "passengers" : airportStatistics.departureOrArrivalPassengers},
	    	 {"transitType" : "transit passengers", "passengers" : airportStatistics.transitPassengers}
	    	 ]
	    	plotPie(transitTypeData, null , $("#transitTypePie"), "transitType", "passengers")
	    	
	    	assignAirlineColors(airportStatistics.airlineDeparture, "airlineId")
	    	assignAirlineColors(airportStatistics.airlineArrival, "airlineId")
	    	
	    	plotPie(airportStatistics.airlineDeparture, activeAirline ? activeAirline.name : null , $("#airlineDeparturePie"), "airlineName", "passengers")
	    	plotPie(airportStatistics.airlineArrival, activeAirline ? activeAirline.name : null, $("#airlineArrivalPie"), "airlineName", "passengers")
	    	
	    	$('#airportDetailsPassengerCount').text(airportStatistics.departureOrArrivalPassengers)
	    	$('#airportDetailsConnectedCountryCount').text(airportStatistics.connectedCountryCount)
	    	$('#airportDetailsConnectedAirportCount').text(airportStatistics.connectedAirportCount)
	    	$('#airportDetailsAirlineCount').text(airportStatistics.airlineCount)
	    	$('#airportDetailsLinkCount').text(airportStatistics.linkCount)
	    	$('#airportDetailsFlightFrequency').text(airportStatistics.flightFrequency)
	    	
	    	updateFacilityList(airportStatistics)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateFacilityList(statistics) {
	$('#airportDetailsHeadquarterList').children('.table-row').remove()
	$('#airportDetailsBaseList').children('.table-row').remove()
	$('#airportDetailsLoungeList').children('.table-row').remove()
	
	
	var hasHeadquarters = false
	var hasBases = false
	var hasLounges = false
	$.each(statistics.bases, function(index, base) {
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell'>" +  getAirlineLogoImg(base.airlineId) + base.airlineName + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + getCountryFlagImg(base.airlineCountryCode) + "</div>")
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
	
	$.each(statistics.lounges, function(index, loungeStats) {
		var lounge = loungeStats.lounge
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell'>" +  getAirlineLogoImg(lounge.airlineId) + lounge.airlineName + "</div>")
		row.append("<div class='cell'>" + lounge.name + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + lounge.level + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + lounge.status + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(loungeStats.selfVisitors) + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(loungeStats.allianceVisitors) + "</div>")
		
		$('#airportDetailsLoungeList').append(row)
		hasLounges = true
	})
	
	if (!hasHeadquarters) {
		var emtpyRow = $("<div class='table-row'></div>")
		emtpyRow.append("<div class='cell'>-</div>")
		emtpyRow.append("<div class='cell' style='text-align: right;'>-</div>")
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
		emtpyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		$('#airportDetailsBaseList').append(emtpyRow)
	}
	if (!hasLounges) {
		var emtpyRow = $("<div class='table-row'></div>")
		emtpyRow.append("<div class='cell'>-</div>")
		emtpyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emtpyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emtpyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emtpyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		$('#airportDetailsLoungeList').append(emtpyRow)
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
			  
			  var isBase = false;
			  
			  if (activeAirline) {
				  updateBaseInfo(this.airport.id)
			  }
			  $("#airportPopupName").text(this.airport.name)
			  $("#airportPopupIata").text(this.airport.iata)
			  $("#airportPopupCity").html(this.airport.city + "&nbsp;" + getCountryFlagImg(this.airport.countryCode))
			  $("#airportPopupZone").text(zoneById[this.airport.zone])
			  $("#airportPopupSize").text(this.airport.size)
			  $("#airportPopupPopulation").text(commaSeparateNumber(this.airport.population))
			  $("#airportPopupIncomeLevel").text(this.airport.incomeLevel)
			  $("#airportPopupOpenness").html(getOpennessSpan(loadedCountriesByCode[this.airport.countryCode].openness))
			  updateAirportExtendedDetails(this.airport.id)
			  updateAirportSlots(this.airport.id)
			  
			  $("#airportPopupId").val(this.airport.id)
			  infoWindow.setContent($("#airportPopup").html())
			  infoWindow.open(map, this);
			  
			  activeAirportPopupInfoWindow = infoWindow
			  
			  if (activeAirline) {
				  if (!activeAirline.headquarterAirport) {
					  $("#planToAirportButton").hide()
				  } else if (this.airport.id == activeAirline.headquarterAirport.airportId) {
					  $("#planToAirportButton").hide()
				  } else {
					  $("#planToAirportButton").show()
				  }
			  } else {
				  $("#planToAirportButton").hide()
			  }
			  
		  });
		  marker.setVisible(isShowMarker(marker, currentZoom))
		  resultMarkers[airportInfo.id] = marker
	}
	//now assign it to markers to indicate that it's ready
	markers = resultMarkers
}

function planToAirportFromInfoWindow() {
	activeAirportPopupInfoWindow.close();
	planToAirport($('#airportPopupId').val(), $('#airportPopupName').text())
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
			  $("#cityPopupCountryCode").append("<img class='flag' src='assets/images/flags/" + city.countryCode + ".png' />")
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
}

function updateAirportExtendedDetails(airportId) {
	//clear the old values
	$(".airportSlots").text('-')
	$(".airportAwareness").text('-')
	$(".airportLoyalty").text('-')
	$(".airportRelationship").text('-')
	$("#airportIcons .feature").hide()

	if (activeAirline) {
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
		    	}
		    	
		    	$(".airportSlots").text(airport.slots)
		    	
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
	
	if (activeAirline) {
		var airlineId = activeAirline.id
		$.ajax({
			type: 'GET',
			url: "airports/" + airportId + "/slots?airlineId=" + airlineId,
		    dataType: 'json',
		    success: function(slotInfo) {
		    	var availableSlots = slotInfo.preferredSlots - slotInfo.assignedSlots 
	    		$(".airportAssignedSlots").text(availableSlots + " / " + slotInfo.maxSlots)
	    		if (availableSlots < 0) {
	    			$(".airportAssignedSlots").addClass("warning")
	    		} else {
	    			$(".airportAssignedSlots").removeClass("warning")
	    		}
		    },
		    error: function(jqXHR, textStatus, errorThrown) {
		            console.log(JSON.stringify(jqXHR));
		            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
		    }
		});
	}
}


function updateAirportMarkers(airline) { //set different markers for head quarter and bases
	if (!markers) { //markers not ready yet, wait
		setTimeout(function() { updateAirportMarkers(airline) }, 100)
	} else {
		//reset baseMarkers
		$.each(baseMarkers, function(index, marker) {
			marker.setIcon(marker.originalIcon)
		})
		baseMarkers = []
		var headquarterMarkerIcon = $("#map").data("headquarterMarker")
		var baseMarkerIcon = $("#map").data("baseMarker")
		$.each(airline.baseAirports, function(key, baseAirport) {
			var marker = markers[baseAirport.airportId]
			marker.originalIcon = marker.icon
			if (baseAirport.headquarter) {
				marker.setIcon(headquarterMarkerIcon) 
			} else {
				marker.setIcon(baseMarkerIcon)
			}
			marker.setZIndex(999)
			marker.isBase = true
			marker.setVisible(true)
			baseMarkers.push(marker)			
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
	    },
	    beforeSend: function() {
	    	$('body .loadingSpinner').show()
	    },
	    complete: function(){
	    	$('body .loadingSpinner').hide()
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
		polylines.push(airportLinkPath)
		polylines.push(airportLinkPath.shadowPath)
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


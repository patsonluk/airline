var flightPaths = {} //key: link id, value : { path, shadow }
var flightMarkers = {} //key: link id, value: { markers : array[], animation}
//var flightMarkerAnimations = []
var historyPaths = {}
var linkHistoryState = "hidden"
var tempPath //temp path for new link creation

	
function updateAirlineInfo(airlineId) {
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airline) {
	    	refreshTopBar(airline)
	    	$("#currentAirline").text(airline.name)
	    	if (airline.headquarterAirport) {
	    		$("#currentAirlineCountry").html("<img src='assets/images/flags/" + airline.headquarterAirport.countryCode + ".png' />")
	    	} else {
	    		$("#currentAirlineCountry").empty()
	    	}
	    	$("#serviceFunding").val(airline.serviceFunding)
			$("#maintenanceQuality").val(airline.maintenanceQuality)
			plotMaintenanceQualityGauge($("#maintenanceQualityGauge"), $("#maintenanceQuality"))
	    	activeAirline = airline
	    	updateAirplaneList()
	    	updateLinksInfo()
	    	updateAirportMarkers(airline)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}
	
	
	
function refreshTopBar(airline) {
	changeColoredElementValue($("#balance"), airline.balance)
	changeColoredElementValue($("#reputation"), airline.reputation)
	changeColoredElementValue($("#serviceQuality"), airline.serviceQuality)
	$("#reputationLevel").text("(" + getAirlineCategory(airline.reputation) + ")")
}

function loadAirlines() {
	$.ajax({
		type: 'GET',
		url: "airlines",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airlines) {
	    	$.each(airlines, function( key, airline ) {
	    		var optionItem = $("<option></option>").attr("value", airline.id).text(airline.name)
	    		$("#airlineOption").append(optionItem);
	  		});
	    	
	    	if ($("#airlineOption option:first")) {
	    		selectAirline($("#airlineOption option:first").val())
	    	}
	    	
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function selectAirline(airlineId) {
	initWebSocket(airlineId)
	updateAllPanels(airlineId)
}

function buildBase(airportId, isHeadquarter) {
	var url = "airlines/" + activeAirline.id + "/bases/" + $("#airportPopupId").val() 
	var baseData = { 
			"airportId" : parseInt($("#airportPopupId").val()),
			"airlineId" : activeAirline.id,
			"scale" : 1,
			"headquarter" : isHeadquarter}
	$.ajax({
		type: 'PUT',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    data: JSON.stringify(baseData),
	    dataType: 'json',
	    success: function() {
	    	updateAllPanels(activeAirline.id)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function clearMarkerEntry(markerEntry) {
	//remove all animation intervals
	window.clearInterval(markerEntry.animation)
	
	//remove all markers
	$.each(markerEntry.markers, function(key, marker) {
		marker.setMap(null)
	})
}

function clearPathEntry(pathEntry) {
	pathEntry.path.setMap(null)
	pathEntry.shadow.setMap(null)
}

//remove and re-add all the links
function updateLinksInfo() {

	$.each(flightMarkers, function( linkId, markerEntry ) {
		clearMarkerEntry(markerEntry)
	});
	//remove all links from UI first
	$.each(flightPaths, function( key, pathEntry ) {
		clearPathEntry(pathEntry)
	})
	
	flightPaths = {}
	flightMarkers = {}
	
	//remove from link list
	$('#linkList').empty()

	//remove link details
	//$("#linkDetails").hide()
	
	var url = "airlines/" + activeAirline.id + "/links?getProfit=true"
	
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(links) {
	    	$.each(links, function( key, link ) {
	    		drawFlightPath(link)
	    		insertLinkToList(link)
	  		});
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

//refresh links without removal/addition
function refreshLinks() {
	var url = "airlines/" + activeAirline.id + "/links?getProfit=true"
	
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(links) {
	    	$.each(links, function( key, link ) {
	    		refreshFlightPath(link)
	  		});
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}



function drawFlightPath(link) {
	
   var flightPath = new google.maps.Polyline({
     path: [{lat: link.fromLatitude, lng: link.fromLongitude}, {lat: link.toLatitude, lng: link.toLongitude}],
     geodesic: true,
     strokeColor: getLinkColor(link.profit, link.revenue),
     strokeOpacity: 0.6,
     strokeWeight: 2,
     frequency : link.frequency,
     modelId : link.modelId,
     zIndex: 90
   });
   
   var icon = "assets/images/icons/airplane.png"
   
   flightPath.setMap(map)
   
   var shadowPath = new google.maps.Polyline({
	     path: [{lat: link.fromLatitude, lng: link.fromLongitude}, {lat: link.toLatitude, lng: link.toLongitude}],
	     geodesic: true,
	     map: map,
	     strokeColor: getLinkColor(link.profit, link.revenue),
	     strokeOpacity: 0.01,
	     strokeWeight: 15,
	     zIndex: 100
	   });
   
   var resultPath = { path : flightPath, shadow : shadowPath }
   if (link.id) {
	  shadowPath.addListener('click', function() {
	   		selectLinkAndLoadDetails(link.id, false)
	  });
      drawFlightMarker(flightPath, link);
	  flightPaths[link.id] = resultPath 
   }
   
   return resultPath
}

function refreshFlightPath(link) {
	if (flightPaths[link.id]) {
		var path = flightPaths[link.id].path
		if (path.frequency != link.frequency || path.modelId != link.modelId) { //require marker change
			path.frequency = link.frequency
			path.modelId = link.modelId
			
			drawFlightMarker(path, link)
		} 
		path.setOptions({ strokeColor : getLinkColor(link.profit, link.revenue)})
	
		//flightPaths[link.id].setOptions({ strokeColor : getLinkColor(link)})
	}
}

function getLinkColor(profit, revenue) {
   if (profit !== undefined) {
	   var maxProfitFactor = 0.5
	   var minProfitFactor = -0.5
	   var profitFactor
	   if (revenue > 0) {
		   profitFactor = profit / revenue
	   } else if (profit < 0) { //revenue 0, losing money
		   profitFactor = minProfitFactor
	   } else {
		   profitFactor = 0
	   }
	   
	   if (profitFactor > maxProfitFactor) {
		   profitFactor = maxProfitFactor
	   } else if (profitFactor < minProfitFactor) {
		   profitFactor = minProfitFactor
	   }
	   var redHex 
	   if (profitFactor > 0) {
		   redHex = 220 * (1 - (profitFactor / maxProfitFactor)) 
	   } else { 
		   redHex = 220 
	   }
	   var greenHex
	   if (profitFactor < 0) { 
		   greenHex = 220 * (1 + (profitFactor / maxProfitFactor)) 
	   } else { 
		   greenHex = 220 
	   }
	   
	   var redHexString = parseInt(redHex).toString(16)
	   if (redHexString.length == 1) { redHexString = "0" + redHexString }
	   var greenHexString = parseInt(greenHex).toString(16)
	   if (greenHexString.length == 1) { greenHexString = "0" + greenHexString }
	   return colorHex = "#" + redHexString + greenHexString + "20"
   } else  { //no history yet
	   return "#DCDC20"
   }
}

function highlightPath(path) {
	var originalColorString = path.path.strokeColor
	path.path.originalColor = originalColorString
	var totalFrames = 20
	
	var rgbHexValue = parseInt(originalColorString.substring(1), 16);
	var currentRgb = { r : rgbHexValue >> (4 * 4), g : rgbHexValue >> (2 * 4) & 0xff, b : rgbHexValue & 0xff }
	var highlightColor = { r : 0xff, g : 0xff, b : 0xff}
	var colorStep = { r : (highlightColor.r - currentRgb.r) / totalFrames, g : (highlightColor.g - currentRgb.g) / totalFrames, b : (highlightColor.b - currentRgb.b) / totalFrames }
	var currentFrame = 0
	var animation = window.setInterval(function() {
		if (currentFrame < totalFrames) { //transition to highlight color
			currentRgb = { r : currentRgb.r + colorStep.r, g : currentRgb.g + colorStep.g, b : currentRgb.b + colorStep.b }
		} else { //transition back to original color
			currentRgb = { r : currentRgb.r - colorStep.r, g : currentRgb.g - colorStep.g, b : currentRgb.b - colorStep.b }
		}
		//convert currentRgb back to hexstring
		var redHex = Math.round(currentRgb.r).toString(16)
		if (redHex.length < 2) {
			redHex = "0" + redHex
		}
		var greenHex = Math.round(currentRgb.g).toString(16)
		if (greenHex.length < 2) {
			greenHex = "0" + greenHex
		}
		var blueHex = Math.round(currentRgb.b).toString(16)
		if (blueHex.length < 2) {
			blueHex = "0" + blueHex
		}
		 
		var colorHexString = "#" + redHex + greenHex + blueHex
		path.path.setOptions({ strokeColor : colorHexString , strokeWeight : 4, zIndex : 91})
		
		currentFrame = (currentFrame + 1) % (totalFrames * 2)
		
	}, 50)
	path.path.animation = animation
}
function unhighlightPath(path) {
	window.clearInterval(path.path.animation)
	path.path["animation"] = undefined
	path.path.setOptions({ strokeColor : path.path.originalColor , strokeWeight : 2, zIndex : 90})
}



//Use the DOM setInterval() function to change the offset of the symbol
//at fixed intervals.
function drawFlightMarker(line, link) {
	var linkId = link.id
	
	//clear the old entry first
	var oldMarkerEntry = flightMarkers[link.id]
	if (oldMarkerEntry) {
		clearMarkerEntry(oldMarkerEntry)
	}
	
	if (link.assignedAirplanes && link.assignedAirplanes.length > 0) {
		var from = line.getPath().getAt(0)
		var to = line.getPath().getAt(1)
		var image = {
	        url: "assets/images/markers/dot.png",
	        origin: new google.maps.Point(0, 0),
	        anchor: new google.maps.Point(6, 6),
	    };
	
		var totalIntervals = 60 * 24 * 7 //min in a week
		var frequency = link.frequency
		var airplaneCount = link.assignedAirplanes.length
		var frequencyByAirplane = {}
		$.each(link.assignedAirplanes, function(key, airplane) {
			frequencyByAirplane[key] = Math.floor(frequency / airplaneCount)
		})
		for (i = 0; i < frequency % airplaneCount; i++) { //assign the remainder
			frequencyByAirplane[i] = frequencyByAirplane[i] + 1
		}
		 
		var markersOfThisLink = []
		$.each(frequencyByAirplane, function(key, airplane) {
			var marker = new google.maps.Marker({
			    position: from,
			    icon : image, 
			    totalDistance : link.distance,
			    totalDuration : link.duration * 2, //make it X2 duration as we dont show return trip. so a single trip should animate double the duration
			    elapsedDuration : 0,
			    nextDepartureFrame : Math.floor(key * link.duration * 2 / airplaneCount),
				departureInterval : Math.floor(totalIntervals / frequencyByAirplane[key]),
			    isActive: false,
			    clickable: false
			});
			
			//flightMarkers.push(marker)
			markersOfThisLink.push(marker)
		})
		
		flightMarkers[linkId] = {} //initialize
		flightMarkers[linkId].markers = markersOfThisLink
		
		var count = 0;
		var animation = window.setInterval(function() {
			$.each(markersOfThisLink, function(key, marker) { 
				if (count == marker.nextDepartureFrame) {
					marker.isActive = true
					marker.elapsedDuration = 0
					marker.setPosition(from)
					marker.setMap(map)
				} else if (marker.isActive) {
					marker.elapsedDuration += 1
					
					if (marker.elapsedDuration == marker.totalDuration) { //arrived
						marker.setMap(null)
						marker.isActive = false
						marker.nextDepartureFrame = (marker.nextDepartureFrame + marker.departureInterval) % totalIntervals
						//console.log("next departure " + marker.nextDepartureFrame)
					} else {
						var newPosition = google.maps.geometry.spherical.interpolate(from, to, marker.elapsedDuration / marker.totalDuration)
						marker.setPosition(newPosition)
					}
				}
			})
			count = (count + 1) % totalIntervals;
		}, 20)
		
		flightMarkers[linkId].animation = animation;
	}
}




function insertLinkToList(link) {
	var linkList = $('#linkList')
	linkList.append($("<a href='javascript:void(0)' data-link-id='" +  link.id + "' onclick='selectLinkAndLoadDetails(" + link.id + ", true)'></a>").text(link.fromAirportCode + " => " + link.toAirportCode + "(" + parseInt(link.distance) + "km)"))
	linkList.append($("<br/>"))
}

function unselectLink() {
	var previousSelectedListItem = $("#linkList a.selected")
	 
	if (previousSelectedListItem.length > 0) {
		previousSelectedListItem.removeClass("selected")
		var previousLinkId = previousSelectedListItem.data("linkId")
		unhighlightPath(flightPaths[previousLinkId])
	}
	selectedLink = undefined
	
	$("#linkDetails").hide()
}

function selectLinkAndLoadDetails(linkId, refocus) {
	selectLink(linkId, refocus)
	refreshLinkDetails(linkId)
}

/**
 * Performs UI changes for selecting a link
 */
function selectLink(linkId, refocus) {
	unselectLink()
	
	if (tempPath) {
		removeTempPath(tempPath)
	}
	
	//highlight the selected link's flight path
	highlightPath(flightPaths[linkId])
	
	//focus to the from airport
	if (refocus) {
		map.setCenter(flightPaths[linkId].path.getPath().getAt(0))
	}
	
	//highlight the corresponding list item
	var selectedListItem = $("#linkList a[data-link-id='" + linkId + "']")
	selectedListItem.addClass("selected")
	
	selectedLink = linkId
	setActiveDiv($("#linkDetails"))
	$("#actionLinkId").val(linkId)
}

function refreshLinkDetails(linkId) {
	var airlineId = activeAirline.id
	
	$("#linkCompetitons .data-row").remove()
	
	//load link
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/links/" + linkId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(link) {
	    	$("#linkFromAirport").text(getAirportText(link.fromAirportCity, link.fromAirportName))
	    	$("#linkFromAirport").append("<img src='assets/images/flags/" + link.fromCountryCode + ".png' />")
	    	$("#linkToAirport").text(getAirportText(link.toAirportCity, link.toAirportName))
	    	$("#linkToAirport").append("<img src='assets/images/flags/" + link.toCountryCode + ".png' />")
	    	$("#linkCurrentPrice").text(toLinkClassValueString(link.price, "$"))
	    	$("#linkDistance").text(link.distance)
	    	$("#linkQuality").text(link.computedQuality)
	    	$("#linkCurrentCapacity").text(toLinkClassValueString(link.capacity))
	    	$("#linkCurrentDetails").show()
	    	$("#linkToAirportId").val(link.toAirportId)
	    	$("#linkFromAirportId").val(link.fromAirportId)
	    	
	    	//load competition
	    	$.ajax({
	    		type: 'GET',
	    		url: "airports/" + link.fromAirportId + "/to/" + link.toAirportId,
	    	    contentType: 'application/json; charset=utf-8',
	    	    dataType: 'json',
	    	    success: function(linkConsumptions) {
    	    		$.each(linkConsumptions, function(index, linkConsumption) {
    	    			if (linkConsumption.airlineId != airlineId) {
		    	    		$("#linkCompetitons").append("<div class='table-row data-row'><div style='display: table-cell;'>" + linkConsumption.airlineName
		    	    				+ "</div><div style='display: table-cell;'>" + toLinkClassValueString(linkConsumption.price, "$")
		    	    				+ "</div><div style='display: table-cell;'>" + linkConsumption.capacity 
		    	    				+ "</div><div style='display: table-cell;'>" + linkConsumption.quality + "</div></div>")
    	    			}
	    	    	})
	    	    	if ($("#linkCompetitons .data-row").length == 0) {
	    	    		$("#linkCompetitons").append("<div class='table-row data-row'><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div></div>")
	    	    	}
	    	    	$("#linkCompetitons").show()
	    	    	
	    	    	plotPie(linkConsumptions, null, $("#linkCompetitionsPie"), "airlineName", "soldSeats")
	    	    },
	            error: function(jqXHR, textStatus, errorThrown) {
	    	            console.log(JSON.stringify(jqXHR));
	    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    	    }
	    	});
	    	
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	//load history
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/link-consumptions/" + linkId + "?cycleCount=30",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(linkConsumptions) {
	    	if (jQuery.isEmptyObject(linkConsumptions)) {
	    		$("#linkHistoryPrice").text("-")
		    	$("#linkHistoryCapacity").text("-")
		    	$("#linkLoadFactor").text("-")
		    	$("#linkProfit").text("-")
		    	$("#linkRevenue").text("-")
		    	$("#linkFuelCost").text("-")
		    	$("#linkCrewCost").text("-")
		    	$("#linkAirportFees").text("-")
		    	$("#linkDepreciation").text("-")
		    	$("#linkOtherCosts").text("-")
	    	} else {
	    		var linkConsumption = linkConsumptions[0]
	    		$("#linkHistoryPrice").text(toLinkClassValueString(linkConsumption.price, "$"))
		    	$("#linkHistoryCapacity").text(toLinkClassValueString(linkConsumption.capacity))
		    	
		    	var loadFactor = {}
		    	loadFactor.economy = "-"
		    	if (linkConsumption.capacity.economy > 0)  { loadFactor.economy = parseInt(linkConsumption.soldSeats.economy / linkConsumption.capacity.economy * 100)}
	    		loadFactor.business = "-"
			    if (linkConsumption.capacity.business > 0)  { loadFactor.business = parseInt(linkConsumption.soldSeats.business / linkConsumption.capacity.business * 100)}
	    		loadFactor.first = "-"
				if (linkConsumption.capacity.first > 0)  { loadFactor.first = parseInt(linkConsumption.soldSeats.first / linkConsumption.capacity.first * 100)}
		    	
	    		$("#linkLoadFactor").text(toLinkClassValueString(loadFactor, "", "%"))
		    	$("#linkProfit").text("$" + commaSeparateNumber(linkConsumption.profit))
		    	$("#linkRevenue").text("$" + commaSeparateNumber(linkConsumption.revenue))
		    	$("#linkFuelCost").text("$" + commaSeparateNumber(linkConsumption.fuelCost))
		    	$("#linkCrewCost").text("$" + commaSeparateNumber(linkConsumption.crewCost))
		    	$("#linkAirportFees").text("$" + commaSeparateNumber(linkConsumption.airportFees))
		    	$("#linkDepreciation").text("$" + commaSeparateNumber(linkConsumption.depreciation))
		    	$("#linkOtherCosts").text("$" + commaSeparateNumber(linkConsumption.inflightCost + linkConsumption.maintenanceCost))
	    	}
	    	plotLinkProfit(linkConsumptions, $("#linkProfitChart"))
	    	plotLinkRidership(linkConsumptions, $("#linkRidershipChart"))
	    	$("#linkHistoryDetails").show()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	
	
}

function editLink(linkId) {
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/links/" + linkId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(link) {
	    	$("#planLinkFromAirportName").text(link.fromAirportName)
	    	$("#planLinkFromAirportName").append("<img src='assets/images/flags/" + link.fromCountryCode + ".png' />")
	    	$("#planLinkToAirportName").text(link.toAirportName)
	    	$("#planLinkToAirportName").append("<img src='assets/images/flags/" + link.toCountryCode + ".png' />")
	    	$("#planLinkFromAirportId").val(link.fromAirportId)
	    	$("#planLinkToAirportId").val(link.toAirportId)
	    	planLink(link.fromAirportId, link.toAirportId)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function watchLink(linkId) {
	$.ajax({
		type: 'PUT',
		url: "airlines/" + activeAirline.id + "/watched-link?linkId=" + linkId,
		data: JSON.stringify({}),
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(watchedLinkId) {
	    	if (watchedLinkId) {
	    		activeWatchedLink = watchedLinkId
	    	}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function toggleLinkHistory() {
	if (linkHistoryState == "hidden") {
		$.ajax({
			type: 'GET',
			url: "airlines/" + activeAirline.id + "/link-history",
		    contentType: 'application/json; charset=utf-8',
		    dataType: 'json',
		    success: function(linkHistory) {
		    	if (!jQuery.isEmptyObject(linkHistory)) {
		    		$.each(linkHistory.relatedLinks, function(key, relatedLink) {
		    			drawLinkHistoryPath(relatedLink, false, linkHistory.watchedLinkId)
		    		})
		    		$.each(linkHistory.invertedRelatedLinks, function(key, relatedLink) {
		    			drawLinkHistoryPath(relatedLink, true, linkHistory.watchedLinkId)
		    		})
		    		linkHistoryState = "show"
		    		showLinkHistoryPaths(linkHistoryState)
		    	}
		    },
	        error: function(jqXHR, textStatus, errorThrown) {
		            console.log(JSON.stringify(jqXHR));
		            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
		    }
		});
	} else if (linkHistoryState == "show") {
		linkHistoryState = "showInverted"
		showLinkHistoryPaths(linkHistoryState)
	} else if (linkHistoryState == "showInverted") {
		linkHistoryState = "hidden"
		showLinkHistoryPaths(linkHistoryState)
		historyPaths = {}
	} else {
		console.log("unknown linkHistoryState " + linkHistoryState)
	}
}

function drawLinkHistoryPath(link, inverted, watchedLinkId) {
	var from = new google.maps.LatLng({lat: link.fromLatitude, lng: link.fromLongitude})
	var to = new google.maps.LatLng({lat: link.toLatitude, lng: link.toLongitude})
	var pathKey = link.fromAirportId + "|"  + link.toAirportId + "|" + inverted
	
	var lineSymbol = {
	    path: google.maps.SymbolPath.FORWARD_OPEN_ARROW
	};
	
	var isWatchedLink = link.linkId == watchedLinkId
	var relatedPath
	if (!historyPaths[pathKey]) {
		relatedPath = new google.maps.Polyline({
			 geodesic: true,
		     strokeColor: isWatchedLink ? "#FF3BF2" : "#DC83FC",
		     strokeOpacity: 0.8,
		     strokeWeight: 2,
		     path: [from, to],
		     icons: [{
			      icon: lineSymbol,
			      offset: '50%'
			    }],
		     zIndex : 1100,
		     inverted : inverted,
		     thisAirlinePassengers : 0,
		     otherAirlinePassengers : 0,
		     watched : isWatchedLink
		});
		
		var fromAirport
		if (link.fromAirportCity) {
			fromAirport = link.fromAirportCity + "(" + link.fromAirportCode + ")"
		} else {
			fromAirport = link.fromAirportName
		}
		
		var toAirport 
		if (link.toAirportCity) {
			toAirport = link.toAirportCity + "(" + link.toAirportCode + ")"
		} else {
			toAirport = link.toAirportName
		}
		
		shadowPath = new google.maps.Polyline({
			 geodesic: true,
		     strokeColor: "#DC83FC",
		     strokeOpacity: 0.0001,
		     strokeWeight: 25,
		     path: [from, to],
		     zIndex : 401,
		     inverted : inverted,
		     fromAirport : fromAirport,
		     toAirport : toAirport,
		     thisAirlinePassengers : 0,
		     otherAirlinePassengers : 0
		});
		
		relatedPath.shadowPath = shadowPath
		
		var infowindow; 
		shadowPath.addListener('mouseover', function(event) {
			$("#linkHistoryPopupFrom").text(this.fromAirport)
			$("#linkHistoryPopupTo").text(this.toAirport)
			$("#linkHistoryThisAirlinePassengers").text(this.thisAirlinePassengers)
			$("#linkHistoryOtherAirlinePassengers").text(this.otherAirlinePassengers)
			infowindow = new google.maps.InfoWindow({
	             content: $("#linkHistoryPopup").html(),
	             maxWidth : 300});
			
			infowindow.setPosition(event.latLng);
			infowindow.open(map);
		})		
		shadowPath.addListener('mouseout', function(event) {
			infowindow.close()
		})
		
		historyPaths[pathKey] = relatedPath
	} else {
		relatedPath = historyPaths[pathKey]
	}
	
	if (link.airlineId == activeAirline.id) {
		shadowPath.thisAirlinePassengers += link.passenger
	} else {
		shadowPath.otherAirlinePassengers += link.passenger
	}
}

function showLinkHistoryPaths(state) {
	$.each(historyPaths, function(key, historyPath) {
		if ((state == "showInverted" && historyPath.inverted) || (state == "show" && !historyPath.inverted)) {
			var totalPassengers = historyPath.shadowPath.thisAirlinePassengers + historyPath.shadowPath.otherAirlinePassengers
			if (totalPassengers > 1000) {
				historyPath.setOptions({strokeWeight : 3})
			} else if (totalPassengers > 2000) {
				historyPath.setOptions({strokeWeight : 4})
			} else if (totalPassengers < 100) {
				var newOpacity = 0.1 + totalPassengers / 100 * (historyPath.strokeOpacity - 0.1) 
				historyPath.setOptions({strokeOpacity : newOpacity})
			}
			
			historyPath.setMap(map)
			historyPath.shadowPath.setMap(map)
		} else {
			historyPath.setMap(null)
			historyPath.shadowPath.setMap(null)
		}
	})
}


function planFromAirport(fromAirportId) {
	$('#planLinkFromAirportId').val(fromAirportId)
//	$('#planLinkFromAirportName').text(fromAirportName)
	
	if ($('#planLinkFromAirportId').val() && $('#planLinkToAirportId').val()) {
		planLink($('#planLinkFromAirportId').val(), $('#planLinkToAirportId').val())
	}
}

function planToAirport(toAirportId, toAirportName) {
	$('#planLinkToAirportId').val(toAirportId)
	//$('#planLinkToAirportName').text(toAirportName)
	
	if (!$('#planLinkFromAirportId').val()) { //set the HQ by default for now
		$('#planLinkFromAirportId').val(activeAirline.headquarterAirport.airportId)
		//$('#planLinkFromAirportName').text(activeAirline.headquarterAirport.airportName)
	}
	if ($('#planLinkFromAirportId').val() && $('#planLinkToAirportId').val()) {
		planLink($('#planLinkFromAirportId').val(), $('#planLinkToAirportId').val())
	}
}


function planLink(fromAirport, toAirport) {
	var airlineId = activeAirline.id
	
	if (fromAirport && toAirport) {
		var url = "airlines/" + airlineId + "/plan-link"
		$.ajax({
			type: 'POST',
			url: url,
			data: { 'airlineId' : parseInt(airlineId), 'fromAirportId': parseInt(fromAirport), 'toAirportId' : parseInt(toAirport)} ,
//			contentType: 'application/json; charset=utf-8',
			dataType: 'json',
		    success: function(linkInfo) {
		    	updatePlanLinkInfo(linkInfo)
		    },
	        error: function(jqXHR, textStatus, errorThrown) {
		            console.log(JSON.stringify(jqXHR));
		            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
		    }
		});
		//hide existing info
		$("#planLinkDetails div.value").hide()
		setActiveDiv($('#planLinkDetails'))
	}
}

var planLinkInfo = null
var planLinkInfoByModel = {}
var existingLinkModelId = 0

function updatePlanLinkInfo(linkInfo) {
	$('#planLinkFromAirportName').text(getAirportText(linkInfo.fromAirportCity, linkInfo.fromAirportName))
	$('#planLinkFromAirportName').append("<img src='assets/images/flags/" + linkInfo.fromCountryCode + ".png' />")
	
	$('#planLinkToAirportName').text(getAirportText(linkInfo.toAirportCity, linkInfo.toAirportName))
	$('#planLinkToAirportName').append("<img src='assets/images/flags/" + linkInfo.toCountryCode + ".png' />")
	
	$('#planLinkDistance').text(linkInfo.distance)
	$('#planLinkDirectDemand').text(toLinkClassValueString(linkInfo.directDemand))
	$('#planLinkBusinessPassengers').text(linkInfo.businessPassengers)
	$('#planLinkTouristPassengers').text(linkInfo.touristPassengers)
	//$('#planLinkAirportLinkCapacity').text(linkInfo.airportLinkCapacity)
	
	
	$("#planLinkCompetitons .data-row").remove()
	$.each(linkInfo.otherLinks, function(index, linkConsumption) {
		if (linkConsumption.airlineId != activeAirline.id) {
			$("#planLinkCompetitons").append("<div class='table-row data-row'><div style='display: table-cell;'>" + linkConsumption.airlineName
				    	    			   + "</div><div style='display: table-cell;'>" + toLinkClassValueString(linkConsumption.price, "$")
				    	    			   + "</div><div style='display: table-cell;'>" + linkConsumption.capacity 
				    	    			   + "</div><div style='display: table-cell;'>" + linkConsumption.quality + "</div></div>")
		}			
	})
	if ($("#planLinkCompetitons .data-row").length == 0) {
		$("#planLinkCompetitons").append("<div class='table-row data-row'><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div></div>")
	}
	
	
	
	if (!linkInfo.existingLink) {
		$('#planLinkEconomyPrice').val(linkInfo.suggestedPrice.economy)
		$('#planLinkBusinessPrice').val(linkInfo.suggestedPrice.business)
		$('#planLinkFirstPrice').val(linkInfo.suggestedPrice.first)
		$('#addLinkButton').show()
		$('#updateLinkButton').hide()
		
		//unselect the existing path if any
		unselectLink()
		//create a temp path
		var tempLink = {fromLatitude : linkInfo.fromAirportLatitude, fromLongitude : linkInfo.fromAirportLongitude, toLatitude : linkInfo.toAirportLatitude, toLongitude : linkInfo.toAirportLongitude}
		//set the temp path
		tempPath = drawFlightPath(tempLink)
		highlightPath(tempPath)
	} else {
		$('#planLinkEconomyPrice').val(linkInfo.existingLink.price.economy)
		$('#planLinkBusinessPrice').val(linkInfo.existingLink.price.business)
		$('#planLinkFirstPrice').val(linkInfo.existingLink.price.first)
		$('#addLinkButton').hide()
		$('#updateLinkButton').show()
	}
	$("#planLinkModelSelect").find('option').remove()

	planLinkInfo = linkInfo
	planLinkInfoByModel = {}
	existingLinkModelId = 0
	
	$.each(linkInfo.modelPlanLinkInfo, function(key, modelPlanLinkInfo) {
		var modelId = modelPlanLinkInfo.modelId
		var modelname = modelPlanLinkInfo.modelName
		var option = $("<option></option>").attr("value", modelId).text(modelname + " (" + modelPlanLinkInfo.airplanes.length + ")")
		option.appendTo($("#planLinkModelSelect"))
		if (modelPlanLinkInfo.isAssigned) {
			option.prop("selected", true)
			existingLinkModelId = modelId
		}
		
		planLinkInfoByModel[modelId] = modelPlanLinkInfo
	});

	updatePlanLinkInfoWithModelSelected($("#planLinkModelSelect").val())
	$("#planLinkDetails div.value").show()
}

function resetPrice() {
	$('#planLinkEconomyPrice').val(planLinkInfo.suggestedPrice.economy)
	$('#planLinkBusinessPrice').val(planLinkInfo.suggestedPrice.business)
	$('#planLinkFirstPrice').val(planLinkInfo.suggestedPrice.first)
}

function updateFrequencyBar(airplaneModelId, callback) {
	var frequencyBar = $("#frequencyBar")
	var selectedCount
	if ($("#planLinkAirplaneSelect").val()) { 
		selectedCount = $("#planLinkAirplaneSelect").val().length 
	} else { 
		selectedCount = 0 
	}
	var maxFrequencyByAirplanes = planLinkInfoByModel[airplaneModelId].maxFrequency * selectedCount
	var maxFrequencyFromAirport = planLinkInfo.maxFrequencyFromAirport
	var maxFrequencyToAirport = planLinkInfo.maxFrequencyToAirport
	
	if (maxFrequencyFromAirport <= maxFrequencyToAirport && maxFrequencyFromAirport <= maxFrequencyByAirplanes) { //limited by from airport 
		if (maxFrequencyFromAirport == 0) {
			frequencyBar.text("No routing allowed, reason: ")
		} else {
			generateImageBar(frequencyBar.data("emptyIcon"), frequencyBar.data("fillIcon"), maxFrequencyFromAirport, frequencyBar, $("#planLinkFrequency"), null, null, callback)
		}
		$("#planLinkLimitingFactor").html("<h6></h6><br/><br/>").text("Limited by Departure Airport")
	} else if (maxFrequencyToAirport <= maxFrequencyFromAirport && maxFrequencyToAirport <= maxFrequencyByAirplanes) { //limited by to airport 
		if (maxFrequencyToAirport == 0) {
			frequencyBar.text("No routing allowed, reason: ")
		} else {
			generateImageBar(frequencyBar.data("emptyIcon"), frequencyBar.data("fillIcon"), maxFrequencyToAirport, frequencyBar, $("#planLinkFrequency"), null, null, callback)
		}
		$("#planLinkLimitingFactor").html("<h6></h6><br/><br/>").text("Limited by Destination Airport")
	} else { //limited by airplanes
		if (maxFrequencyByAirplanes == 0) {
			frequencyBar.text("No routing allowed, reason: ")
		} else {
			generateImageBar(frequencyBar.data("emptyIcon"), frequencyBar.data("fillIcon"), maxFrequencyByAirplanes, frequencyBar, $("#planLinkFrequency"), null, null, callback)
		}
		$("#planLinkLimitingFactor").html("<h6></h6><br/><br/>").text("Limited by airplanes")
	}
}

function updatePlanLinkInfoWithModelSelected(airplaneModelId) {
	if (airplaneModelId) {
		var existingLink = planLinkInfo.existingLink
		
		var isCurrentlyAssigned = existingLink && existingLinkModelId == airplaneModelId
		var thisModelPlanLinkInfo = planLinkInfoByModel[airplaneModelId]
		
		$('#planLinkAirplaneSelect').find('option').remove()
		
		$.each(thisModelPlanLinkInfo.airplanes, function(key, airplane) {
			var option = $("<option></option>").attr("value", airplane.airplaneId).text("#" + airplane.airplaneId)
			option.appendTo($("#planLinkAirplaneSelect"))
			if (airplane.isAssigned) {
				option.prop("selected", true)
			}
		})
		
		$('#planLinkDuration').text(thisModelPlanLinkInfo.duration)
		
		if (existingLink) {
			$("#planLinkServiceLevel").val(existingLink.rawQuality / 20)
		} else {
			$("#planLinkServiceLevel").val(1)
		}
		
		if (isCurrentlyAssigned) {
			$("#planLinkFrequency").val(existingLink.frequency)
			thisModelPlanLinkInfo.configuration = { "economy" : existingLink.capacity.economy / existingLink.frequency, 
													"business" : existingLink.capacity.business / existingLink.frequency, 
													"first" : existingLink.capacity.first / existingLink.frequency}
		} else {
			$("#planLinkFrequency").val(1)
			$("#planLinkAirplaneSelect").val($("#planLinkAirplaneSelect option:first").val());
			thisModelPlanLinkInfo.configuration = { "economy" : thisModelPlanLinkInfo.capacity, "business" : 0, "first" : 0}
		}
		 
		updateFrequencyBar(airplaneModelId, function(oldFrequency, newFrequency) {
			console.log("frequency from " + oldFrequency + " to " + newFrequency)
			console.log(thisModelPlanLinkInfo.configuration)
		})
		
		plotSeatConfigurationGauge($("#seatConfigurationGauge"), thisModelPlanLinkInfo.configuration, thisModelPlanLinkInfo.capacity)
			
		var serviceLevelBar = $("#serviceLevelBar")
		generateImageBar(serviceLevelBar.data("emptyIcon"), serviceLevelBar.data("fillIcon"), 5, serviceLevelBar, $("#planLinkServiceLevel"))
		$("#planLinkExtendedDetails").show()
	} else {
		$("#planLinkExtendedDetails").hide()
	}
}

function createLink() {
	if ($("#planLinkFromAirportId").val() && $("#planLinkToAirportId").val()) {
		var airlineId = activeAirline.id
		var url = "airlines/" + airlineId + "/links"
	    console.log("selected " + $("#planLinkAirplaneSelect").val())
	    var configuration = planLinkInfoByModel[$("#planLinkModelSelect").val()].configuration
	    var linkData = { 
			"fromAirportId" : parseInt($("#planLinkFromAirportId").val()), 
			"toAirportId" : parseInt($("#planLinkToAirportId").val()),
			"airplanes" : $("#planLinkAirplaneSelect").val().map(Number),
			"airlineId" : airlineId,
			"configuration" : { "economy" : configuration.economy, "business" : configuration.business, "first" : configuration.first},
			"price" : { "economy" : parseInt($("#planLinkEconomyPrice").val()), "business" : parseInt($("#planLinkBusinessPrice").val()), "first" : parseInt($("#planLinkFirstPrice").val())},
			"frequency" : parseInt($("#planLinkFrequency").val()),
			"model" : parseInt($("#planLinkModelSelect").val()),
			"rawQuality" : parseInt($("#planLinkServiceLevel").val()) * 20}
		$.ajax({
			type: 'PUT',
			url: url,
		    data: JSON.stringify(linkData),
		    contentType: 'application/json; charset=utf-8',
		    dataType: 'json',
		    success: function(savedLink) {
		    	if (savedLink.id) {
		    		if (!flightPaths[savedLink.id]) { //new link
		    			//remove temp path
		    			removeTempPath()
		    			//add to link List
		    			insertLinkToList(savedLink)
		    			//draw flight path
		    			var newPath = drawFlightPath(savedLink)
		    			selectLink(savedLink.id, false)
		    		}
		    		refreshPanels(activeAirline.id)
		    		setActiveDiv($('#linkDetails'))
		    	}
		    },
	        error: function(jqXHR, textStatus, errorThrown) {
		            console.log(JSON.stringify(jqXHR));
		            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
		    }
		});
	}
}

function deleteLink(linkId) {
	$.ajax({
		type: 'DELETE',
		url: "airlines/" + activeAirline.id + "/links/" + linkId,
	    success: function() {
	    	$("#linkDetails").fadeOut(200)
	    	updateLinksInfo()
	    	updateAirplaneList() //refresh all airplane list for now
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function cancelPlanLink() {
	//remove the temp path
	if (tempPath) { //create new link
		removeTempPath()
		$('#planLinkDetails').hide()
	} else { //simply go back to linkDetails
		setActiveDiv($('#linkDetails'))
	}
}

function removeTempPath() {
	unhighlightPath(tempPath)
	clearPathEntry(tempPath)
	tempPath = undefined
}


function showVipRoutes() {
	map.setZoom(2)
	map.setCenter({lat: 20, lng: 150.644})
   	
	$.ajax({
		type: 'GET',
		url: "vip-routes",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(routes) {
	    	var routePaths = []
	    	$.each(routes, function(key1, route) { 
	    		var paths = []
	    		$.each(route, function(key2, link) { //create paths for each route
	    			var from = new google.maps.LatLng({lat: link.fromLatitude, lng: link.fromLongitude})
	    			var to = new google.maps.LatLng({lat: link.toLatitude, lng: link.toLongitude})
	    			var vipPath = new google.maps.Polyline({
	    				 geodesic: true,
	    			     strokeColor: "#DC83FC",
	    			     strokeOpacity: 0.6,
	    			     strokeWeight: 2,
	    			     from : from,
	    			     to : to,
	    			     zIndex : 500,
	    			     distance : google.maps.geometry.spherical.computeDistanceBetween(from, to) / 1000
	    			});
	    			paths.push(vipPath)
	    		})
	    		routePaths.push(paths)
	    	})
	    	
	    	animateVipRoutes(routePaths, 0, 0, 0, null)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function animateVipRoutes(routePaths, currentRouteIndex, currentPathIndex, currentDistance, vipMarker) {
	var route = routePaths[currentRouteIndex]
	var path = route[currentPathIndex]
	if (currentDistance >= path.distance) {
		currentPathIndex ++
		if (currentPathIndex == route.length) { // all done with this route
			animateArrival(vipMarker, true, 4) //hoooray! hop hop hop
			setTimeout(function(removingRoute, done) {
				$.each(removingRoute, function(key, path) {
					path.setMap(null)
				})
				fadeOutMarker(vipMarker)
				if (!done) {
					animateVipRoutes(routePaths, currentRouteIndex, currentPathIndex, 0, null)
				}
			}, 4000, routePaths[currentRouteIndex], currentRouteIndex + 1 == routePaths.length)
			
			currentPathIndex = 0 //reset path index for next route
			currentRouteIndex++
		} else {
			animateArrival(vipMarker, false, 4) //connnection meh
			setTimeout(function() {
				vipMarker.setAnimation(null)
				animateVipRoutes(routePaths, currentRouteIndex, currentPathIndex, 0, vipMarker) 
			}, 4000)
		}
	} else {
		var from = path.from
		var to = path.to
		var newPosition = google.maps.geometry.spherical.interpolate(from, to, currentDistance / path.distance)
		var newPath = path.getPath()
		newPath.removeAt(1) //remove last to
		newPath.push(newPosition) 
		path.setPath(newPath)
		
		//add path and marker on first frame
		if (currentDistance == 0) {
			path.setMap(map)
		}
		if (vipMarker == null) {
			var image = {
	    	        url: "assets/images/icons/star-24.png",
	    	        origin: new google.maps.Point(0, 0),
	    	        anchor: new google.maps.Point(12, 12),
	    	    };
	    	vipMarker = new google.maps.Marker({
	    		map : map,
	    		icon : image, 
			    clickable: false,
			    zIndex: 1100
			});
		}
		vipMarker.setPosition(newPosition)
		setTimeout(function() { animateVipRoutes(routePaths, currentRouteIndex, currentPathIndex, currentDistance + 50, vipMarker) }, 20)
	}
}

function animateArrival(vipMarker, bounce, influencePointCount) {
	if (bounce) {
		vipMarker.setAnimation(google.maps.Animation.BOUNCE)
	}
	
	var iconDistance = 10
	var anchorXShift = 8 + ((influencePointCount - 1) / 2) * iconDistance //icon center + biggest shift	
	//drop some color wheels!
	for (i = 0 ; i < influencePointCount; i++) {
		setTimeout( function (index) {
			var anchorX = anchorXShift - index * iconDistance
			var image = {
	    	        url: "assets/images/icons/color--plus.png",
	    	        origin: new google.maps.Point(0, 0),
					anchor: new google.maps.Point(anchorX, 30),
	    	    }; 
			colorMarker = new google.maps.Marker({
	    		icon : image, 
	    		position : vipMarker.getPosition(),
			    clickable: false,
			    map : map,
			    opacity: 0,
			    zIndex: 1000 + i,
			})
			fadeInMarker(colorMarker)
			setTimeout( function(marker) {
				fadeOutMarker(marker)
			}, 3000, colorMarker)
		}, (i + 1) * 200, i)
	}
}

function getAirportText(city, airportName) {
	if (city) {
		return city + "(" + airportName + ")"
	} else {
		return airportName
	}
}

function updateServiceFunding() {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/serviceFunding"
    var data = { "serviceFunding" : parseInt($("#serviceFunding").val()) }
	$.ajax({
		type: 'PUT',
		url: url,
	    data: JSON.stringify(data),
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function() {
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateMaintenanceQuality() {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/maintenanceQuality"
    var data = { "maintenanceQuality" : parseInt($("#maintenanceQuality").val()) }
	$.ajax({
		type: 'PUT',
		url: url,
	    data: JSON.stringify(data),
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function() {
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

	
//TEST METHODS

function removeAllLinks() {
	$.ajax({
		type: 'DELETE',
		url: "links",
	    success: function() {
	    	updateLinksInfo()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


	
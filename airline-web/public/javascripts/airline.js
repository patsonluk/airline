var flightPaths = {} //key: link id, value : { path, shadow }
var flightMarkers = {} //key: link id, value: { markers : array[], animation}

var tempPath //temp path for new link creation
var loadedLinks = []
var loadedLinksById = {}
var currentAnimationStatus = true
var currentAirlineAllianceMembers = []

$( document ).ready(function() {
    $('#linkEventModal .filterCheckboxes input:checkbox').change(function() {
        $("#linkEventModal .linkEventHistoryTable .table-row").hide() //hide all first
        $('#linkEventModal .filterCheckboxes input:checkbox').each(function() { //have to iterate them, as they are not mutually exclusive...
           var filterType = $(this).data('filter')
           if ($(this).prop('checked')) {
               $("#linkEventModal .linkEventHistoryTable .table-row.filter-" + filterType).show()
           }
        });
    })
})

function updateAirlineInfo(airlineId) {
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    async: false,
	    success: function(airline) {
	    	refreshTopBar(airline)
	    	$(".currentAirline").html(getAirlineLogoImg(airline.id) + airline.name)

	    	if (airline.headquarterAirport) {
                        $("#currentAirlineCountry").html("<img class='flag' src='assets/images/flags/" + airline.headquarterAirport.countryCode + ".png' />")
	    	} else {
                        $("#currentAirlineCountry").empty()
	    	}
	    	activeAirline = airline
	    	updateLinksInfo()
	    	updateAirportMarkers(airline)
	    	updateAirlineLogo()
	    	updateLogoUpload()
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateAirlineLogo() {
	$('.airlineLogo').attr('src', '/airlines/' + activeAirline.id + "/logo?dummy=" + Math.random())
}
	
	
function refreshTopBar(airline) {
    changeColoredElementValue($(".balance"), airline.balance)
	//changeColoredElementValue($(".reputation"), airline.reputation)
	$(".reputationValue").text(airline.reputation)
	$(".reputationStars").empty()

	//mobile
	$(".reputation").text(airline.reputation)
	$(".reputationLevel").text(" " + airline.gradeDescription + " (Next Grade: " + airlineGradeLookup[airline.gradeValue] + ")")

	//desktop
	//$(getGradeStarsImgs(airline.gradeValue)).attr('title', "Reputation: " + airline.reputation).appendTo($(".reputationStars"))
	var reputationText = "Reputation: " + airline.reputation + " (" + airline.gradeDescription + ") Next Grade: " + airlineGradeLookup[airline.gradeValue]
	var $starBar = $(getGradeStarsImgs(airline.gradeValue))
	$(".reputationStars").append($starBar)
	addTooltip($(".reputationStars"), reputationText, {'top' : 0, 'width' : '350px', 'white-space' : 'nowrap'})

	//updateTopBarDelegatesStatus
	refreshTopBarDelegates(airline)
}

function getGradeStarsImgs(gradeValue) {
	var fullStars = Math.floor(gradeValue / 2)
	var halfStar = gradeValue % 2
	var html = ""
	for (i = 0 ; i < fullStars; i ++) {
		html += "<img src='assets/images/icons/star.png'/>"
	}
	if (halfStar) {
		html += "<img src='assets/images/icons/star-half.png'/>"
	}
	for (i = 0 ; i < 5 - fullStars - halfStar; i ++) {
		html += "<img src='assets/images/icons/star-empty.png'/>"
	}
	return html
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

function selectHeadquarters(airportId) {
    if (!activeAirline.headquarterAirport) {
        if (!activeAirline.initialized) {
            $.ajax({
                    type: 'GET',
                    url: "airlines/" + activeAirline.id + "/profiles?airportId=" + airportId ,
                    contentType: 'application/json; charset=utf-8',
                    dataType: 'json',
                    success: function(profiles) {
                        updateProfiles(profiles)
                    },
                    error: function(jqXHR, textStatus, errorThrown) {
                            console.log(JSON.stringify(jqXHR));
                            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
                    }
                });
        } else {

            promptConfirm("Build your headquarters at this airport?", data => buildBase(true))
        }
    }
}

function buildBase(isHeadquarter, scale) {
	scale = scale || 1
	var url = "airlines/" + activeAirline.id + "/bases/" + activeAirportId 
	var baseData = { 
			"airportId" : parseInt(activeAirportId),
			"airlineId" : activeAirline.id,
			"scale" : scale,
			"headquarter" : isHeadquarter}
	$.ajax({
		type: 'PUT',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    data: JSON.stringify(baseData),
	    dataType: 'json',
	    success: function() {
            updateAllPanels(activeAirline.id)
	    	showWorldMap()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function deleteBase() {
	var url = "airlines/" + activeAirline.id + "/bases/" + activeAirportId 
	
	$.ajax({
		type: 'DELETE',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function() {
	    	updateAllPanels(activeAirline.id)
	    	showWorldMap()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function downgradeBase() {
	var url = "airlines/" + activeAirline.id + "/downgradeBase/" + activeAirportId 
	
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    success: function() {
	    	updateAllPanels(activeAirline.id)
	    	showWorldMap()
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

function clearAllPaths() {
	$.each(flightMarkers, function( linkId, markerEntry ) {
		clearMarkerEntry(markerEntry)
	});
	//remove all links from UI first
	$.each(flightPaths, function( key, pathEntry ) {
		clearPathEntry(pathEntry)
	})
	
	flightPaths = {}
	flightMarkers = {}
	
	$.each(polylines, function(index, polyline) {
		if (polyline.getMap() != null) {
			polyline.setMap(null)
		}
	})
	
	polylines = polylines.filter(function(polyline) { 
	    return polyline.getMap() != null
	})
}

//remove and re-add all the links
function updateLinksInfo() {
	clearAllPaths()

	if (activeAirline) {
		var url = "airlines/" + activeAirline.id + "/links-details"
		
		$.ajax({
			type: 'GET',
			url: url,
		    contentType: 'application/json; charset=utf-8',
		    dataType: 'json',
		    async: false,
		    success: function(links) {
		    	$.each(links, function( key, link ) {
		    		drawFlightPath(link)
		  		});
		    	updateLoadedLinks(links);
		    },
	        error: function(jqXHR, textStatus, errorThrown) {
		            console.log(JSON.stringify(jqXHR));
		            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
		    }
		});
	}
}

//refresh links without removal/addition
function refreshLinks(forceRedraw) {
	var url = "airlines/" + activeAirline.id + "/links-details"
	
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(links) {
	    	$.each(links, function( key, link ) {
	    		refreshFlightPath(link, forceRedraw)
	  		});
	    	updateLoadedLinks(links);
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}



function drawFlightPath(link, linkColor) {
	
   if (!linkColor) {
	   linkColor = getLinkColor(link.profit, link.revenue) 
   }
   var flightPath = new google.maps.Polyline({
     path: [{lat: link.fromLatitude, lng: link.fromLongitude}, {lat: link.toLatitude, lng: link.toLongitude}],
     geodesic: true,
     strokeColor: linkColor,
     strokeOpacity: pathOpacityByStyle[currentStyles].normal,
     strokeWeight: 2,
     frequency : link.frequency,
     modelId : link.modelId,
     link : link,
     zIndex: 90
   });
   
   var icon = "assets/images/icons/airplane.png"
   
   flightPath.setMap(map)
   polylines.push(flightPath)
   
   var shadowPath = new google.maps.Polyline({
	     path: [{lat: link.fromLatitude, lng: link.fromLongitude}, {lat: link.toLatitude, lng: link.toLongitude}],
	     geodesic: true,
	     map: map,
	     strokeColor: getLinkColor(link.profit, link.revenue),
	     strokeOpacity: 0.001,
	     strokeWeight: 15,
	     zIndex: 100
	   });
   
   var resultPath = { path : flightPath, shadow : shadowPath }
   if (link.id) {
	  shadowPath.addListener('click', function() {
	   		selectLinkFromMap(link.id, false)
	  });
      drawFlightMarker(flightPath, link);
	  flightPaths[link.id] = resultPath 
   }
   
   return resultPath
}

function refreshFlightPath(link, forceRedraw) {
	if (flightPaths[link.id]) {
		var path = flightPaths[link.id].path
		if (forceRedraw || path.frequency != link.frequency || path.modelId != link.modelId) { //require marker change
			path.frequency = link.frequency
			path.modelId = link.modelId
			
			drawFlightMarker(path, link)
		} 
		path.setOptions({ strokeColor : getLinkColor(link.profit, link.revenue), strokeOpacity : pathOpacityByStyle[currentStyles].normal })
	
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
	   if (currentStyles === "light") {
	      redHex -= 50
	      greenHex -= 50
	   }
	   if (redHex < 0) redHex = 0
	   if (greenHex < 0) greenHex = 0

	   
	   var redHexString = parseInt(redHex).toString(16)
	   if (redHexString.length == 1) { redHexString = "0" + redHexString }
	   var greenHexString = parseInt(greenHex).toString(16)
	   if (greenHexString.length == 1) { greenHexString = "0" + greenHexString }
	   return colorHex = "#" + redHexString + greenHexString + "20"
   } else  { //no history yet
	   return "#DCDC20"
   }
}

function highlightPath(path, refocus) {
	refocus = refocus || false
	//focus to the from airport
	if (refocus) {
		map.setCenter(path.getPath().getAt(0))
	}

	
	if (!path.highlighted) { //only highlight again if it's not already done so
	    var originalColorString = path.strokeColor
		//keep track of original values so we can revert...shouldn't there be a better way to just get all options all at once?
		path.originalColor = originalColorString
		path.originalStrokeWeight = path.strokeWeight
		path.originalZIndex = path.zIndex
		path.originalStrokeOpacity = path.strokeOpacity

		path.setOptions({ strokeOpacity : pathOpacityByStyle[currentStyles].highlight })
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
			path.setOptions({ strokeColor : colorHexString , strokeWeight : 4, zIndex : 91})
			
			currentFrame = (currentFrame + 1) % (totalFrames * 2)
			
		}, 50)
		path.animation = animation
		
		path.highlighted = true
	}		
	
}
function unhighlightPath(path) {
	window.clearInterval(path.animation)
	path["animation"] = undefined
	path.setOptions({ strokeColor : path.originalColor , strokeWeight : path.originalStrokeWeight, zIndex : path.originalZIndex, strokeOpacity : path.originalStrokeOpacity})
	
	delete path.highlighted
}

function toggleMapAnimation() {
	if (currentAnimationStatus) {
		currentAnimationStatus = false
	} else {
		currentAnimationStatus = true
	}
	refreshLinks(true)	
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
	
	if (currentAnimationStatus && link.assignedAirplanes && link.assignedAirplanes.length > 0) {
		var from = line.getPath().getAt(0)
		var to = line.getPath().getAt(1)
		var image = {
	        url: "assets/images/markers/dot.png",
	        origin: new google.maps.Point(0, 0),
	        anchor: new google.maps.Point(6, 6),
	    };



		var frequency = link.frequency
//		var airplaneCount = link.assignedAirplanes.length
//		var frequencyByAirplane = {}
//		$.each(link.assignedAirplanes, function(key, airplane) {
//			frequencyByAirplane[key] = Math.floor(frequency / airplaneCount)
//		})
//		for (i = 0; i < frequency % airplaneCount; i++) { //assign the remainder
//			frequencyByAirplane[i] = frequencyByAirplane[i] + 1
//		}
        var animationInterval = 100
        var minsPerInterval = 1
        var minutesPerWeek = 60 * 24 * 7
        var maxTripsPerMarker = (60 * 24 * 7) / (link.duration * 2) //how many round trips can a marker make in a week, assuming a marker go back and forth right the way
        var markersRequired = Math.ceil(frequency / maxTripsPerMarker)
        var totalIntervalsPerWeek = minutesPerWeek / minsPerInterval //min in a week, assume each interval is 1 mins

		var markersOfThisLink = []
		for (i = 0; i < markersRequired; i ++) {
			var marker = new google.maps.Marker({
			    position: from,
			    icon : image, 
			    totalDuration : link.duration * 2, //round trip
			    elapsedDuration : 0,
			    nextDepartureFrame : Math.floor((i + 0.1) * totalIntervalsPerWeek / frequency) % totalIntervalsPerWeek, //i + 0.1 so they wont all depart at the same time
				departureInterval : Math.floor(totalIntervalsPerWeek / markersRequired),
				status : "forward",
			    isActive: false,
			    clickable: false,
			});
			
			//flightMarkers.push(marker)
			markersOfThisLink.push(marker)
		}
		
		flightMarkers[linkId] = {} //initialize
		flightMarkers[linkId].markers = markersOfThisLink
		
		var count = 0;
		var animation = window.setInterval(function() {
			$.each(markersOfThisLink, function(key, marker) { 
				if (count == marker.nextDepartureFrame) {
					if (christmasMarker) {
						marker.icon = {
						        url: randomFlightMarker(),
						        origin: new google.maps.Point(0, 0),
						        anchor: new google.maps.Point(6, 6),
						    };
					}
					marker.status = "forward"
					marker.isActive = true
					marker.elapsedDuration = 0
					marker.setPosition(from)
					marker.setMap(map)
				} else if (marker.isActive) {
					marker.elapsedDuration += minsPerInterval
					
					if (marker.elapsedDuration >= marker.totalDuration) { //finished a round trip
						//marker.setMap(null)
						fadeOutMarker(marker, animationInterval)
						marker.isActive = false
						marker.nextDepartureFrame = (marker.nextDepartureFrame + marker.departureInterval) % totalIntervalsPerWeek
						//console.log("next departure " + marker.nextDepartureFrame)
					} else {
					    if (marker.status === "forward") {
					         if (marker.elapsedDuration / marker.totalDuration >= 0.45) { //finished forward, now go into turnaround
                                marker.status = "turnaround"
					         } else {
					            var newPosition = google.maps.geometry.spherical.interpolate(from, to, marker.elapsedDuration / marker.totalDuration / 0.45)
                                marker.setPosition(newPosition)
                             }
                        }
                        if (marker.status === "turnaround") {
                             if (marker.elapsedDuration / marker.totalDuration >= 0.55) { //finished turnaround, now go into backward
                                marker.status = "backward"
                             }
                        }
                        if (marker.status === "backward") {
                            var newPosition = google.maps.geometry.spherical.interpolate(to, from, (marker.elapsedDuration / marker.totalDuration - 0.55) / 0.45)
                            marker.setPosition(newPosition)
                        }

					}
				}
			})
			count = (count + 1) % totalIntervalsPerWeek;
		}, animationInterval)
		
		flightMarkers[linkId].animation = animation;
	}
}


/**
 * deselect a currently selected link, perform both UI and underlying data changes
 * @returns
 */
function deselectLink() {
	if (selectedLink) {
		unhighlightLink(selectedLink)
		selectedLink = undefined
	}
	removeTempPath()
	$("#sidePanel").fadeOut(200)
}

/**
 * Perform UI changes for unhighlighting currently highlighted link
 * @param linkId
 * @returns
 */
function unhighlightLink() {
	$.each(flightPaths, function(linkId, path) {
		if (path.path.highlighted) {
			unhighlightPath(path.path)
		}
	})
		
}

/**
 * Performs UI changes to highlight a link
 */
function highlightLink(linkId, refocus) {
	if (tempPath) {
		removeTempPath(tempPath)
	}

	//highlight the selected link's flight path
	highlightPath(flightPaths[linkId].path, refocus)
	
	//highlight the corresponding list item
//	var selectedListItem = $("#linkList a[data-link-id='" + linkId + "']")
//	selectedListItem.addClass("selected")
}

function refreshLinkDetails(linkId) {
	var airlineId = activeAirline.id
	
	$("#linkCompetitons .data-row").remove()
	$("#actionLinkId").val(linkId)
	
	//load link
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/links/" + linkId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(link) {
	    	$("#linkFromAirport").attr("onclick", "showAirportDetails(" + link.fromAirportId + ")").html(getCountryFlagImg(link.fromCountryCode) + getAirportText(link.fromAirportCity, link.fromAirportCode))
	    	//$("#linkFromAirportExpectedQuality").attr("onclick", "loadLinkExpectedQuality(" + link.fromAirportId + "," + link.toAirportId + "," + link.fromAirportId + ")")
	    	$("#linkToAirport").attr("onclick", "showAirportDetails(" + link.toAirportId + ")").html(getCountryFlagImg(link.toCountryCode) + getAirportText(link.toAirportCity, link.toAirportCode))
	    	//$("#linkToAirportExpectedQuality").attr("onclick", "loadLinkExpectedQuality(" + link.fromAirportId + "," + link.toAirportId + "," + link.toAirportId + ")")
	    	$("#linkFlightCode").text(link.flightCode)
	    	if (link.assignedAirplanes && link.assignedAirplanes.length > 0) {
	    		$('#linkAirplaneModel').text(link.assignedAirplanes[0].airplane.name + "(" + link.assignedAirplanes.length + ")")
	    	} else {
	    		$('#linkAirplaneModel').text("-")
	    	}
	    	$("#linkCurrentPrice").text(toLinkClassValueString(link.price, "$"))
	    	$("#linkDistance").text(link.distance + " km (" + link.flightType + ")")
	    	$("#linkQuality").html(getGradeStarsImgs(Math.round(link.computedQuality / 10)) + link.computedQuality)
	    	$("#linkCurrentCapacity").text(toLinkClassValueString(link.capacity))
	    	if (link.future) {
	    	    $("#linkCurrentDetails .future .capacity").text(toLinkClassValueString(link.future.capacity))
	    	    $("#linkCurrentDetails .future").show()
	    	} else {
	    	    $("#linkCurrentDetails .future").hide()
	    	}
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
	    	    	$("#linkCompetitons .data-row").remove()
	    	    	$.each(linkConsumptions, function(index, linkConsumption) {
    	    			var row = $("<div class='table-row data-row clickable' onclick='showRivalsCanvas(" + linkConsumption.airlineId + ")' data-link='rival'><div style='display: table-cell;'>" + linkConsumption.airlineName
                                  		    	    				+ "</div><div style='display: table-cell;'>" + toLinkClassValueString(linkConsumption.price, "$")
                                  		    	    				+ "</div><div style='display: table-cell; text-align: right;'>" + toLinkClassValueString(linkConsumption.capacity)
                                  		    	    				+ "</div><div style='display: table-cell; text-align: right;'>" + linkConsumption.quality
                                  		    	    				+ "</div><div style='display: table-cell; text-align: right;'>" + linkConsumption.frequency + "</div></div>")
                        if (linkConsumption.airlineId == airlineId) {
                            $("#linkCompetitons .table-header").after(row) //self is always on top
                        } else {
                            $("#linkCompetitons").append(row)
                        }

	    	    	})

	    	    	populateNavigation($('#linkCompetitons'))
	    	    	if ($("#linkCompetitons .data-row").length == 0) {
	    	    		$("#linkCompetitons").append("<div class='table-row data-row'><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div></div>")
	    	    	}
	    	    	$("#linkCompetitons").show()
	    	    	
	    	    	assignAirlineColors(linkConsumptions, "airlineId")
	    	    	plotPie(linkConsumptions, null, $("#linkCompetitionsPie"), "airlineName", "soldSeats")
	    	    },
	            error: function(jqXHR, textStatus, errorThrown) {
	    	            console.log(JSON.stringify(jqXHR));
	    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    	    }
	    	});

	    	$('#linkEventModal').data('link', link)
	    	
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});

    var plotUnit = $("#linkDetails #switchMonth").is(':checked') ? plotUnitEnum.MONTH : plotUnitEnum.QUARTER
	var cycleCount = plotUnit.maxWeek

	//load history
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/link-consumptions/" + linkId + "?cycleCount=" + cycleCount,
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
		    	$("#linkCompensation").text("-")
		    	$("#linkLoungeCost").text("-")
		    	$("#linkServiceSupplies").text("-")
		    	$("#linkMaintenance").text("-")
		    	$("#linkOtherCosts").text("-")
		    	$("#linkDelays").text("-")
		    	$("#linkCancellations").text("-")

		    	disableButton($("#linkDetails .button.viewLinkHistory"), "Passenger Map is not yet available for this route - please wait for the simulation (time estimation on top left of the screen).")
		    	disableButton($("#linkDetails .button.viewLinkComposition"), "Passenger Survey is not yet available for this route - please wait for the simulation (time estimation on top left of the screen).")
		    	disableButton($("#linkDetails .button.viewLinkEvent"), "Event history is not yet available for this route - please wait for the simulation (time estimation on top left of the screen).")
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
		    	$("#linkCompensation").text("$" + commaSeparateNumber(linkConsumption.delayCompensation))
		    	$("#linkLoungeCost").text("$" + commaSeparateNumber(linkConsumption.loungeCost))
		    	$("#linkServiceSupplies").text("$" + commaSeparateNumber(linkConsumption.inflightCost))
		    	$("#linkMaintenance").text("$" + commaSeparateNumber(linkConsumption.maintenanceCost))
		    	if (linkConsumption.minorDelayCount == 0 && linkConsumption.majorDelayCount == 0) {
		    		$("#linkDelays").removeClass("warning")
		    		$("#linkDelays").text("-")
		    	} else {
		    		$("#linkDelays").addClass("warning")
		    		$("#linkDelays").text(linkConsumption.minorDelayCount + " minor " + linkConsumption.majorDelayCount + " major")
		    	}
	    		
	    		if (linkConsumption.cancellationCount == 0) {
		    		$("#linkCancellations").removeClass("warning")
		    		$("#linkCancellations").text("-")
		    	} else {
		    		$("#linkCancellations").addClass("warning")
		    		$("#linkCancellations").text(linkConsumption.cancellationCount)
		    	}
		    	enableButton($("#linkDetails .button.viewLinkHistory"))
		    	enableButton($("#linkDetails .button.viewLinkComposition"))
		    	enableButton($("#linkDetails .button.viewLinkEvent"))
	    	}
            plotLinkCharts(linkConsumptions, plotUnit)
            $('#linkEventChart').data('linkConsumptions', linkConsumptions)
	    	$("#linkHistoryDetails").show()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	setActiveDiv($("#linkDetails"))
	hideActiveDiv($("#extendedPanel #airplaneModelDetails"))
	$('#sidePanel').fadeIn(200);
}

function plotLinkCharts(linkConsumptions, plotUnit) {
    plotLinkProfit(linkConsumptions, $("#linkProfitChart"), plotUnit)
	plotLinkConsumption(linkConsumptions, $("#linkRidershipChart"), $("#linkRevenueChart"), $("#linkPriceChart"), plotUnit)
}

function refreshLinkCharts() {
    var plotUnit = $("#linkDetails #switchMonth").is(':checked') ? plotUnitEnum.MONTH : plotUnitEnum.QUARTER
    var cycleCount = plotUnit.maxWeek
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/link-consumptions/" + $("#actionLinkId").val() + "?cycleCount=" + cycleCount,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(linkConsumptions) {
	        plotLinkCharts(linkConsumptions, plotUnit)
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
	    	planLink(link.fromAirportId, link.toAirportId)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function fadeOutMarker(marker, animationInterval) {
    var opacity = 1.0
    var animation = window.setInterval(function () {
        if (opacity <= 0) {
            marker.setMap(null)
            marker.setOpacity(1)
            window.clearInterval(animation)
        } else {
            marker.setOpacity(opacity)
            opacity -= 0.1
        }
    }, animationInterval)
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

function planLink(fromAirport, toAirport, isRefresh) {
    checkTutorial("planLink")
	var airlineId = activeAirline.id

	$("#planLinkFromAirportId").val(fromAirport)
	$("#planLinkToAirportId").val(toAirport)
	
	if (fromAirport && toAirport) {
		setActiveDiv($('#planLinkDetails'))
		$('#planLinkDetails .warning').hide()

		var loadPlanLink = function() {
            var url = "airlines/" + airlineId + "/plan-link"
            $.ajax({
                type: 'POST',
                url: url,
                data: { 'airlineId' : parseInt(airlineId), 'fromAirportId': parseInt(fromAirport), 'toAirportId' : parseInt(toAirport)} ,
    //			contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                success: function(linkInfo) {
                    updatePlanLinkInfo(linkInfo, isRefresh)
                    if (!isRefresh) {
                        $('#sidePanel').fadeIn(200);
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
            //hide existing info
            //$("#planLinkDetails div.value").hide()
        }

        if (!isRefresh) {
            $('#sidePanel').fadeOut(200, loadPlanLink)
        } else {
            loadPlanLink()
        }
	}
	
	
}

var planLinkInfo = null
var planLinkInfoByModel = {}
var spaceMultipliers = null
var existingLink
//var existingLinkModelId = 0

function updatePlanLinkInfo(linkInfo, isRefresh) {
	$('#planLinkFromAirportName').attr("onclick", "showAirportDetails(" + linkInfo.fromAirportId + ")").html(getCountryFlagImg(linkInfo.fromCountryCode) + getAirportText(linkInfo.fromAirportCity, linkInfo.fromAirportCode))
	if (activeAirline.baseAirports.length > 1) { //only allow changing from airport if this is a new link and there are more than 1 base
		$('#planLinkFromAirportEditIcon').show()
		//fill the from list
		$('#planLinkFromAirportSelect').empty()
		$.each(activeAirline.baseAirports, function(index, base) {
			var airportId = base.airportId
			var cityName = base.city
			var airportCode = base.airportCode
			var option = $("<option></option>").attr("value", airportId).text(getAirportText(cityName, airportCode))
			
			if ($('#planLinkFromAirportId').val() == airportId) {
				option.prop("selected", true)
			}
			option.appendTo($("#planLinkFromAirportSelect"))
		});
	} else {
		$('#planLinkFromAirportEditIcon').hide()
	}
	$("#planLinkFromAirportSelect").hide() //do not show the list yet
	//$('#planLinkFromAirportExpectedQuality').attr("onclick", "loadLinkExpectedQuality(" + linkInfo.fromAirportId + "," + linkInfo.toAirportId + "," + linkInfo.fromAirportId + ")")
	
	$('#planLinkToAirportName').attr("onclick", "showAirportDetails(" + linkInfo.toAirportId + ")").html(getCountryFlagImg(linkInfo.toCountryCode) + getAirportText(linkInfo.toAirportCity, linkInfo.toAirportCode))
	//$('#planLinkToAirportExpectedQuality').attr("onclick", "loadLinkExpectedQuality(" + linkInfo.fromAirportId + "," + linkInfo.toAirportId + "," + linkInfo.toAirportId + ")")
	$('#planLinkFlightCode').text(linkInfo.flightCode)
	$('#planLinkMutualRelationship').html(getCountryFlagImg(linkInfo.fromCountryCode) + "&nbsp;vs&nbsp;" + getCountryFlagImg(linkInfo.toCountryCode) + getCountryRelationshipDescription(linkInfo.mutualRelationship))

	var relationship = linkInfo.toCountryRelationship
    var relationshipSpan = getAirlineRelationshipDescriptionSpan(relationship.total)
    $("#planLinkToCountryRelationship .total").html(relationshipSpan)

    var $relationshipDetailsIcon = $("#planLinkToCountryRelationship .detailsIcon")
    $relationshipDetailsIcon.data("relationship", relationship)
    $relationshipDetailsIcon.data("title", linkInfo.toCountryTitle)
    $relationshipDetailsIcon.data("countryCode", linkInfo.toCountryCode)
    $relationshipDetailsIcon.show()

    var title = linkInfo.toCountryTitle
    updateAirlineTitle(title, $("#planLinkToCountryTitle img.airlineTitleIcon"), $("#planLinkToCountryTitle .airlineTitle"))

	$('#planLinkDistance').text(linkInfo.distance + " km (" + linkInfo.flightType + ')')
	$('#planLinkDirectDemand').text(toLinkClassValueString(linkInfo.directDemand))

    var $breakdown = $("#planLinkDetails .directDemandBreakdown")
    $breakdown.find(".fromAirport .airportLabel").empty()
    $breakdown.find(".fromAirport .airportLabel").append(getAirportSpan({ "iata" : linkInfo.fromAirportCode, "countryCode" : linkInfo.fromCountryCode, "city" : linkInfo.fromAirportCity}))
    $breakdown.find(".fromAirport .businessDemand").text(toLinkClassValueString(linkInfo.fromAirportBusinessDemand))
    $breakdown.find(".fromAirport .touristDemand").text(toLinkClassValueString(linkInfo.fromAirportTouristDemand))

    $breakdown.find(".toAirport .airportLabel").empty()
    $breakdown.find(".toAirport .airportLabel").append(getAirportSpan({ "iata" : linkInfo.toAirportCode, "countryCode" : linkInfo.toCountryCode, "city" : linkInfo.toAirportCity}))
    $breakdown.find(".toAirport .businessDemand").text(toLinkClassValueString(linkInfo.toAirportBusinessDemand))
    $breakdown.find(".toAirport .touristDemand").text(toLinkClassValueString(linkInfo.toAirportTouristDemand))

	 //+ " (business: " + linkInfo.businessPassengers + " tourist: " + linkInfo.touristPassengers + ")")
	//$('#planLinkAirportLinkCapacity').text(linkInfo.airportLinkCapacity)
	
	
	$("#planLinkCompetitors .data-row").remove()

	linkInfo.otherLinks.sort(function(a, b) {
        return b.capacity.total - a.capacity.total;
    });
	$.each(linkInfo.otherLinks, function(index, linkConsumption) {
		if (linkConsumption.airlineId != activeAirline.id) {
		    let loadFactorPercentage = Math.round(linkConsumption.soldSeats * 100 / linkConsumption.capacity.total)
			$("#planLinkCompetitors").append("<div class='table-row data-row'><div style='display: table-cell;'>" + getAirlineSpan(linkConsumption.airlineId, linkConsumption.airlineName)
				    	    			   + "</div><div style='display: table-cell;'>" + toLinkClassValueString(linkConsumption.price, "$")
				    	    			   + "</div><div style='display: table-cell; text-align:right;'>" + toLinkClassValueString(linkConsumption.capacity) + " (" + linkConsumption.frequency + ")"
				    	    			   + "</div><div style='display: table-cell; text-align:right;'>" + linkConsumption.quality
				    	    			   + "</div><div style='display: table-cell; text-align:right;'>" + loadFactorPercentage + "%</div></div>")
		}			
	})
	if ($("#planLinkCompetitors .data-row").length < 5) { //then additional info
	    linkInfo.otherViaLocalTransitLinks.sort(function(a, b) {
            return b.capacity.total - a.capacity.total;
        });
	    $.each(linkInfo.otherViaLocalTransitLinks, function(index, linkConsumption) { //reachable by 1 local transit
                if (linkConsumption.airlineId != activeAirline.id) {
                    let loadFactorPercentage = Math.round(linkConsumption.soldSeats * 100 / linkConsumption.capacity.total)
                    var $row = $("<div class='table-row data-row' style='opacity: 60%'><div style='display: table-cell;'>" + getAirlineSpan(linkConsumption.airlineId, linkConsumption.airlineName)
                                                                          + "</div><div style='display: table-cell;'>" + toLinkClassValueString(linkConsumption.price, "$")
                                                                          + "</div><div style='display: table-cell; text-align:right;'>" + toLinkClassValueString(linkConsumption.capacity) + " (" + linkConsumption.frequency + ")"
                                                                          + "</div><div style='display: table-cell; text-align:right;'>" + linkConsumption.quality
                                                                          + "</div><div style='display: table-cell; text-align:right;'>" + loadFactorPercentage + "%</div></div>")
                    let phrases = []
                    if (linkConsumption.altFrom) {
                        phrases.push("Depart from " + linkConsumption.altFrom)
                    }
                    if (linkConsumption.altTo) {
                        phrases.push("Arrive at " + linkConsumption.altTo)
                    }
                    $row.attr('title', phrases.join('; '))
                    $("#planLinkCompetitors").append($row)
                }
            })
	}

	if ($("#planLinkCompetitors .data-row").length == 0) {
		$("#planLinkCompetitors").append("<div class='table-row data-row'><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div><div style='display: table-cell;'>-</div></div>")
	}
	
	if (tempPath) { //remove previous plan link if it exists
		removeTempPath()
	}
	
	$('#planLinkCost').text('$' + commaSeparateNumber(linkInfo.cost))

	if (linkInfo.estimatedDifficulty) {
	    $('#planLinkEstimatedDifficulty').text(linkInfo.estimatedDifficulty.toFixed(2) + " +")
    } else {
        $('#planLinkEstimatedDifficulty').text('-')
    }
    
	//unhighlight the existing path if any
	if (selectedLink) {
	    unhighlightLink(selectedLink)
	    if (!linkInfo.existingLink || linkInfo.existingLink.id != selectedLink) {
	        deselectLink()
	    }
	}
	
	if (!linkInfo.existingLink || !flightPaths[linkInfo.existingLink.id]) { //new link or link show visible (other views)
		//create a temp path
		var tempLink = {fromLatitude : linkInfo.fromAirportLatitude, fromLongitude : linkInfo.fromAirportLongitude, toLatitude : linkInfo.toAirportLatitude, toLongitude : linkInfo.toAirportLongitude}
		//set the temp path
		tempPath = drawFlightPath(tempLink, '#2658d3')
		highlightPath(tempPath.path, false)
	} else {
		//selectLinkFromMap(linkInfo.existingLink.id, true)
		highlightLink(linkInfo.existingLink.id, false)
	}

    $('#planLinkDetails .titleCue').removeClass('glow')
	if (linkInfo.rejection) {
		$('.linkRejection #linkRejectionReason').text(linkInfo.rejection.description)
		if (linkInfo.rejection.type === "TITLE_REQUIREMENT") {
		    $('#planLinkDetails .titleCue').addClass('glow')
		}
		$('.linkRejection').show()
		$('#addLinkButton').hide()
		$('#updateLinkButton').hide()
		$('#deleteLinkButton').hide()
		$('#planLinkExtendedDetails').hide()
		$('#planLinkModelRow').hide()
		$('#extendedPanel').hide()
		return
	} else {
		$('.linkRejection').hide()
		$('#planLinkModelRow').show()
	}

    var initialPrice = {}
	if (!linkInfo.existingLink) {
	    initialPrice.economy = linkInfo.suggestedPrice.economy
	    initialPrice.business = linkInfo.suggestedPrice.business
	    initialPrice.first = linkInfo.suggestedPrice.first

		$('#addLinkButton').show()
		$('#deleteLinkButton').hide()
		$('#updateLinkButton').hide()
	} else {
	    initialPrice.economy = linkInfo.existingLink.price.economy
        initialPrice.business = linkInfo.existingLink.price.business
        initialPrice.first = linkInfo.existingLink.price.first
		$('#addLinkButton').hide()
		if (linkInfo.deleteRejection) {
			$('#deleteLinkButton').hide()
		} else {
			$('#deleteLinkButton').show()
		}
		$('#updateLinkButton').show()
	}
	$('#planLinkEconomyPrice').val(initialPrice.economy)
    $('#planLinkBusinessPrice').val(initialPrice.business)
    $('#planLinkFirstPrice').val(initialPrice.first)

	$('.planLinkPrice').off(".priceChange").on("focusout.priceChange", function() {
        var defaultPrice = initialPrice[$(this).data('class')]
        if (isNaN($(this).val())) {
            $(this).val(defaultPrice)
        } else {
            var inputPrice = Number($(this).val());
            if (inputPrice < 0) {
                $(this).val(defaultPrice)
            } else {
                $(this).val(Math.floor(inputPrice))
            }
        }
	})


    //reset/display warnings
    $("#planLinkDetails .warningList").empty()
    if (linkInfo.warnings) {
        $.each(linkInfo.warnings, function(index, warning) {
            $("#planLinkDetails .warningList").append("<div class='warning'><img src='assets/images/icons/exclamation-red-frame.png'>&nbsp;" + warning + "</div>")
        })
    }

	
	//populate airplane model drop down
	var explicitlySelectedModelId = $("#planLinkModelSelect").data('explicitId')
	$("#planLinkModelSelect").removeData('explicitId')

	//or if refresh, just use whatever selected previously
	if (isRefresh) {
	    explicitlySelectedModelId = $('#planLinkModelSelect').find(":selected").attr('value')
    }

	$("#planLinkModelSelect").children('option').remove()

	planLinkInfo = linkInfo
	spaceMultipliers = {
                economy : planLinkInfo.economySpaceMultiplier,
                business : planLinkInfo.businessSpaceMultiplier,
                first : planLinkInfo.firstSpaceMultiplier
    }

	planLinkInfoByModel = {}
	//existingLinkModelId = 0

	//find which model is assigned to the existing link (if exist)
	var assignedModelId
	var selectedModelId
	
	if (explicitlySelectedModelId) { //if there was a explicitly selected model, for example from buying a new plane
		selectedModelId = explicitlySelectedModelId;
	}
	
	if (linkInfo.existingLink) {
		$.each(linkInfo.modelPlanLinkInfo, function(key, modelPlanLinkInfo) {
			if (modelPlanLinkInfo.isAssigned) { //higher precedence
				assignedModelId = modelPlanLinkInfo.modelId
				if (!selectedModelId) {
					selectedModelId = assignedModelId
				}
				return false
			}
		});
	}
	
	if (!selectedModelId) {
		$.each(linkInfo.modelPlanLinkInfo, function(key, modelPlanLinkInfo) {
			if (modelPlanLinkInfo.airplanes.length > 0) { //select the first one with available planes
				selectedModelId = modelPlanLinkInfo.modelId
				return false
			}  
		})
	}
	
	$.each(linkInfo.modelPlanLinkInfo, function(key, modelPlanLinkInfo) {
		if (modelPlanLinkInfo.airplanes.length > 0) {
			modelPlanLinkInfo.owned = true
		} else {
			modelPlanLinkInfo.owned = false
		}
	})
	
	linkInfo.modelPlanLinkInfo = sortPreserveOrder(linkInfo.modelPlanLinkInfo, "capacity", true)
	linkInfo.modelPlanLinkInfo = sortPreserveOrder(linkInfo.modelPlanLinkInfo, "owned", false)
	
	if (!selectedModelId) { //nothing available, select the first one in the list
		if (linkInfo.modelPlanLinkInfo.length > 0) { //select the first one with available planes
			selectedModelId = linkInfo.modelPlanLinkInfo[0].modelId
		}
	}
	
	$.each(linkInfo.modelPlanLinkInfo, function(key, modelPlanLinkInfo) {
		var modelId = modelPlanLinkInfo.modelId
		var modelname = modelPlanLinkInfo.modelName

		var option = $("<option></option>").attr("value", modelId).text(modelname + " (" + modelPlanLinkInfo.maxFrequency + ")")
		if (modelPlanLinkInfo.airplanes.length > 0) {
		    option.addClass("highlight-text")
		}

		option.appendTo($("#planLinkModelSelect"))
		
		if (selectedModelId == modelId) {
			option.prop("selected", true)
			updateModelInfo(modelId)
		}
		
		planLinkInfoByModel[modelId] = modelPlanLinkInfo
	});
	
	if (linkInfo.modelPlanLinkInfo.length == 0) {
		$("#planLinkModelSelect").next($(".warning")).remove()
		$("#planLinkModelSelect").after("<span class='label warning'>No airplane model can fly to this destination</span>")
		$("#planLinkModelSelect").hide()
		
		hideActiveDiv($("#extendedPanel #airplaneModelDetails"))
	} else {
		$("#planLinkModelSelect").next($(".warning")).remove()
		$("#planLinkModelSelect").show()
		
		setActiveDiv($("#extendedPanel #airplaneModelDetails"))
	}
	
	updatePlanLinkInfoWithModelSelected(selectedModelId, assignedModelId, isRefresh)
	$("#planLinkDetails div.value").show()
}

function resetPrice() {
	updatePrice(1)
}

function increasePrice() {
	var currentPrice = parseFloat($('#planLinkEconomyPrice').val()) * 20
	var currentPercentage = Math.round(currentPrice / planLinkInfo.suggestedPrice.economy) / 20
	var newPercentage = currentPercentage + 0.05
	updatePrice(newPercentage)
	$('#planLinkPricePercentage').val(newPercentage)
}

function decreasePrice() {
	var currentPrice = parseFloat($('#planLinkEconomyPrice').val()) * 20
	var currentPercentage = Math.round(currentPrice / planLinkInfo.suggestedPrice.economy) / 20
	var newPercentage = currentPercentage
	if (currentPercentage > 0) {
		newPercentage -= 0.05
	}
	updatePrice(newPercentage)
	
	$('#planLinkPricePercentage').val(newPercentage)
}

function updatePrice(percentage) {
	$('#planLinkEconomyPrice').val(Math.round(planLinkInfo.suggestedPrice.economy * percentage))
	$('#planLinkBusinessPrice').val(Math.round(planLinkInfo.suggestedPrice.business * percentage))
	$('#planLinkFirstPrice').val(Math.round(planLinkInfo.suggestedPrice.first * percentage))
}

function updateFrequencyBar(frequencyBar, valueContainer, airplane, currentFrequency) {
    var availableFrequency = Math.floor(airplane.availableFlightMinutes / planLinkInfoByModel[airplane.modelId].flightMinutesRequired)
    var maxFrequency = availableFrequency + currentFrequency
    if (currentFrequency == 0) { //set 1 as min
        valueContainer.val(1)
    }
    generateImageBar(frequencyBar.data("emptyIcon"), frequencyBar.data("fillIcon"), maxFrequency, frequencyBar, valueContainer, null, null, updateTotalValues)
}

function updatePlanLinkInfoWithModelSelected(newModelId, assignedModelId, isRefresh) {
    selectedModelId = newModelId //modify the global one
    selectedModel = loadedModelsById[newModelId]
	if (selectedModelId) {
		var thisModelPlanLinkInfo = planLinkInfoByModel[selectedModelId]
		

//		thisModelPlanLinkInfo.airplanes.sort(sortByProperty('airplane.condition', true))
//		thisModelPlanLinkInfo.airplanes = sortPreserveOrder(thisModelPlanLinkInfo.airplanes, 'frequency', false) //higher frequency first
		
		$('#planLinkAirplaneSelect').data('badConditionThreshold', thisModelPlanLinkInfo.badConditionThreshold)

		thisModelPlanLinkInfo.airplanes.sort(function(a, b) {
		    var result = b.frequency - a.frequency
		    if (result != 0) {
		        if (b.frequency == 0 || a.frequency == 0) { //if either one is not assigned to this route at all, then return result ie also higher precedence to compare if airplane is assigned
		            return result
		        }
		    }

		    return a.airplane.condition - b.airplane.condition //otherwise: both assigned or both not assigned, then return lowest condition ones first
		})

        $('#planLinkAirplaneSelect').empty()

		$.each(thisModelPlanLinkInfo.airplanes, function(key, airplaneEntry) {
//			var option = $("<option></option>").attr("value", airplane.airplaneId).text("#" + airplane.airplaneId)
//			option.appendTo($("#planLinkAirplaneSelect"))

			//check existing UI changes if just a refresh
			if (isRefresh) {
			    var $existingAirplaneRow = $("#planLinkDetails .frequencyDetail .airplaneRow[data-airplaneId='" + airplaneEntry.airplane.id + "']")
			    //UI values merge into the airplane/frequency info as we want to preserve previously UI change on refresh
    			mergeAirplaneEntry(airplaneEntry, $existingAirplaneRow)
			}

			var airplane = airplaneEntry.airplane
			airplane.isAssigned = airplaneEntry.frequency >  0
			var div =  $('<div class="clickable airplaneButton" onclick="toggleAssignedAirplane(this)" style="float: left;"></div>')
			div.append(getAssignedAirplaneIcon(airplane))
			div.data('airplane', airplane)
			div.data('existingFrequency', airplaneEntry.frequency)

			$('#planLinkAirplaneSelect').append(div)
		})
		if (thisModelPlanLinkInfo.airplanes.length == 0) {
		    $('#planLinkDetails .noAirplaneHelp').show()
		} else {
		    $('#planLinkDetails .noAirplaneHelp').hide()
		}
		toggleUtilizationRate($('#planLinkAirplaneSelect'), $('#planLinkExtendedDetails .toggleUtilizationRateBox'))
		toggleCondition($('#planLinkAirplaneSelect'), $('#planLinkExtendedDetails .toggleConditionBox'))


		$('#planLinkDuration').text(getDurationText(thisModelPlanLinkInfo.duration))

		if (!isRefresh) { //for refresh, do not reload the existing link, otherwise refresh on config change would show the new values in confirmation dialog etc
		    existingLink = planLinkInfo.existingLink
        }
		
		if (existingLink) {
			$("#planLinkServiceLevel").val(existingLink.rawQuality / 20)
		} else {
			$("#planLinkServiceLevel").val(1)
		}
	
		updateFrequencyDetail(thisModelPlanLinkInfo)

		var serviceLevelBar = $("#serviceLevelBar")
		generateImageBar(serviceLevelBar.data("emptyIcon"), serviceLevelBar.data("fillIcon"), 5, serviceLevelBar, $("#planLinkServiceLevel"))
		$("#planLinkExtendedDetails").show()
	} else {
		$("#planLinkExtendedDetails").hide()
	}
}
//Merge UI change (though temp) to the data loaded from api service
//This is used for refresh which we want to reload but keep temp changes from UI
function mergeAirplaneEntry(airplaneEntry, $airplaneRow) {
    var frequencyFromUi = $airplaneRow.length ? parseInt($airplaneRow.find(".frequency").val()) : 0
    var frequencyDelta = frequencyFromUi - airplaneEntry.frequency
    if (frequencyDelta != 0) { //then UI value is not the same as the original. Do adjustments
        airplaneEntry.frequency = frequencyFromUi
        var airplane = airplaneEntry.airplane
        airplane.availableFlightMinutes = airplane.availableFlightMinutes - (planLinkInfoByModel[airplane.modelId].flightMinutesRequired * frequencyDelta)
    }
}

function updateFrequencyDetail(info) {
    var airplaneEntries = info.airplanes
    $("#planLinkDetails .frequencyDetail .table-row").remove()

    var isEmpty = true
    $.each(airplaneEntries, function(index, airplaneEntry) {
        if (airplaneEntry.frequency > 0) { //only draw for those that are assigned to this link
            addAirplaneRow($("#planLinkDetails .frequencyDetail"), airplaneEntry.airplane, airplaneEntry.frequency)
            isEmpty = false
        }
    })
    if (isEmpty) {
        $("#planLinkDetails .frequencyDetail").append("<div class='table-row empty'><div class='cell'></div><div class='cell'>-</div><div class='cell'>-</div></div>")
    }

//    updateLimit()
    updateTotalValues()
}



function addAirplaneRow(container, airplane, frequency) {
    var airplaneRow = $("<div class='table-row airplaneRow'></div>") //airplane bar contains - airplane icon, configuration, frequency

    var configurationDiv = $("<div class='configuration' style='transform: translate(0%, -75%);'></div>")
    var airplaneUpdateCallback = function(configurationDiv, airplaneId) {
        return function() {
            $.ajax({
                    type: 'GET',
                    url: "airlines/" + activeAirline.id + "/airplanes/" + airplaneId,
                    contentType: 'application/json; charset=utf-8',
                    dataType: 'json',
                    success: function(result) {
                        var updatedAirplane = result
                        //should not redraw the whole airplaneRow as the unsaved frequency change will be reverted
                        plotSeatConfigurationBar(configurationDiv, updatedAirplane.configuration, updatedAirplane.capacity, spaceMultipliers, true, "10px")
                        airplaneRow.data("airplane", updatedAirplane)
                        updateTotalValues()
                    },
                    error: function(jqXHR, textStatus, errorThrown) {
                            console.log(JSON.stringify(jqXHR));
                            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
                    }
                });
        }
    }

    var airplaneCellOuter = $('<div class="cell"></div>')
    var airplaneCell = $("<div style='display: flex; align-items: center;'></div>")
    airplaneCellOuter.append(airplaneCell)

    var onclickFunction = 'loadOwnedAirplaneDetails(' + airplane.id + ', null, $(this).data(\'airplaneUpdateCallback\'), true)'
    var airplaneInspectIcon = $('<div class="clickable-no-highlight" onclick="' + onclickFunction + '" style="display: inline-block; margin-left: 1px; margin-right: 1px;"></div>')
    airplaneInspectIcon.data("airplaneUpdateCallback", airplaneUpdateCallback(configurationDiv, airplane.id))
    airplaneInspectIcon.append($('<img src="assets/images/icons/airplane-magnifier.png" title="Inspect airplane #' + airplane.id + '">'))
    airplaneCell.append(airplaneInspectIcon)

    var airplaneRemovalIcon = $('<div class="clickable-no-highlight" onclick="removeAirplaneFromLink(' + airplane.id + ')" style="display: inline-block; margin-left: 1px; margin-right: 1px;"></div>')
    airplaneRemovalIcon.append($('<img src="assets/images/icons/airplane-minus.png" title="Unassign airplane #' + airplane.id + '">'))
    airplaneCell.append(airplaneRemovalIcon)

    airplaneCell.append($("<span>#" + airplane.id + "</span>"))

    var sharedLinkCount = 0
    $.each(airplane.linkAssignments, function(linkId, frequency) {
        if (linkId != selectedLink) {
            sharedLinkCount ++
        }
    })
    if (sharedLinkCount > 0) {
        airplaneCell.append($('<img src="assets/images/icons/information.png" title="Shared with ' + sharedLinkCount + ' other route(s)">'))
    }

    if (!airplane.isReady) {
        airplaneCell.append($('<img src="assets/images/icons/construction.png" title="Under construction">'))
    }

    airplaneRow.append(airplaneCellOuter)


    var configurationCell = $("<div class='cell'></div>")
    configurationCell.append(configurationDiv)
    airplaneRow.append(configurationCell)

    var frequencyBar = $("<div class='frequencyBar cell' data-empty-icon='assets/images/icons/round-dot-grey.png' data-fill-icon='assets/images/icons/round-dot-green.png'></div>")
    airplaneRow.append(frequencyBar)

    var valueContainer = $("<input class='frequency' type='hidden'>") //so changing the frequency bar would write the new value back to this ...is this necessary? since there's a callback function now...
    valueContainer.val(frequency)
    airplaneRow.append(valueContainer)
    airplaneRow.data("airplane", airplane)
    airplaneRow.attr('data-airplaneId', airplane.id) //for easier jquery selector

    container.append(airplaneRow)
    updateFrequencyBar(frequencyBar, valueContainer, airplane, frequency)
    plotSeatConfigurationBar(configurationDiv, airplane.configuration, airplane.capacity, spaceMultipliers, true, "10px")
}

function addAirplaneToLink(airplane, frequency) {
    $("#planLinkDetails .frequencyDetail .table-row.empty").remove()
    addAirplaneRow($("#planLinkDetails .frequencyDetail"), airplane, frequency)
    updateTotalValues()
}

function removeAirplaneFromLink(airplaneId) {
    $("#planLinkDetails .frequencyDetail .airplaneRow").each(function(index, row){
        if ($(row).data("airplane").id == airplaneId) {
            $(row).remove()
        }
    })
    if ($("#planLinkDetails .frequencyDetail .airplaneRow").length == 0) {
        $("#planLinkDetails .frequencyDetail").append("<div class='table-row empty'><div class='cell'></div><div class='cell'>-</div><div class='cell'>-</div></div>")
    }

    updateTotalValues()

    //update the available airplane list
    $('#planLinkAirplaneSelect .airplaneButton').each(function(index, airplaneIcon){
      var airplane = $(airplaneIcon).data('airplane')
      if (airplane.id == airplaneId) {
        airplane.isAssigned = false
        $(airplaneIcon).find('img').replaceWith(getAssignedAirplaneImg(airplane))
      }
    })
}


//Get capacity based on current UI status
function getPlanLinkCapacity() {
    var currentFrequency = 0 //airplanes that are ready
    var currentCapacity = { "economy" : 0, "business" : 0, "first" : 0}

    var futureFrequency = 0 //airplanes that are ready + under construction
    var futureCapacity = { "economy" : 0, "business" : 0, "first" : 0}
    var hasUnderConstructionAirplanes = false

    $("#planLinkDetails .frequencyDetail .airplaneRow").each(function(index, airplaneRow) {
       frequency = parseInt($(airplaneRow).find(".frequency").val())
       configuration = $(airplaneRow).data("airplane").configuration

       futureFrequency += frequency
       futureCapacity.economy += configuration.economy * frequency
       futureCapacity.business += configuration.business * frequency
       futureCapacity.first += configuration.first * frequency

       if ($(airplaneRow).data("airplane").isReady) {
           currentFrequency += frequency
           currentCapacity.economy += configuration.economy * frequency
           currentCapacity.business += configuration.business * frequency
           currentCapacity.first += configuration.first * frequency
       } else {
            hasUnderConstructionAirplanes = true
       }
    })

    if (hasUnderConstructionAirplanes) {
        return { "current" : { "capacity" : currentCapacity, "frequency" : currentFrequency }, "future" : { "capacity" : futureCapacity, "frequency" : futureFrequency }}
    } else {
        return { "current" : { "capacity" : currentCapacity, "frequency" : currentFrequency }}
    }
}


// Update total frequency and capacity
function updateTotalValues() {
    var planCapacity = getPlanLinkCapacity()
    var currentCapacity = planCapacity.current.capacity
    var futureFrequency = planCapacity.future ? planCapacity.future.frequency : planCapacity.current.frequency
    var futureCapacity = planCapacity.future ? planCapacity.future.capacity : planCapacity.current.capacity

    $(".frequencyDetailTotal .total").text(futureFrequency)

    $('#planLinkCapacity').text(toLinkClassValueString(currentCapacity))
    if (planCapacity.future) {
        $("#planLinkDetails .future .capacity").text(toLinkClassValueString(futureCapacity))
        $("#planLinkDetails .future").show()
    } else {
        $("#planLinkDetails .future").hide()
    }


    $('#planLinkAirplaneSelect').removeClass('glow')
    $('.noAirplaneHelp').removeClass('glow')
    if (futureFrequency == 0) {
        disableButton($("#planLinkDetails .modifyLink"), "Must assign airplanes and frequency")

        var thisModelPlanLinkInfo = planLinkInfoByModel[selectedModelId]
        if (thisModelPlanLinkInfo.airplanes.length == 0) {
            $('.noAirplaneHelp').addClass('glow')
        } else {
            $('#planLinkAirplaneSelect').addClass('glow')
        }
    } else {
        enableButton($("#planLinkDetails .modifyLink"))
    }
    getLinkStaffingInfo()

    $('#planLinkEstimatedDifficulty').remove('.remarks')
    getLinkNegotiation(function(result) {
        if (result.negotiationInfo.finalRequirementValue) {
            $('#planLinkEstimatedDifficulty').text(result.negotiationInfo.finalRequirementValue.toFixed(2))
        } else {
            if (result.negotiationInfo.remarks) {
                $('#planLinkEstimatedDifficulty').empty()
                var $remarksSpan = $('<span class="remarks glow"></span>').appendTo($('#planLinkEstimatedDifficulty'))
                $remarksSpan.text(result.negotiationInfo.remarks)
            } else if (futureFrequency > 0) { //otherwise it might just overwrite estimated difficulty on new link
                $('#planLinkEstimatedDifficulty').text('-')
            }
        }
    })
}


function getAssignedAirplaneIcon(airplane) {
	var badConditionThreshold = $('#planLinkAirplaneSelect').data('badConditionThreshold')
	return getAirplaneIcon(airplane, badConditionThreshold, airplane.isAssigned)
}

function getAssignedAirplaneImg(airplane) {
	var badConditionThreshold = $('#planLinkAirplaneSelect').data('badConditionThreshold')
	return getAirplaneIconImg(airplane, badConditionThreshold, airplane.isAssigned)
}


function toggleAssignedAirplane(iconSpan) {
	var airplane = $(iconSpan).data('airplane')
	var existingFrequency =  $(iconSpan).data('existingFrequency')
	if (airplane.isAssigned) {
		airplane.isAssigned = false
	} else {
		airplane.isAssigned = true
	}
	$(iconSpan).find('img').replaceWith(getAssignedAirplaneImg(airplane))

	if (airplane.isAssigned) { //add to the airplane frequency detail
        addAirplaneToLink(airplane, existingFrequency)
	} else { //remove from the airplane frequency detail
	    removeAirplaneFromLink(airplane.id)
	}
}

function getAssignedAirplaneFrequencies() {
	var assignedAirplaneFrequencies = {} //key airplaneId, value frequeuncy
	$('#planLinkDetails .frequencyDetail').find('.airplaneRow').each(function(index, airplaneRow) {
		var airplane = $(airplaneRow).data("airplane")
        assignedAirplaneFrequencies[airplane.id] = parseInt($(airplaneRow).find('.frequency').val())
	})
	
	return assignedAirplaneFrequencies
}

function createLink() {
	if ($("#planLinkFromAirportId").val() && $("#planLinkToAirportId").val()) {
		var airlineId = activeAirline.id
		var url = "airlines/" + airlineId + "/links"
	    //console.log("selected " + $("#planLinkAirplaneSelect").val())
	    var configuration = planLinkInfoByModel[$("#planLinkModelSelect").val()].configuration
	    var linkData = { 
			"fromAirportId" : parseInt($("#planLinkFromAirportId").val()), 
			"toAirportId" : parseInt($("#planLinkToAirportId").val()),
			//"airplanes" : $("#planLinkAirplaneSelect").val().map(Number),
			airplanes : getAssignedAirplaneFrequencies(),
			"airlineId" : airlineId,
			//"configuration" : { "economy" : configuration.economy, "business" : configuration.business, "first" : configuration.first},
			"price" : { "economy" : parseInt($("#planLinkEconomyPrice").val()), "business" : parseInt($("#planLinkBusinessPrice").val()), "first" : parseInt($("#planLinkFirstPrice").val())},
			//"frequency" : parseInt($("#planLinkFrequency").val()),
			"model" : parseInt($("#planLinkModelSelect").val()),
			"rawQuality" : parseInt($("#planLinkServiceLevel").val()) * 20,
			"assignedDelegates" : assignedDelegates }
		$.ajax({
			type: 'PUT',
			url: url,
		    data: JSON.stringify(linkData),
		    contentType: 'application/json; charset=utf-8',
		    dataType: 'json',
		    success: function(savedLink) {
		    	var isSuccessful
		    	closeModal($('#linkConfirmationModal'))
                if (savedLink.negotiationResult) {
                    isSuccessful = savedLink.negotiationResult.isSuccessful
                    if (isSuccessful) {
                        negotiationAnimation(savedLink, refreshSavedLink, savedLink)
                    } else {
                        negotiationAnimation(savedLink, updateTopBarDelegates, activeAirline.id)
                    }
                } else {
                    refreshSavedLink(savedLink)
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
}

function deleteLink() {
	var linkId = $('#actionLinkId').val()
	$.ajax({
		type: 'DELETE',
		url: "airlines/" + activeAirline.id + "/links/" + linkId,
	    success: function() {
	    	$("#linkDetails").fadeOut(200)
	    	updateLinksInfo()
	    	deselectLink()
	    	
	    	if ($('#linksCanvas').is(':visible')) { //reload the links table then
		    	loadLinksTable()
    		}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function getLinkStaffingInfo() {
    var airlineId = activeAirline.id
    var url = "airlines/" + airlineId + "/link-overtime-compensation"
    //console.log("selected " + $("#planLinkAirplaneSelect").val())
    var configuration = planLinkInfoByModel[$("#planLinkModelSelect").val()].configuration
    var linkData = {
        "fromAirportId" : parseInt($("#planLinkFromAirportId").val()),
        "toAirportId" : parseInt($("#planLinkToAirportId").val()),
        //"airplanes" : $("#planLinkAirplaneSelect").val().map(Number),
        airplanes : getAssignedAirplaneFrequencies(),
        "airlineId" : airlineId,
        //"configuration" : { "economy" : configuration.economy, "business" : configuration.business, "first" : configuration.first},
        "price" : { "economy" : parseInt($("#planLinkEconomyPrice").val()), "business" : parseInt($("#planLinkBusinessPrice").val()), "first" : parseInt($("#planLinkFirstPrice").val())},
        //"frequency" : parseInt($("#planLinkFrequency").val()),
        "model" : parseInt($("#planLinkModelSelect").val()),
        "rawQuality" : parseInt($("#planLinkServiceLevel").val()) * 20,
        "assignedDelegates" : assignedDelegates }
    $.ajax({
        type: 'POST',
        url: url,
        data: JSON.stringify(linkData),
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            if (result.extraOvertimeCompensation > 0) {
                $('#planLinkDetails .overtimeCompensation .amount').text(result.extraOvertimeCompensation)
                $('#planLinkDetails .overtimeCompensation').show()
            } else {
                $('#planLinkDetails .overtimeCompensation').hide()
            }

            $('#planLinkDetails .staffRequired').text(result.staffBreakdown.total)

            $('#linkStaffBreakdownTooltip .flightType').text(result.flightType)
            $('#linkStaffBreakdownTooltip .basic').text(result.staffBreakdown.basic)
            var frequencyStaff = result.staffBreakdown.frequency.toFixed(1)
            $('#linkStaffBreakdownTooltip .frequency').text(frequencyStaff)
            var capacityStaff = result.staffBreakdown.capacity.toFixed(1)
            $('#linkStaffBreakdownTooltip .capacity').text(capacityStaff)
            $('#linkStaffBreakdownTooltip .modifier').text(result.staffBreakdown.modifier == 1 ? "-" : result.staffBreakdown.modifier)

            var totalText = result.staffBreakdown.basic + " + " + frequencyStaff + " + " + capacityStaff
            if (result.staffBreakdown.modifier != 1) {
                totalText = "(" + totalText + ") * " + result.staffBreakdown.modifier
            }
            totalText += " = " + result.staffBreakdown.total
            $('#linkStaffBreakdownTooltip .total').text(totalText)

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
		//hideActiveDiv($('#planLinkDetails'))
		$('#sidePanel').fadeOut(200) //hide the whole side panel
	} else { //simply go back to linkDetails of the current link (exit edit mode)
		setActiveDiv($('#linkDetails'))
	}
	hideActiveDiv($("#extendedPanel #airplaneModelDetails"))
}

function removeTempPath() {
	if (tempPath) {
		unhighlightPath(tempPath.path)
		clearPathEntry(tempPath)
		tempPath = undefined
	}
}

function showLinksDetails() {
	selectedLink = undefined
	loadLinksTable()
	setActiveDiv($('#linksCanvas'));
	highlightTab($('.linksCanvasTab'))
	$('#sidePanel').fadeOut(200);
	$('#sidePanel').appendTo($('#linksCanvas'))
}

function loadLinksTable() {
	var url = "airlines/" + activeAirline.id + "/links-details"
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(links) {
	    	updateLoadedLinks(links);
	    	$.each(links, function(key, link) {
				link.totalCapacity = link.capacity.economy + link.capacity.business + link.capacity.first
				link.totalCapacityHistory = link.capacityHistory.economy + link.capacityHistory.business + link.capacityHistory.first
				link.totalPassengers = link.passengers.economy + link.passengers.business + link.passengers.first
				link.totalLoadFactor = link.totalCapacityHistory > 0 ? Math.round(link.totalPassengers / link.totalCapacityHistory * 100) : 0
				var assignedModel 
				if (link.assignedAirplanes && link.assignedAirplanes.length > 0) {
					assignedModel = link.assignedAirplanes[0].airplane.name
				} else {
					assignedModel = "-"
				}
				link.model = assignedModel //so this can be sorted
			})
	    	
			var selectedSortHeader = $('#linksTableSortHeader .cell.selected') 
		    updateLinksTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function toggleLinksTableSortOrder(sortHeader) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}
	
	sortHeader.siblings().removeClass("selected")
	sortHeader.addClass("selected")
	
	updateLinksTable(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
}

function updateLinksTable(sortProperty, sortOrder) {
	var linksTable = $("#linksCanvas #linksTable")
	linksTable.children("div.table-row").remove()
	
	//sort the list
	//loadedLinks.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	loadedLinks = sortPreserveOrder(loadedLinks, sortProperty, sortOrder == "ascending")
	
	$.each(loadedLinks, function(index, link) {
		var row = $("<div class='table-row clickable' onclick='selectLinkFromTable($(this), " + link.id + ")'></div>")
		
		row.append("<div class='cell'>" + getCountryFlagImg(link.fromCountryCode) + getAirportText(link.fromAirportCity, link.fromAirportCode) + "</div>")
		row.append("<div class='cell'>" + getCountryFlagImg(link.toCountryCode) + getAirportText(link.toAirportCity, link.toAirportCode) + "</div>")
		row.append("<div class='cell'>" + link.model + "</div>")
		row.append("<div class='cell' align='right'>" + link.distance + "km</div>")
		row.append("<div class='cell' align='right'>" + link.totalCapacity + "(" + link.frequency + ")</div>")
		row.append("<div class='cell' align='right'>" + link.totalPassengers + "</div>")
		row.append("<div class='cell' align='right'>" + link.totalLoadFactor + '%' + "</div>")
		row.append("<div class='cell' align='right'>" + Math.round(link.satisfaction * 100) + '%' + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(link.revenue) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(link.profit) + "</div>")
		
		if (selectedLink == link.id) {
			row.addClass("selected")
		}
		
		linksTable.append(row)
	});
	if (loadedLinks.length == 0) {
        $('#linksCanvas .noLinkTips').show();
	} else {
	    $('#linksCanvas .noLinkTips').hide();
    }


}

function selectLinkFromMap(linkId, refocus) {
	refocus = refocus || false 
	unhighlightLink(selectedLink)
	selectedLink = linkId
	highlightLink(linkId, refocus)
	
	//update link details panel
	refreshLinkDetails(linkId)
}


function selectLinkFromTable(row, linkId) {
	selectedLink = linkId
	//update table
	row.siblings().removeClass("selected")
	row.addClass("selected")
	
	//update link details panel
	refreshLinkDetails(linkId)
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


function updateLoadedLinks(links) {
	var previousOrder = {}
	if (loadedLinks) {
		$.each(loadedLinks, function(index, link) {
			previousOrder[link.id] = index
		})
		$.each(links, function(index, link) {
			link.previousOrder = previousOrder[link.id]
		})
		loadedLinks = links;
		loadedLinks.sort(sortByProperty("previousOrder"), true)
	} else {
		loadedLinks = links;
	}
	
	
	loadedLinksById = {}
	$.each(links, function(index, link) {
		loadedLinksById[link.id] = link
	});
}

function showLinkExpectedQualityModal(isFromAirport) {
	var fromAirportId = $('#planLinkFromAirportId').val()
	var toAirportId = $('#planLinkToAirportId').val()
	var queryAirportId
	if (isFromAirport) {
		queryAirportId = fromAirportId
	} else {
		queryAirportId = toAirportId
	}
	var url = "airlines/" + activeAirline.id + "/expectedQuality/" + fromAirportId + "/" + toAirportId + "/" + queryAirportId
	$('#expectedQualityModal .expectedQualityValue').empty()
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	$('#expectedQualityModal .expectedQualityValue.firstClass').html(getGradeStarsImgs(Math.round(result.F / 10)))
	    	$('#expectedQualityModal .expectedQualityValue.businessClass').html(getGradeStarsImgs(Math.round(result.J / 10)))
	    	$('#expectedQualityModal .expectedQualityValue.economyClass').html(getGradeStarsImgs(Math.round(result.Y / 10)))
	    	$('#expectedQualityModal').fadeIn(200)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function showLinkComposition(linkId) {
	var url = "airlines/" + activeAirline.id + "/link-composition/" + linkId
	
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	        updateSatisfaction(result)

	    	updateTopCountryComposition(result.country)
	    	updatePassengerTypeComposition(result.passengerType)
	    	updatePreferenceTypeComposition(result.preferenceType)
	    	updateTopAirportComposition($('#linkCompositionModal div.topHomeAirports'), result.homeAirports)
	    	updateTopAirportComposition($('#linkCompositionModal div.topDestinationAirports'), result.destinationAirports)
	    	plotPie(result.country, null , $("#passengerCompositionByCountryPie"), "countryName", "passengerCount")
	    	plotPie(result.passengerType, null , $("#passengerCompositionByPassengerTypePie"), "title", "passengerCount")
	    	plotPie(result.preferenceType, null , $("#passengerCompositionByPreferenceTypePie"), "title", "passengerCount")

	    	$('#linkCompositionModal').fadeIn(200)
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


function showLinkEventHistory(linkId) {
    $('#linkEventModal .chart').hide()
    $('#linkRivalHistoryChart').show()

    //always default to all airlines (instead of self)
    $("#switchLinkEventRival").prop('checked', true)
    $("#switchLinkEventSelf").prop('checked', false)

    var link = $("#linkEventModal").data("link")
    $("#linkEventModal .title").html("<div style='display: inline-flex; align-items: center;'>"
    + getCountryFlagImg(link.fromCountryCode)
    + getAirportText(link.fromAirportCity, link.fromAirportCode)
    + "<img src='assets/images/icons/arrow.png' style='margin: 0 5px;'>"
    + getCountryFlagImg(link.toCountryCode)
    + getAirportText(link.toAirportCity, link.toAirportCode) + "</div>")
    $('#linkEventModal .fromAirportCode').text(link.fromAirportCode)
    $('#linkEventModal .toAirportCode').text(link.toAirportCode)
    $('#linkEventModal .bothAirportCode').append(link.fromAirportCode + link.toAirportCode)
    $("#linkEventModal div.filterCheckboxes input:checkbox").prop('checked', true)

    var linkConsumptions = $($('#linkEventChart').data('linkConsumptions')).toArray().slice(0, 8 * 13)

    var chart = plotLinkEvent(linkConsumptions, $('#linkEventChart'),
        function(hoverCycle) {
            var $linkEventTableContainer = $("#linkEventModal .linkEventHistoryTableContainer")
            $linkEventTableContainer.find(".table-row").removeClass('selected')
            var $matchingRows = $linkEventTableContainer.find(".table-row[data-cycle='" + hoverCycle + "']")
            $matchingRows.addClass('selected')
            if ($matchingRows.length > 0) {
                scrollToRow($matchingRows[0], $linkEventTableContainer)
            }
        },
        function() { //chartout
             $("#linkEventModal .linkEventHistoryTableContainer .table-row").removeClass('selected')
        })
    $("#linkEventChart").data("chart", chart) //record back to the container

    //load rival comparison
    $.ajax({
        		type: 'GET',
        		url: "airlines/" + activeAirline.id + "/link-related-rival-history/" + linkId + "?cycleCount=" + linkConsumptions.length,
        	    contentType: 'application/json; charset=utf-8',
        	    dataType: 'json',
        	    success: function(result) {
                    var chart = plotRivalHistory(result, $('#linkRivalHistoryChart'),
                        function(hoverCycle) {
                            var $linkEventTableContainer = $("#linkEventModal .linkEventHistoryTableContainer")
                            $linkEventTableContainer.find(".table-row").removeClass('selected')
                            var $matchingRows = $linkEventTableContainer.find(".table-row[data-cycle='" + hoverCycle + "']")
                            $matchingRows.addClass('selected')
                            if ($matchingRows.length > 0) {
                                var row = $matchingRows[0]
                                var baseOffset = $linkEventTableContainer.find(".table-row")[0].offsetTop //somehow first row is not 0...
                                var realOffset = row.offsetTop - baseOffset
                                $linkEventTableContainer.stop(true, true) //stop previous animation
                                $linkEventTableContainer.animate ({scrollTop: realOffset}, "fast");
                            }
                        },
                        function() { //chartout
                             $("#linkEventModal .linkEventHistoryTableContainer .table-row").removeClass('selected')
                        }
                    )
                    $("#linkRivalHistoryChart").data("chart", chart) //record back to the container
        	    },
                error: function(jqXHR, textStatus, errorThrown) {
        	            console.log(JSON.stringify(jqXHR));
        	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        	    }
        	});






    var url = "airlines/" + activeAirline.id + "/link-related-event-history/" + linkId + "?cycleCount=" + linkConsumptions.length
    $.ajax({
    		type: 'GET',
    		url: url,
    	    contentType: 'application/json; charset=utf-8',
    	    dataType: 'json',
    	    success: function(result) {
                var linkEventTable = $("#linkEventModal .linkEventHistoryTable")
                linkEventTable.children("div.table-row").remove()

                result = result.sort(function (a, b) {
                    return b.cycle - a.cycle
                });

                $.each(result, function(index, entry) {
                    var row = $("<div class='table-row clickable'></div>")
                    row.attr("data-cycle", entry.cycle)
                    row.data("index", index)
                    row.append("<div class='cell'>" + getCycleDeltaText(entry.cycleDelta) + "</div>")
                    if (entry.airlineId) {
                        row.append("<div class='cell'>" + getAirlineLogoImg(entry.airlineId) + entry.airlineName + "</div>")
                    } else {
                        row.append("<div class='cell'>-</div>")
                    }

                    var $descriptionCell = $("<div class='cell'>" + entry.description + "</div>")
                    if (entry.descriptionCountryCode) {
                        $descriptionCell.prepend(getCountryFlagImg(entry.descriptionCountryCode))
                    }
                    row.append($descriptionCell)
                    if (entry.capacity) {
                        $("<div class='cell' align='right'></div>").appendTo(row).append(getCapacitySpan(entry.capacity, entry.frequency))
                    } else {
                        row.append("<div class='cell'>-</div>")
                    }

                    if (entry.capacityDelta) {
                        $("<div class='cell' align='right'></div>").appendTo(row).append(getCapacityDeltaSpan(entry.capacityDelta))
                    } else {
                        row.append("<div class='cell'>-</div>")
                    }

                    if (entry.price) {
                        $("<div class='cell'></div>").appendTo(row).text(toLinkClassValueString(entry.price, '$'))
                    } else {
                        row.append("<div class='cell'>-</div>")
                    }
                    if (entry.priceDelta) {
                        $("<div class='cell'></div>").appendTo(row).append(getPriceDeltaSpan(entry.priceDelta))
                    } else {
                        row.append("<div class='cell'>-</div>")
                    }
                    row.mouseenter(function() {
                        toggleLinkEventBar($('#linkEventModal .chart:visible').data('chart'), entry.cycle, true)
                    })
                    if (entry.matchFrom) {
                        row.addClass('filter-fromAirport')
                    }
                    if (entry.matchTo) {
                        row.addClass('filter-toAirport')
                    }
                    if (entry.matchFrom && entry.matchTo) {
                        row.addClass('filter-bothAirport')
                    }
                    if (!entry.matchFrom && !entry.matchTo) {
                        row.addClass('filter-other')
                    }
                    linkEventTable.append(row)
                });

                if (result.length == 0) {
                    var row = $("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div></div>")
                    linkEventTable.append(row)
                }

                linkEventTable.mouseleave(function() {
                  toggleLinkEventBar($('#linkEventModal .chart:visible').data('chart'), -1, false)
                })
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

    $('#linkEventModal').fadeIn(200)
}

function showLinkRivalHistory(linkId) {
	var url = "airlines/" + activeAirline.id + "/link-rival-history/" + linkId + "?cycleCount=30"

	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	plotRivalHistoryChart(result.overlappingLinks, $("#rivalEconomyPriceChart"), "economy", "price", "$", activeAirline.id)
	    	plotRivalHistoryChart(result.overlappingLinks, $("#rivalBusinessPriceChart"), "business", "price", "$", activeAirline.id)
	    	plotRivalHistoryChart(result.overlappingLinks, $("#rivalFirstPriceChart"), "first", "price", "$", activeAirline.id)
	    	plotRivalHistoryChart(result.overlappingLinks, $("#rivalEconomyCapacityChart"), "economy", "capacity", "", activeAirline.id)
            plotRivalHistoryChart(result.overlappingLinks, $("#rivalBusinessCapacityChart"), "business", "capacity", "", activeAirline.id)
            plotRivalHistoryChart(result.overlappingLinks, $("#rivalFirstCapacityChart"), "first", "capacity", "", activeAirline.id)

	    	$('#linkRivalHistoryModal').fadeIn(200)
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

function switchLinkEventChart($chartContainer) {
    $("#linkEventModal .chart").hide()
    $chartContainer.show()
}

function showLinkRivalDetails(linkId) {
	var url = "airlines/" + activeAirline.id + "/link-rival-details/" + linkId

	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	        updateRivalTables(result)

	    	$('#linkRivalDetailsModal').fadeIn(200)
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

function getLoungeIconSpan(lounge) {
    var $loungeSpan = $('<span style="position:relative"></span>')
    $loungeSpan.append($('<img src="' + 'assets/images/icons/sofa.png' +  '">'))
    $loungeSpan.attr('title', lounge.name + ' at ' + lounge.airportName + ' (' + lounge.airlineName + ' level ' + lounge.level + ')')
    $loungeSpan.append('<div style="position: absolute; right: 0px; bottom: 0px; padding: 0px; vertical-align: middle; color: rgb(69, 69, 68); background-color: rgb(140, 185, 217); font-size: 8px; font-weight: bold;">' + lounge.level + '</div>')
    return $loungeSpan
}

function updateRivalTables(result) {
    var appealTable = $("#rivalAppealTable")
    var networkCapacityTable = $("#networkCapacity")
    appealTable.children(".table-row").remove()
    networkCapacityTable.children(".table-row").remove()
    var fromAirportText = getAirportText(result.fromCity, result.fromAirportCode)
    var toAirportText = getAirportText(result.toCity, result.toAirportCode)
    $("#linkRivalDetailsModal .fromAirportText").text(fromAirportText)
    $("#linkRivalDetailsModal .toAirportText").text(toAirportText)


	var airlineNameById = {}
	var fromAirportLoyalty = {}
	var toAirportLoyalty = {}
	var fromAirportCapacity = {}
    var toAirportCapacity = {}
    var fromAirportLounge = {}
    var toAirportLounge = {}

    $.each(result.fromAirport, function(index, entry) {
	     var airlineId = entry.airline.id
	     airlineNameById[airlineId] = entry.airline.name
	     fromAirportLoyalty[airlineId] = entry.loyalty
	     fromAirportCapacity[airlineId] = { "economy" : entry.network.economy, "business" : entry.network.business, "first" : entry.network.first}
	     if (entry.lounge) {
            fromAirportLounge[airlineId] = entry.lounge
	     }
     })

    $.each(result.toAirport, function(index, entry) {
         var airlineId = entry.airline.id
         toAirportLoyalty[airlineId] = entry.loyalty
         toAirportCapacity[airlineId] = { "economy" : entry.network.economy, "business" : entry.network.business, "first" : entry.network.first}
         if (entry.lounge) {
             toAirportLounge[airlineId] = entry.lounge
         }
    })


    var fullHeartSource = "assets/images/icons/heart.png"
    var halfHeartSource = "assets/images/icons/heart-half.png"
    var emptyHeartSource = "assets/images/icons/heart-empty.png"
    var greenManSource = "assets/images/icons/man-green.png"
    var blueManSource = "assets/images/icons/man-blue.png"
    var yellowManSource = "assets/images/icons/man-yellow.png"
    $.each(airlineNameById, function(airlineId, airlineName) {
     	var row = $("<div class='table-row'></div>")
     	var $airlineSpan = $(getAirlineSpan(airlineId, airlineName))
     	if (fromAirportLounge[airlineId]) {
     	    $airlineSpan.append(getLoungeIconSpan(fromAirportLounge[airlineId]))
     	}
     	if (toAirportLounge[airlineId]) {
            $airlineSpan.append(getLoungeIconSpan(toAirportLounge[airlineId]))
        }
        var $airlineCell = $("<div class='cell' align='left'></div>").append($airlineSpan)
		row.append($airlineCell)
		getPaddedHalfStepImageBarByValue(fullHeartSource, halfHeartSource, emptyHeartSource, 10, fromAirportLoyalty[airlineId].toFixed(2)).appendTo($("<div class='cell' align='right'></div>").appendTo(row))
		getPaddedHalfStepImageBarByValue(fullHeartSource, halfHeartSource, emptyHeartSource, 10, toAirportLoyalty[airlineId].toFixed(2)).appendTo($("<div class='cell' align='right'></div>").appendTo(row))
		appealTable.append(row)

		row = $("<div class='table-row'></div>")

		row.append("<div class='cell' align='left'>" + getAirlineSpan(airlineId, airlineName) + "</div>")
        getCapacityImageBar(greenManSource, fromAirportCapacity[airlineId].economy, "economy").appendTo($("<div class='cell' align='right'></div>").appendTo(row))
        getCapacityImageBar(blueManSource, fromAirportCapacity[airlineId].business, "business").appendTo($("<div class='cell' align='right'></div>").appendTo(row))
        getCapacityImageBar(yellowManSource, fromAirportCapacity[airlineId].first, "first").appendTo($("<div class='cell' align='right'></div>").appendTo(row))
        getCapacityImageBar(greenManSource, toAirportCapacity[airlineId].economy, "economy").appendTo($("<div class='cell' align='right'></div>").appendTo(row))
        getCapacityImageBar(blueManSource, toAirportCapacity[airlineId].business, "business").appendTo($("<div class='cell' align='right'></div>").appendTo(row))
        getCapacityImageBar(yellowManSource, toAirportCapacity[airlineId].first, "first").appendTo($("<div class='cell' align='right'></div>").appendTo(row))

        networkCapacityTable.append(row)
	});

}

function getPaddedHalfStepImageBarByValue(fullStepImageSrc, halfStepImageSrc, emptyStepImageSrc, halfStepAmount, value) {
    var containerDiv = $("<div>")
	containerDiv.prop("title", value)

    var halfSteps = Math.floor(value / halfStepAmount)
    var fullSteps = Math.floor(halfSteps / 2)
    var hasRemainder = halfSteps % 2;
    for (i = 0 ; i < fullSteps ; i ++) {
		var image = $("<img src='" + fullStepImageSrc + "'>")
		containerDiv.append(image)
    }
    if (hasRemainder && halfStepImageSrc) {
        var image = $("<img src='" + halfStepImageSrc + "'>")
    	containerDiv.append(image)
    }
    if (emptyStepImageSrc && halfSteps == 0 && fullSteps == 0) {
        var image = $("<img src='" + emptyStepImageSrc + "'>")
        containerDiv.append(image)
    }

    return containerDiv
}

function getHalfStepImageBarByValue(fullStepImageSrc, halfStepImageSrc, halfStepAmount, value) {
    return getPaddedHalfStepImageBarByValue(fullStepImageSrc, halfStepImageSrc, null, halfStepAmount, value)
}

function getCapacityImageBar(imageSrc, value, linkClass) {
    var containerDiv = $("<div>")
	containerDiv.prop("title", value)

    if (linkClass == "business") {
        value *= 5
    } else if (linkClass == "first") {
        value *= 20
    }
    var count;
    if (value >= 200000) {
        count = 10
    } else if (value >= 100000) {
        count = 9
    } else if (value >= 50000) {
        count = 8
    } else if (value >= 30000) {
        count = 7
    } else if (value >= 20000) {
        count = 6
    } else if (value >= 10000) {
        count = 5
    } else if (value >= 8000) {
        count = 4
    } else if (value >= 5000) {
        count = 3
    } else if (value >= 2000) {
        count = 2
    } else if (value > 0) {
        count = 1
    } else {
        count = 0
    }

    for (i = 0 ; i < count ; i ++) {
		var image = $("<img src='" + imageSrc + "'>")
		containerDiv.append(image)
    }

    return containerDiv
}

function updateSatisfaction(result) {
    var linkClassSatisfaction = result.linkClassSatisfaction
    var preferenceSatisfaction = result.preferenceSatisfaction
    $('#linkCompositionModal .linkClassSatisfaction .table-row').remove()
    $('#linkCompositionModal .preferenceSatisfaction .table-row').remove()
    $('#linkCompositionModal .positiveComments .table-row').remove()
    $('#linkCompositionModal .negativeComments .table-row').remove()
    var topPositiveCommentsByClass = result.topPositiveCommentsByClass
    var topNegativeCommentsByClass = result.topNegativeCommentsByClass
    var topPositiveCommentsByPreference = result.topPositiveCommentsByPreference
    var topNegativeCommentsByPreference = result.topNegativeCommentsByPreference

    $.each(linkClassSatisfaction, function(index, entry) {
        $row = $("<div class='table-row data-row'><div class='cell' style='width: 70%; vertical-align: middle;'>" + entry.title + "</div></div>")
        var $icon = getSatisfactionIcon(entry.satisfaction)
        $icon.on('mouseover.breakdown', function() {
            showSatisfactionBreakdown($(this), topPositiveCommentsByClass[entry.level], topNegativeCommentsByClass[entry.level], entry.satisfaction)
        })

        $icon.on('mouseout.breakdown', function() {
            $('#satisfactionDetailsTooltip').hide()
        })
        $iconCell = $("<div class='cell' style='width: 30%;'>").append($icon)
        $row.append($iconCell)

        $('#linkCompositionModal .linkClassSatisfaction').append($row)
    })
    $.each(preferenceSatisfaction, function(index, entry) {
        $row = $("<div class='table-row data-row'><div class='cell' style='width: 70%; vertical-align: middle;'>" + entry.title + "</div></div>")
        var $icon = getSatisfactionIcon(entry.satisfaction)
        $icon.on('mouseover.breakdown', function() {
            showSatisfactionBreakdown($(this), topPositiveCommentsByPreference[entry.id], topNegativeCommentsByPreference[entry.id], entry.satisfaction)
        })

        $icon.on('mouseout.breakdown', function() {
            $('#satisfactionDetailsTooltip').hide()
        })

        $iconCell = $("<div class='cell' style='width: 30%;'>").append($icon)
        $row.append($iconCell)

        $('#linkCompositionModal .preferenceSatisfaction').append($row)
    })

    var topPositiveComments = result.topPositiveComments
    var topNegativeComments = result.topNegativeComments
    $.each(topPositiveComments, function(index, entry) {
            var percentage = Math.round(entry[1] * 100)
            if (percentage == 0) {
                percentage = "< 1"
            }
            $row = $("<div class='table-row data-row'><div class='cell'>" + entry[0].comment + "</div><div class='cell'>" + percentage + "%</div></div>")
            $('#linkCompositionModal .positiveComments').append($row)
        })
    $.each(topNegativeComments, function(index, entry) {
            var percentage = Math.round(entry[1] * 100)
            if (percentage == 0) {
                percentage = "< 1"
            }
            $row = $("<div class='table-row data-row'><div class='cell'>" + entry[0].comment + "</div><div class='cell'>" + percentage + "%</div></div>")
            $('#linkCompositionModal .negativeComments').append($row)
        })
}

function getSatisfactionIcon(satisfaction) {
    $icon = $('<img>')
    var source
    if (satisfaction < 0.25) {
        source = "symbols-on-mouth"
    } else if (satisfaction < 0.3) {
        source = "steam"
    } else if (satisfaction < 0.4) {
        source = "confused"
    } else if (satisfaction < 0.5) {
        source = "expressionless"
    } else if (satisfaction < 0.6) {
        source = "slightly-smiling"
    } else if (satisfaction < 0.7) {
        source = "grinning"
    } else if (satisfaction < 0.8) {
        source = "smiling"
    } else if (satisfaction < 0.9) {
        source = "blowing-a-kiss"
    } else {
        source = "heart-eyes"
    }
    source = 'assets/images/smiley/' + source + '.png'
    $icon.attr('src', source)
    //$icon.attr('title', Math.round(satisfaction * 100) + "%")
    $icon.width('22px')
    $icon.css({ display: "block", margin: "auto"})
    return $icon
}

function showSatisfactionBreakdown($icon, positiveComments, negativeComments, satisfactionValue) {
    var yPos = $icon.offset().top - $(window).scrollTop() + $icon.height() + 5
    var xPos = $icon.offset().left - $(window).scrollLeft() + $icon.width() - $('#appealBonusDetailsTooltip').width() / 2
    $('#satisfactionDetailsTooltip .satisfactionValue').text(Math.round(satisfactionValue * 100) + '%')

    $('#satisfactionDetailsTooltip .table .table-row').remove()
    $.each(positiveComments, function(index, entry) {
        var percentage = Math.round(entry[1] * 100)
        if (percentage == 0) {
            percentage = "< 1"
        }
        var $row = $('<div class="table-row" style="font-size: 15px; text-shadow: 0px 0px 3px rgba(0,0,0,0.5);"><div class="cell">' + entry[0].comment + '</div><div class="cell">' + percentage + '%</div></div>')
        $row.css('color', '#9ACD32')
        $('#satisfactionDetailsTooltip .table').append($row)
    })
    $.each(negativeComments, function(index, entry) {
        var percentage = Math.round(entry[1] * 100)
        if (percentage == 0) {
            percentage = "< 1"
        }
        var $row = $('<div class="table-row"  style="font-size: 15px; text-shadow: 0px 0px 3px rgba(0,0,0,0.5);"><div class="cell">' + entry[0].comment + '</div><div class="cell">' + percentage + '%</div></div>')
        $row.css('color', '#F08080')
        $('#satisfactionDetailsTooltip .table').append($row)
    })

     //adjust xPos if it's outside of the screen
    var windowWidth = $(window).width()
    var tooltipWidth = $('#satisfactionDetailsTooltip').width()
    if (xPos + tooltipWidth > windowWidth) {
        xPos = windowWidth - tooltipWidth
    }

    $('#satisfactionDetailsTooltip').css('top', yPos + 'px')
    $('#satisfactionDetailsTooltip').css('left', xPos + 'px')

    $('#satisfactionDetailsTooltip').show()
}


function updateTopCountryComposition(countryComposition) {
	countryComposition = countryComposition.sort(function (a, b) {
	    return b.passengerCount - a.passengerCount 
	});
	
	var max = 5;
	var index = 0;
	$('#linkCompositionModal .topCountryTable .table-row').remove()
	$.each(countryComposition, function(key, entry) {
		$('#linkCompositionModal .topCountryTable').append("<div class='table-row data-row'><div class='cell' style='width: 70%;'>" + getCountryFlagImg(entry.countryCode) + entry.countryName
	 			   + "</div><div class='cell' style='width: 30%; text-align: right;'>" + commaSeparateNumber(entry.passengerCount) + "</div></div>")
		index ++;
		if (index >= max) {
			return false;
		}
	});
}

function updateTopAirportComposition($container, airportComposition) {
	var maxPerColumn = 10;
	var index = 0;
	$container.empty()
	var $table
	$.each(airportComposition, function(index, entry) {
	    if (index % maxPerColumn == 0) { //flush a column (a table)
	        $table = $('<div class="table data" style="flex : 1; min-width: 200px;"></div>').appendTo($container)
	    }
		$table.append("<div class='table-row data-row'><div class='cell' style='width: 70%;'>" + getCountryFlagImg(entry.countryCode) + entry.airport
	 			   + "</div><div class='cell' style='width: 30%; text-align: right;'>" + commaSeparateNumber(entry.passengerCount) + "</div></div>")
	});
}

function updatePassengerTypeComposition(typeComposition) {
	typeComposition = typeComposition.sort(function (a, b) {
	    return b.passengerCount - a.passengerCount 
	});
	
	$('#linkCompositionModal .passengerTypeTable .table-row').remove()
	$.each(typeComposition, function(key, entry) {
		$('#linkCompositionModal .passengerTypeTable').append("<div class='table-row data-row'><div class='cell' style='width: 70%;'>" + entry.title
	 			   + "</div><div class='cell' style='width: 30%; text-align: right;'>" + commaSeparateNumber(entry.passengerCount) + "</div></div>")
	});
}

function updatePreferenceTypeComposition(preferenceComposition) {
	preferenceComposition = preferenceComposition.sort(function (a, b) {
	    return b.passengerCount - a.passengerCount 
	});
	
	$('#linkCompositionModal .preferenceTypeTable .table-row').remove()
	$.each(preferenceComposition, function(key, entry) {
		$('#linkCompositionModal .preferenceTypeTable').append("<div class='table-row data-row'><div class='cell' style='width: 70%;'>" + entry.title
	 			   + "</div><div class='cell' style='width: 30%; text-align: right;'>" + commaSeparateNumber(entry.passengerCount) + "</div></div>")
	});
}


function updateAirlineBaseList(airlineId, table) {
	table.children('.table-row').remove()

	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/bases",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(bases) {
	    	var hasHeadquarters = false
	    	var hasBases = false
	    	$(bases).each(function(index, base) {
	    		var row = $("<div class='table-row'></div>")
	    		hasBases = true
	    		if (base.headquarter) {
	    			row.append("<div class='cell'><img src='assets/images/icons/building-hedge.png' style='vertical-align:middle;'><span>(" + base.scale + ")</span></div><div class='cell'>" + getCountryFlagImg(base.countryCode) + getAirportText(base.city, base.airportCode) + "</div>")
	    			table.prepend(row)
	    		} else {
	    			row.append("<div class='cell'><img src='assets/images/icons/building-low.png' style='vertical-align:middle;'><span>(" + base.scale + ")</span></div><div class='cell'>" + getCountryFlagImg(base.countryCode) + getAirportText(base.city, base.airportCode) + "</div>")
	    			table.append(row)
	    		}
	    	})
	    	var emptyRow = $("<div class='table-row'></div>")
			emptyRow.append("<div class='cell'>-</div>")

			if (!hasBases) {
    			table.append(emptyRow)
    		}

	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

var assignedDelegates = 0
var availableDelegates = 0
var negotiationOddsLookup

function linkConfirmation() {
	$('#linkConfirmationModal div.existing').empty()
	$('#linkConfirmationModal div.updating').empty()
	$('#linkConfirmationModal div.controlButtons').hide()
	$('#linkConfirmationModal .negotiationIcons').empty()
	$('#linkConfirmationModal .negotiationBar').empty()
	//$('#linkConfirmationModal .modal-content').css("height", 600)
	$('#linkConfirmationModal div.negotiationInfo').hide()

	var fromAirportId = parseInt($("#planLinkFromAirportId").val())
    var toAirportId = parseInt($("#planLinkToAirportId").val())
    loadAirportImages(fromAirportId, toAirportId)

	if (existingLink) {
		//existing link section
		$('#linkConfirmationModal .modalHeader').text('Update Route')
		$('#linkConfirmationModal div.existing.model').text(existingLink.modelName)
		$('#linkConfirmationModal div.existing.duration').text(getDurationText(existingLink.duration))

		refreshAssignedAirplanesBar($('#linkConfirmationModal div.existingLink .airplanes'), existingLink.assignedAirplanes)

        var existingFrequency = existingLink.future ? existingLink.future.frequency : existingLink.frequency
		for (i = 0 ; i < existingFrequency ; i ++) {
			var image = $("<img>")
			image.attr("src", $(".frequencyBar").data("fillIcon"))
			$('#linkConfirmationModal div.existing.frequency').append(image)
			if ((i + 1) % 10 == 0) {
				$('#linkConfirmationModal div.existing.frequency').append("<br/>")
			}
		}

		var existingCapacity = $('<span>' + toLinkClassValueString(existingLink.capacity) + '</span>')
		$("#linkConfirmationModal div.existing.capacity").append(existingCapacity)
		if (existingLink.future) {
		    var futureCapacity = $('<div class="future">(' + toLinkClassValueString(existingLink.future.capacity) + ')</div>')
		    $("#linkConfirmationModal div.existing.capacity").append(futureCapacity)
		}

		$('#linkConfirmationModal div.existing.price').text(toLinkClassValueString(existingLink.price, '$'))
	} else {
	    $('#linkConfirmationModal .modalHeader').text('New Route')
		$('#linkConfirmationModal div.existing').text('-')
	}

	//update link section
	var updateLinkModel = planLinkInfoByModel[$("#planLinkModelSelect").val()]
	$('#linkConfirmationModal div.updating.model').text(updateLinkModel.modelName)
	$('#linkConfirmationModal div.updating.duration').text(getDurationText(updateLinkModel.duration))

	var assignedAirplaneFrequencies = [] //[(airplane, frequency)]
    $('#planLinkDetails .frequencyDetail').find('.airplaneRow').each(function(index, airplaneRow) {
        var airplane = $(airplaneRow).data("airplane")
        var frequency = parseInt($(airplaneRow).find('.frequency').val())
        assignedAirplaneFrequencies.push({"airplane" : airplane, "frequency" : frequency})
    })

	refreshAssignedAirplanesBar($('#linkConfirmationModal div.updating.airplanes'), assignedAirplaneFrequencies)

    var planInfo = getPlanLinkCapacity()
    var planFrequency = planInfo.future ? planInfo.future.frequency : planInfo.current.frequency

	for (i = 0 ; i < planFrequency ; i ++) {
		var image = $("<img>")
		image.attr("src", $(".frequencyBar").data("fillIcon"))
		$('#linkConfirmationModal div.updating.frequency').append(image)
		if ((i + 1) % 10 == 0) {
			$('#linkConfirmationModal div.updating.frequency').append("<br/>")
		}
	}

	var planCapacitySpan = $('<span>' + toLinkClassValueString(planInfo.current.capacity) + '</span>')
    $("#linkConfirmationModal div.updating.capacity").append(planCapacitySpan)
    if (planInfo.future) {
        var futureCapacitySpan = $('<div class="future">(' + toLinkClassValueString(planInfo.future.capacity) + ')</div>')
        $("#linkConfirmationModal div.updating.capacity").append(futureCapacitySpan)
    }



	$('#linkConfirmationModal div.updating.price').text('$' + $('#planLinkEconomyPrice').val() + " / $" + $('#planLinkBusinessPrice').val() + " / $" + $('#planLinkFirstPrice').val())

	$('#linkConfirmationModal').fadeIn(200)

    getLinkNegotiation()
}

function loadAirportImages(fromAirportId, toAirportId) {
    loadAirportImage(fromAirportId, $('#linkConfirmationModal img.fromAirport') )
    loadAirportImage(toAirportId, $('#linkConfirmationModal img.toAirport'))
}

function loadAirportImage(airportId, $imgContainer) {
	var url = "airports/" + airportId + "/images"
	var genericImageUrl = "assets/images/background/town.png"
	$imgContainer.attr('src', genericImageUrl)
	$imgContainer.addClass('blur')


	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	        var imageUrl
	        if (result.cityImageUrl) {
	            imageUrl = result.cityImageUrl
	        } else if (result.airportImageUrl) {
                imageUrl = result.airportImageUrl
	        } else {

	        }

	        if (imageUrl) {
	            $imgContainer.attr('src', imageUrl)
            }
            $imgContainer.removeClass('blur')
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

function changeAssignedDelegateCount(delta) {
    if (!isNaN(negotiationOddsLookup[assignedDelegates + delta])) {
       updateAssignedDelegateCount(assignedDelegates + delta)
    }
}

function changeAssignedDelegateCountToMax() {
    for (i = availableDelegates; i >= 0 ; i --) {
        if (!isNaN(negotiationOddsLookup[i])) {
            updateAssignedDelegateCount(i)
            break
        }
    }
}

function updateAssignedDelegateCount(delegateCount) {
    assignedDelegates = delegateCount
    $('#linkConfirmationModal div.assignedDelegatesIcons').empty()
    if (assignedDelegates == 0) {
        $('#linkConfirmationModal div.assignedDelegatesIcons').append("<span>None</span>")
    }
    for (i = 0 ; i < assignedDelegates; i ++) {
        var delegateIcon = $('<img src="assets/images/icons/user-silhouette-available.png" title="Assigned Delegate"/>')
        $('#linkConfirmationModal .assignedDelegatesIcons').append(delegateIcon)
    }
    //look up the odds
    var odds = negotiationOddsLookup[assignedDelegates]
    $('#linkConfirmationModal .successRate').text(Math.floor(odds * 100) + '%')

    if (odds <= 0) { //then need to add delegates
        disableButton($('#linkConfirmationModal .negotiateButton'), "Odds at 0%. Assign more delegates")
    } else {
        enableButton($('#linkConfirmationModal .negotiateButton'))
    }

}



function getLinkNegotiation(callback) {
    assignedDelegates = 0
    availableDelegates = 0
    negotiationOddsLookup = {}
    var airlineId = activeAirline.id
    var url = "airlines/" + activeAirline.id + "/get-link-negotiation"

	var linkData = {
    			"fromAirportId" : parseInt($("#planLinkFromAirportId").val()),
    			"toAirportId" : parseInt($("#planLinkToAirportId").val()),
    			//"airplanes" : $("#planLinkAirplaneSelect").val().map(Number),
    			airplanes : getAssignedAirplaneFrequencies(),
    			"airlineId" : airlineId,
    			//"configuration" : { "economy" : configuration.economy, "business" : configuration.business, "first" : configuration.first},
    			"price" : { "economy" : parseInt($("#planLinkEconomyPrice").val()), "business" : parseInt($("#planLinkBusinessPrice").val()), "first" : parseInt($("#planLinkFirstPrice").val())},
    			//"frequency" : parseInt($("#planLinkFrequency").val()),
    			"model" : parseInt($("#planLinkModelSelect").val()),
    			"rawQuality" : parseInt($("#planLinkServiceLevel").val()) * 20}

    $.ajax({
		type: 'POST',
		url: url,
		data: JSON.stringify(linkData),
		contentType: 'application/json; charset=utf-8',
		dataType: 'json',
	    success: function(result) {
	        if (callback) {
	            callback(result)
	        } else {
                var fromAirport = result.fromAirport
                var toAirport = result.toAirport
                $('#negotiationDifficultyModal span.fromAirport').html(getAirportSpan(fromAirport))
                $('#negotiationDifficultyModal span.toAirport').html(getAirportSpan(toAirport))
                $('#linkConfirmationModal .fromAirportText').html(getAirportSpan(fromAirport))
                $('#linkConfirmationModal .toAirportText').html(getAirportSpan(toAirport))

                var negotiationInfo = result.negotiationInfo
                negotiationOddsLookup = negotiationInfo.odds

                if (negotiationInfo.fromAirportRequirements.length > 0 || negotiationInfo.toAirportRequirements.length > 0) {
                    checkTutorial("negotiation")
                    $('#negotiationDifficultyModal div.negotiationInfo .requirement').empty()
                    $('#negotiationDifficultyModal div.negotiationInfo .discount').empty()

                    var currentRow = $('#negotiationDifficultyModal div.negotiationRequirements.fromAirport .table-header')
                    var fromAirportRequirementValue = 0
                    $.each(negotiationInfo.fromAirportRequirements, function(index, requirement) {
                        var sign = requirement.value >= 0 ? '+' : ''
                        currentRow = $('<div class="table-row requirement"><div class="cell">' + requirement.description + '</div><div class="cell">' + sign + requirement.value.toFixed(2) + '</div></div>').insertAfter(currentRow)
                        fromAirportRequirementValue += requirement.value
                    })
                    if (negotiationInfo.fromAirportRequirements.length == 0) {
                        $('<div class="table-row requirement"><div class="cell">-</div><div class="cell">-</div></div>').insertAfter(currentRow)
                    }

                    $('#negotiationDifficultyModal .negotiationRequirementsTotal.fromAirport .total').text(fromAirportRequirementValue.toFixed(2))

                    currentRow = $('#negotiationDifficultyModal div.negotiationRequirements.toAirport .table-header')
                    var toAirportRequirementValue = 0
                    $.each(negotiationInfo.toAirportRequirements, function(index, requirement) {
                        var sign = requirement.value >= 0 ? '+' : ''
                        currentRow = $('<div class="table-row requirement"><div class="cell">' + requirement.description + '</div><div class="cell">' + sign + requirement.value.toFixed(2) + '</div></div>').insertAfter(currentRow)
                        toAirportRequirementValue += requirement.value
                    })
                    if (negotiationInfo.toAirportRequirements.length == 0) {
                        $('<div class="table-row requirement"><div class="cell">-</div><div class="cell">-</div></div>').insertAfter(currentRow)
                    }


                    $('#negotiationDifficultyModal .negotiationRequirementsTotal.toAirport .total').text(toAirportRequirementValue.toFixed(2))

                    //from airport discounts

                    currentRow = $('#negotiationDifficultyModal div.negotiationFromDiscounts .table-header')
                    $.each(negotiationInfo.fromAirportDiscounts, function(index, discount) {
                        var displayDiscountValue = Math.round(discount.value >= 0 ? discount.value * 100 : discount.value * -100)
                        currentRow = $('<div class="table-row discount"><div class="cell">' + discount.description + '</div><div class="cell discountValue">' + displayDiscountValue + '%</div></div>').insertAfter(currentRow)
                        if (discount.value < 0) {
                            currentRow.find('.discountValue').addClass('warning')
                        }
                    })

                    if (negotiationInfo.fromAirportDiscounts.length == 0) {
                        $('<div class="table-row discount"><div class="cell">-</div><div class="cell">-</div></div>').insertAfter(currentRow)
                    }

                    //to airport discounts
                    currentRow = $('#negotiationDifficultyModal div.negotiationToDiscounts .table-header')
                    $.each(negotiationInfo.toAirportDiscounts, function(index, discount) {
                        var displayDiscountValue = Math.round(discount.value >= 0 ? discount.value * 100 : discount.value * -100)
                        currentRow = $('<div class="table-row discount"><div class="cell">' + discount.description + '</div><div class="cell discountValue">' + displayDiscountValue + '%</div></div>').insertAfter(currentRow)
                        if (discount.value < 0) {
                            currentRow.find('.discountValue').addClass('warning')
                        }
                    })

                    if (negotiationInfo.toAirportDiscounts.length == 0) {
                        $('<div class="table-row discount"><div class="cell">-</div><div class="cell">-</div></div>').insertAfter(currentRow)
                    }

                    var fromDiscount = negotiationInfo.finalFromDiscountValue
                    var displayDiscountValue = Math.round(fromDiscount >= 0 ? fromDiscount * 100 : fromDiscount * -100)
                    $('#negotiationDifficultyModal .negotiationDiscountTotal.fromAirport .total').text(displayDiscountValue + "%")
                    if (fromDiscount < 0) {
                        $('#negotiationDifficultyModal .negotiationDiscountTotal.fromAirport .total').addClass('warning')
                    } else {
                        $('#negotiationDifficultyModal .negotiationDiscountTotal.fromAirport .total').removeClass('warning')
                    }
                    var toDiscount = negotiationInfo.finalToDiscountValue
                    displayDiscountValue = Math.round(toDiscount >= 0 ? toDiscount * 100 : toDiscount * -100)
                    $('#negotiationDifficultyModal .negotiationDiscountTotal.toAirport .total').text(displayDiscountValue + "%")
                    if (toDiscount < 0) {
                        $('#negotiationDifficultyModal .negotiationDiscountTotal.toAirport .total').addClass('warning')
                    } else {
                        $('#negotiationDifficultyModal .negotiationDiscountTotal.toAirport .total').removeClass('warning')
                    }

                    //total difficulty after discount
                    var difficultyTotalText = fromAirportRequirementValue.toFixed(2) + " * " + Math.round((1 - fromDiscount) * 100) + "% + " + toAirportRequirementValue.toFixed(2) + " * " + Math.round((1 - toDiscount) * 100) + "% = " + negotiationInfo.finalRequirementValue.toFixed(2)
                    $('#linkConfirmationModal .negotiationInfo .negotiationDifficultyTotal').text(negotiationInfo.finalRequirementValue.toFixed(2))

                    var delegateInfo = result.delegateInfo
                    availableDelegates = delegateInfo.availableCount
                    if (negotiationInfo.finalRequirementValue > availableDelegates) {
                        $('#linkConfirmationModal .negotiationInfo img.info').hide();
                        difficultyTotalText += ' (Not enough available delegates)'
                        $('#linkConfirmationModal .negotiationInfo .error').show();
                    } else if (negotiationInfo.finalRequirementValue > 10) {
                        $('#linkConfirmationModal .negotiationInfo img.info').hide();
                        difficultyTotalText += ' (Too difficult to negotiate)'
                        $('#linkConfirmationModal .negotiationInfo .error').show();
                    } else {
                        $('#linkConfirmationModal .negotiationInfo .error').hide();
                        $('#linkConfirmationModal .negotiationInfo img.info').show();
                    }

                    $('#negotiationDifficultyModal .negotiationInfo .negotiationDifficultyTotal').text(difficultyTotalText)

                    //finish updating the negotiationDifficultyModal

                    refreshAirlineDelegateStatus($('#linkConfirmationModal div.delegateStatus'), delegateInfo)

                    if (availableDelegates > 0) {
                        updateAssignedDelegateCount(1)
                    } else {
                        updateAssignedDelegateCount(0)
                    }

                    if (result.rejection) {
                        $('#linkConfirmationModal div.negotiationInfo .rejection .reason').text(result.rejection)
                        $('#linkConfirmationModal div.negotiationInfo .rejection').css('display', 'flex')
                        $('#linkConfirmationModal .negotiateButton').hide()
                    } else {
                        $('#linkConfirmationModal div.negotiationInfo .rejection').hide()
                        $('#linkConfirmationModal .negotiateButton').show()
                    }

                    $('#linkConfirmationModal .confirmButton').hide()

                    //$('#linkConfirmationModal .modal-content').css("height", 750)
                    $('#linkConfirmationModal div.negotiationInfo').show()
                } else { //then no need for negotiation
                    $('#linkConfirmationModal .negotiateButton').hide()
                    $('#linkConfirmationModal .confirmButton').show()
                }
                $('#linkConfirmationModal div.controlButtons').show()
            }
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}




function refreshAssignedAirplanesBar(container, assignedAirplanes) {
	$(container).empty()

	$.each(assignedAirplanes, function(key, entry) {
		var status
		var airplane = entry.airplane
		var frequency = entry.frequency

		var icon = getAirplaneIcon(airplane)
		icon.css("padding", 0)
		icon.css("float", "left")

		$(container).append(icon)
	})
}

function refreshSavedLink(savedLink) {
	if (!flightPaths[savedLink.id]) { //new link
		//remove temp path
		removeTempPath()
		//draw flight path
		var newPath = drawFlightPath(savedLink)
		selectLinkFromMap(savedLink.id, false)
	}
	setActiveDiv($('#linkDetails'))
    hideActiveDiv($('#extendedPanel #airplaneModelDetails'))
	refreshPanels(activeAirline.id) //refresh panels would update link details


	if ($('#linksCanvas').is(':visible')) { //reload the links table then
		loadLinksTable()
	}
}


function negotiationAnimation(savedLink, callback, callbackParam) {
    var negotiationResult = savedLink.negotiationResult
    $('#negotiationAnimation .negotiationIcons').empty()
	//plotNegotiationGauge($('#negotiationAnimation .negotiationBar'), negotiationResult.passingScore)
	animateProgressBar($('#negotiationAnimation .negotiationBar'), 0, 0)
	$('#negotiationAnimation .negotiationDescriptions').text('')
	$('#negotiationAnimation .negotiationBonus').text('')
	$('#negotiationAnimation .negotiationResult').hide()

    var animation = savedLink.airportAnimation
    if (animation.label) {
        $('#negotiationAnimation .animationLabel').text(animation.label)
    } else {
        $('#negotiationAnimation .animationLabel').empty()
    }

    var animationUrl = animation.url
    if (localStorage.getItem("autoplay") === 'true') {
        animationUrl += "?autoplay=1"
    }
    $('#negotiationAnimation .clip').attr('src', animationUrl)


	var gaugeValue = 0

	var index = 0
	$('#negotiationAnimation .successRate').text(Math.floor(negotiationResult.odds * 100))

	$(negotiationResult.sessions).each( function(index, value) {
        $('#negotiationAnimation .negotiationIcons').append("<img src='assets/images/icons/balloon-ellipsis.png' style='padding : 5px;'>")
    });
	var animationInterval = setInterval(function() {
        var value = $(negotiationResult.sessions)[index ++]
        var icon
 		var description
        if (value > 14) {
            icon = "smiley-kiss.png"
            description = "Awesome +" + Math.round(value)
        } else if (value > 11) {
            icon = "smiley-lol.png"
            description = "Great +" + Math.round(value)
        } else if (value > 8) {
            icon = "smiley.png"
            description = "Good +" + Math.round(value)
        } else if (value > 5) {
            icon = "smiley-neutral.png"
            description = "Soso +" + Math.round(value)
        } else if (value > 0) {
            icon = "smiley-sad.png"
            description = "Bad +" + Math.round(value)
        } else {
            icon = "smiley-cry.png"
            description = "Terrible " + Math.round(value)
        }
        $('#negotiationAnimation .negotiationIcons img:nth-child(' + index + ')').attr("src", "assets/images/icons/" + icon)
        $('#negotiationAnimation .negotiationDescriptions').text(description)


        //$('#linkConfirmationModal .negotiationIcons').append("<img src='assets/images/icons/" + icon + "'>")
        gaugeValue += value
        var percentage = gaugeValue / negotiationResult.passingScore * 100

        var callback
        if (index == negotiationResult.sessions.length) {
            callback = function() {
                           var result
                           if (negotiationResult.isGreatSuccess) {
                            result = "Great Success"
                           } else if (negotiationResult.isSuccessful) {
                            result = "Success"
                           } else {
                            result = "Failure"
                           }
                           if (savedLink.negotiationBonus) {
                             $('#negotiationAnimation .negotiationBonus').text(savedLink.negotiationBonus.description)
                           } else if (savedLink.nextNegotiationDiscount) {
                             $('#negotiationAnimation .negotiationBonus').text(savedLink.nextNegotiationDiscount)
                           }

                           $('#negotiationAnimation .negotiationResult .result').text(result)
                           $('#negotiationAnimation .negotiationResult').show()

                            if (negotiationResult.isGreatSuccess) {
                                $('#negotiationAnimation').addClass('transparentBackground')
                                startFirework(20000, savedLink.negotiationBonus.intensity)
                            } else if (negotiationResult.isSuccessful) {
                               showConfetti($("#negotiationAnimation"))
                           }
                       };
        }
        animateProgressBar($('#negotiationAnimation .negotiationBar'), percentage, 500, callback)

        if (index == negotiationResult.sessions.length) {
            clearInterval(animationInterval);
        }
	}, 750)


	if (callback) {
		$('#negotiationAnimation .close, #negotiationAnimation .result').on("click.custom", function() {
		    if (negotiationResult.isGreatSuccess) {
                $('#negotiationAnimation').removeClass('transparentBackground')
                stopFirework()
		    } else if (negotiationResult.isSuccessful) {
                removeConfetti($("#negotiationAnimation"))
            }
            callback(callbackParam)
		})
    } else {
        $('#negotiationAnimation .close, #negotiationAnimation .result').off("click.custom")
    }

    $('#negotiationAnimation .close, #negotiationAnimation .result').on("click.reset", function() {
        // sets the source to nothing, stopping the video
        $('#negotiationAnimation .clip').attr('src','');
    })

	$('#negotiationAnimation').show()
}

function addAirlineTooltip($target, airlineId, slogan, airlineName) {
    var $airlineTooltip = $('<div style="min-width: 150px;"></div>')
    var $liveryImg = $('<img style="max-height: 100px; max-width: 250px; display: none; margin: auto;" loading="lazy">').appendTo($airlineTooltip)
    $liveryImg.attr('src', 'airlines/' + airlineId + "/livery")
    var $sloganDiv =$("<h5></h5>").appendTo($airlineTooltip)
    if (slogan) {
        $sloganDiv.text(slogan)
    } else {
        $sloganDiv.text(airlineName)
    }
    addTooltipHtml($target, $airlineTooltip, {'top' : '100%'})
    $target.on('mouseenter', function() {
        $liveryImg.show()
    })
}


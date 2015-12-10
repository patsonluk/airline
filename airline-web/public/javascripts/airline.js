var flightPaths = {}
var flightMarkers = []
var flightMarkerAnimations = []

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

//remove and re-add all the links
function updateLinksInfo() {
	selectedLink = null
	//remove all animation intervals
	$.each(flightMarkerAnimations, function( key, value ) {
		  window.clearInterval(value)
	});
	//remove all links from UI first
	//remove from Map
	$.each(flightPaths, function( key, value ) {
		$.each(value, function(key2, line) {
			line.setMap(null)
			})
		});
	
	$.each(flightMarkers, function( key, value ) {
		  value.setMap(null)
		});
	
	flightMarkerAnimations = []
	flightPaths = {}
	flightMarkers = []
	
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
	    		insertLinkToList(link, $('#linkList'))
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
     strokeColor: getLinkColor(link),
     strokeOpacity: 0.6,
     strokeWeight: 2
   });
   
   var icon = "assets/images/icons/airplane.png"
   
   addFlightMarker(flightPath, link);
   flightPath.setMap(map)
   
   var shadowPath = new google.maps.Polyline({
	     path: [{lat: link.fromLatitude, lng: link.fromLongitude}, {lat: link.toLatitude, lng: link.toLongitude}],
	     geodesic: true,
	     map: map,
	     strokeColor: getLinkColor(link),
	     strokeOpacity: 0.01,
	     strokeWeight: 15,
	     zIndex: 100
	   });
   shadowPath.addListener('click', function() {
   		loadLinkDetails(link.id)
   });
   
   
   
   flightPaths[link.id] = [flightPath, shadowPath]
}

function refreshFlightPath(link) {
	if (flightPaths[link.id]) {
		$.each(flightPaths[link.id], function(key, line) {
			line.setOptions({ strokeColor : getLinkColor(link)})
		})
		//flightPaths[link.id].setOptions({ strokeColor : getLinkColor(link)})
	}
}

function getLinkColor(link) {
   var profitFactor = link.profit / link.capacity
   var maxProfitFactor = 500
   var minProfitFactor = -500
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
}


//Use the DOM setInterval() function to change the offset of the symbol
//at fixed intervals.
function addFlightMarker(line, link) {
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
			
			flightMarkers.push(marker)
			markersOfThisLink.push(marker)
		})
		
		var count = 0;
		flightMarkerAnimations.push(window.setInterval(function() {
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
		}, 20));
	}
}




function insertLinkToList(link, linkList) {
	linkList.append($("<a href='javascript:void(0)' onclick='loadLinkDetails(" + link.id + ")'></a>").text(link.fromAirportCode + " => " + link.toAirportCode + "(" + parseInt(link.distance) + "km)"))
	linkList.append($("<br/>"))
}

function loadLinkDetails(linkId) {
//	$('#airplaneDetails').hide()
//	$("#linkDetails").show()
	selectedLink = linkId
	setActiveDiv($("#linkDetails"))
	$("#actionLinkId").val(linkId)
	var airlineId = activeAirline.id
	//load link
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/links/" + linkId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(link) {
    		$("#linkFromAirport").text(link.fromAirportName)
	    	$("#linkToAirport").text(link.toAirportName)
	    	$("#linkCurrentPrice").text(link.price)
	    	$("#linkDistance").text(link.distance)
	    	$("#linkQuality").text(link.computedQuality)
	    	$("#linkCurrentCapacity").text(link.capacity)
	    	$("#linkCurrentDetails").show()
	    	$("#linkToAirportId").val(link.toAirportId)
	    	$("#linkFromAirportId").val(link.fromAirportId)
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
	    	} else {
	    		var linkConsumption = linkConsumptions[0]
	    		$("#linkHistoryPrice").text(linkConsumption.price)
		    	$("#linkHistoryCapacity").text(linkConsumption.capacity)
		    	var loadFactor = linkConsumption.soldSeats / linkConsumption.capacity * 100
		    	$("#linkLoadFactor").text(parseInt(loadFactor) + "%")
		    	$("#linkProfit").text("$" + linkConsumption.profit)
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
	selectedLink = null //no refreshing...todo fix this to make it looks better
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/links/" + linkId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(link) {
	    	$("#planLinkFromAirportName").text(link.fromAirportName)
	    	$("#planLinkToAirportName").text(link.toAirportName)
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


function planFromAirport(fromAirportId, fromAirportName) {
	$('#planLinkFromAirportId').val(fromAirportId)
	$('#planLinkFromAirportName').text(fromAirportName)
	if ($('#planLinkFromAirportId').val() && $('#planLinkToAirportId').val()) {
		planLink($('#planLinkFromAirportId').val(), $('#planLinkToAirportId').val())
	}
}

function planToAirport(toAirportId, toAirportName) {
	$('#planLinkToAirportId').val(toAirportId)
	$('#planLinkToAirportName').text(toAirportName)
	if (!$('#planLinkFromAirportId').val()) { //set the HQ by default for now
		$('#planLinkFromAirportId').val(activeAirline.headquarterAirport.airportId)
		$('#planLinkFromAirportName').text(activeAirline.headquarterAirport.airportName)
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
		setActiveDiv($('#planLinkDetails'))
	}
}

var planLinkInfo = null
var planLinkInfoByModel = {}
var existingLinkModelId = 0

function updatePlanLinkInfo(linkInfo) {
	$('#planLinkDistance').text(linkInfo.distance)
	if (!linkInfo.existingLink) {
		$('#planLinkPrice').val(linkInfo.suggestedPrice)
		$('#addLinkButton').show()
		$('#updateLinkButton').hide()
	} else {
		$('#planLinkPrice').val(linkInfo.existingLink.price)
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
}
function updateFrequencyBar(airplaneModelId) {
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
	
	if (maxFrequencyByAirplanes <= maxFrequencyFromAirport && maxFrequencyByAirplanes <= maxFrequencyToAirport) { //limited by airplanes
		if (maxFrequencyByAirplanes == 0) {
			frequencyBar.text("No routing allowed, reason: ")
		} else {
			generateImageBar(frequencyBar.data("emptyIcon"), frequencyBar.data("fillIcon"), maxFrequencyByAirplanes, frequencyBar, $("#planLinkFrequency"))
		}
		$("#planLinkLimitingFactor").html("<h6></h6><br/><br/>").text("Limited by airplanes")
	} else if (maxFrequencyFromAirport <= maxFrequencyToAirport && maxFrequencyFromAirport <= maxFrequencyByAirplanes) { //limited by from airport
		if (maxFrequencyFromAirport == 0) {
			frequencyBar.text("No routing allowed, reason: ")
		} else {
			generateImageBar(frequencyBar.data("emptyIcon"), frequencyBar.data("fillIcon"), maxFrequencyFromAirport, frequencyBar, $("#planLinkFrequency"))
		}
		$("#planLinkLimitingFactor").html("<h6></h6><br/><br/>").text("Limited by Departure Airport")
	} else { //limited by to airport
		if (maxFrequencyToAirport == 0) {
			frequencyBar.text("No routing allowed, reason: ")
		} else {
			generateImageBar(frequencyBar.data("emptyIcon"), frequencyBar.data("fillIcon"), maxFrequencyToAirport, frequencyBar, $("#planLinkFrequency"))
		}
		$("#planLinkLimitingFactor").html("<h6></h6><br/><br/>").text("Limited by Destination Airport")
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
		
		if (isCurrentlyAssigned) {
			$("#planLinkFrequency").val(existingLink.frequency)
			$("#planLinkServiceLevel").val(existingLink.rawQuality / 20)
		} else {
			$("#planLinkFrequency").val(1)
			$("#planLinkServiceLevel").val(1)
			$("#planLinkAirplaneSelect").val($("#planLinkAirplaneSelect option:first").val());
		}
		updateFrequencyBar(airplaneModelId)
	
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
	    var linkData = { 
			"fromAirportId" : parseInt($("#planLinkFromAirportId").val()), 
			"toAirportId" : parseInt($("#planLinkToAirportId").val()),
			"airplanes" : $("#planLinkAirplaneSelect").val().map(Number),
			"airlineId" : airlineId,
			"price" : parseFloat($("#planLinkPrice").val()),
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
		    	updateAllPanels(activeAirline.id)
		    	if (savedLink.id) {
		    		loadLinkDetails(savedLink.id)
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
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
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
	
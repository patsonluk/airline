var flightPaths

function loadAirlines() {
	$.ajax({
		type: 'GET',
		url: "http://localhost:9001/airlines",
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
	setWebSocketAirlineId(airlineId)
	updateAllPanels(airlineId)
}

function updateLinksInfo() {
	//remove all links from UI first
	//remove from Map
	$.each(flightPaths, function( key, value ) {
		  value.setMap(null)
		});
	flightPaths = []
	//remove from link list
	$('#linkList').empty()

	//remove link details
	//$("#linkDetails").hide()
	
	var url;
	if ($('#airlineOption').val()) {
		url = "http://localhost:9001/airlines/" + $('#airlineOption').val() + "/links?getProfit=true"
	} else {
		url = "http://localhost:9001/links?getProfit=true"
	}
	
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


function drawFlightPath(link) {
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
	   redHex = 255 * (1 - (profitFactor / maxProfitFactor)) 
   } else { 
	   redHex = 255 
   }
   var greenHex
   if (profitFactor < 0) { 
	   greenHex = 255 * (1 + (profitFactor / maxProfitFactor)) 
   } else { 
	   greenHex = 255 
   }
   
   var colorHex = "#" + parseInt(redHex).toString(16) + parseInt(greenHex).toString(16) + "20"
   console.log(profitFactor +  " : " + colorHex)
   
   var flightPath = new google.maps.Polyline({
     path: [{lat: link.fromLatitude, lng: link.fromLongitude}, {lat: link.toLatitude, lng: link.toLongitude}], 
     geodesic: true,
     strokeColor: colorHex,
     strokeOpacity: 1.0,
     strokeWeight: 2
                           });
   flightPath.setMap(map)
   flightPaths.push(flightPath)
}

function insertLinkToList(link, linkList) {
	linkList.append($("<a href='javascript:void(0)' onclick='loadLinkDetails(" + link.id + ")'></a>").text(link.fromAirportCode + " => " + link.toAirportCode + "(" + parseInt(link.distance) + "km)"))
	linkList.append($("<br/>"))
}

function loadLinkDetails(linkId) {
	$('#airplaneDetails').hide()
	$("#linkDetails").show()
	$("#actionLinkId").val(linkId)
	//load link
	$.ajax({
		type: 'GET',
		url: "http://localhost:9001/links/id/" + linkId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(link) {
    		$("#linkFromAirport").text(link.fromAirportName)
	    	$("#linkToAirport").text(link.toAirportName)
	    	$("#linkCurrentPrice").text(link.price)
	    	$("#linkDistance").text(link.distance)
	    	$("#linkCurrentCapacity").text(link.capacity)
	    	$("#linkCurrentDetails").show()
	    	
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	//load history
	$.ajax({
		type: 'GET',
		url: "http://localhost:9001/link-consumptions/id/" + linkId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(linkConsumption) {
	    	if (jQuery.isEmptyObject(linkConsumption)) {
	    		$("#linkHistoryPrice").text("-")
		    	$("#linkHistoryCapacity").text("-")
		    	$("#linkLoadFactor").text("-")
		    	$("#linkProfit").text("-")
	    	} else {
	    		$("#linkHistoryPrice").text(linkConsumption.price)
		    	$("#linkHistoryCapacity").text(linkConsumption.capacity)
		    	var loadFactor = linkConsumption.soldSeats / linkConsumption.capacity * 100
		    	$("#linkLoadFactor").text(parseInt(loadFactor) + "%")
		    	$("#linkProfit").text("$" + linkConsumption.profit)
	    	}
	    	$("#linkHistoryDetails").show()
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
		planLink($('#airlineOption').val(), $('#planLinkFromAirportId').val(), $('#planLinkToAirportId').val())
	}
}

function planToAirport(toAirportId, toAirportName) {
	$('#planLinkToAirportId').val(toAirportId)
	$('#planLinkToAirportName').text(toAirportName)
	if ($('#planLinkFromAirportId').val() && $('#planLinkToAirportId').val()) {
		planLink($('#airlineOption').val(), $('#planLinkFromAirportId').val(), $('#planLinkToAirportId').val())
	}
}


function planLink(airlineId, fromAirport, toAirport) {
	if (fromAirport && toAirport) {
		var url = "http://localhost:9001/plan-link"
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
	}
}

var airplanesWithPlanRouteInfo = {}

function updatePlanLinkInfo(linkInfo) {
	$('#planLinkDistance').text(linkInfo.distance)
	$('#planLinkPrice').val(linkInfo.suggestedPrice)
	$("#planLinkAirplaneSelect").empty()
	airplanesWithPlanRouteInfo = {}
	$.each(linkInfo.freeAirplanes, function( key, airplaneWithPlanRouteInfo ) {
		$("#planLinkAirplaneSelect").append($("<option></option>").attr("value", airplaneWithPlanRouteInfo.id).text(airplaneWithPlanRouteInfo.name + " id " + airplaneWithPlanRouteInfo.id));
   		airplanesWithPlanRouteInfo[airplaneWithPlanRouteInfo.id] = airplaneWithPlanRouteInfo
	});
	updatePlanLinkInfoWithAirplaneSelected($("#planLinkAirplaneSelect").val())
}

function updatePlanLinkInfoWithAirplaneSelected(airplaneId) {
	var airplaneInfo = airplanesWithPlanRouteInfo[airplaneId]
	$('#planLinkDuration').text(airplaneInfo.duration)
	$('#planLinkFrequency').val(airplaneInfo.maxFrequency)
}

function createLink() {
	if ($("#planLinkFromAirportId").val() && $("#planLinkToAirportId").val()) {
		var url = "http://localhost:9001/links"
	    console.log("selected " + $("#planLinkAirplaneSelect").val())
		var linkData = { 
			"fromAirportId" : parseInt($("#planLinkFromAirportId").val()), 
			"toAirportId" : parseInt($("#planLinkToAirportId").val()),
			"airlineId" : parseInt($("#airlineOption").val()),
			"airplanes" : [parseInt($("#planLinkAirplaneSelect").val())], 
			"price" : parseFloat($("#planLinkPrice").val()),
			"frequencyPerAirplane" : parseFloat($("#planLinkFrequency").val())}
		$.ajax({
			type: 'PUT',
			url: url,
		    data: JSON.stringify(linkData),
		    contentType: 'application/json; charset=utf-8',
		    dataType: 'json',
		    success: function(savedLink) {
		    	updateAllPanels(parseInt($("#airlineOption").val()))
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
		url: "http://localhost:9001/links/id/" + linkId,
	    success: function() {
	    	updateLinksInfo()
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
		url: "http://localhost:9001/links",
	    success: function() {
	    	updateLinksInfo()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	
	
}
	
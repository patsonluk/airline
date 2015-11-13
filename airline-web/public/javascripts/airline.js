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
	    		updateAllPanels($("#airlineOption option:first").val())
	    	}
	    	
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
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
	$("#linkDetails").hide()
	
	var url;
	if ($('#airlineOption').val()) {
		url = "http://localhost:9001/airlines/" + $('#airlineOption').val() + "/links"
	} else {
		url = "http://localhost:9001/links"
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
   var flightPath = new google.maps.Polyline({
     path: [{lat: link.fromLatitude, lng: link.fromLongitude}, {lat: link.toLatitude, lng: link.toLongitude}], 
     geodesic: true,
     strokeColor: '#F2B022',
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
	$.ajax({
		type: 'GET',
		url: "http://localhost:9001/link-consumptions/id/" + linkId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(link) {
	    	$("#linkDetails").show()
	    	$("#linkFromAirport").text(link.fromAirportName)
	    	$("#linkToAirport").text(link.toAirportName)
	    	$("#linkPrice").text(link.price)
	    	$("#linkDistance").text(link.distance)
	    	$("#linkCapacity").text(link.capacity)
	    	var loadFactor = link.soldSeats / link.capacity * 100
	    	$("#linkLoadFactor").text(parseInt(loadFactor) + "%")
	    	$("#linkProfit").text("$" + link.profit)
	    	$("#actionLinkId").val(link.linkId)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
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

function updatePlanLinkInfo(linkInfo) {
	$('#planLinkDiv').show()
	$('#planLinkDistanceSpan').text(linkInfo.distance)
	$('#planLinkPrice').val(linkInfo.suggestedPrice)
	$("#planLinkAirplaneSelect").empty()
	$.each(linkInfo.freeAirplanes, function( key, airplane ) {
   		$("#planLinkAirplaneSelect").append($("<option></option>").attr("value", airplane.id).text(airplane.name));
	});
}

function createLink() {
	if ($("#fromAirport").val() && $("#toAirport").val()) {
		var url = "http://localhost:9001/links"
	    console.log("selected " + $("#planLinkAirplaneSelect").val())
		var linkData = { 
			"fromAirportId" : parseInt($("#fromAirport").val()), 
			"toAirportId" : parseInt($("#toAirport").val()),
			"airlineId" : parseInt($("#airlineOption").val()),
			"airplanes" : [parseInt($("#planLinkAirplaneSelect").val())], 
			"price" : parseFloat($("#planLinkPrice").val()) }
		$.ajax({
			type: 'PUT',
			url: url,
		    data: JSON.stringify(linkData),
		    contentType: 'application/json; charset=utf-8',
		    dataType: 'json',
		    success: function(savedLink) {
		    	updateLinksInfo()
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
	
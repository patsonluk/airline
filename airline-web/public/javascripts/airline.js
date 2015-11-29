var flightPaths = []

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
   var colorHex = "#" + redHexString + greenHexString + "20"
   
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
//	$('#airplaneDetails').hide()
//	$("#linkDetails").show()
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
		url: "airlines/" + airlineId + "/link-consumptions/" + linkId,
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

function editLink() {
	$("#planLinkFromAirportName").text($("#linkFromAirport").text())
	$("#planLinkToAirportName").text($("#linkToAirport").text())
	$("#planLinkFromAirportId").val($("#linkFromAirportId").val())
	$("#planLinkToAirportId").val($("#linkToAirportId").val())
	setActiveDiv($('#planLinkDetails'))
	planLink($("#linkFromAirportId").val(), $("#linkToAirportId").val())
}


function planFromAirport(fromAirportId, fromAirportName) {
	$('#planLinkFromAirportId').val(fromAirportId)
	$('#planLinkFromAirportName').text(fromAirportName)
	if ($('#planLinkFromAirportId').val() && $('#planLinkToAirportId').val()) {
		planLink($('#planLinkFromAirportId').val(), $('#planLinkToAirportId').val())
	}
	setActiveDiv($('#planLinkDetails'))
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
	setActiveDiv($('#planLinkDetails'))
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
	
	if (maxFrequencyByAirplanes < maxFrequencyFromAirport && maxFrequencyByAirplanes < maxFrequencyToAirport) { //limited by airplanes
		if (maxFrequencyByAirplanes == 0) {
			frequencyBar.text("No routing allowed, reason: ")
		} else {
			generateImageBar(frequencyBar.data("emptyIcon"), frequencyBar.data("fillIcon"), maxFrequencyByAirplanes, frequencyBar, $("#planLinkFrequency"))
		}
		$("#planLinkLimitingFactor").html("<h6></h6><br/><br/>").text("Limited by airplanes")
	} else if (maxFrequencyFromAirport < maxFrequencyToAirport && maxFrequencyFromAirport < maxFrequencyByAirplanes) { //limited by from airport
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
			"rawQuality" : (parseInt($("#planLinkServiceLevel").val()) + 1) * 20}
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
	
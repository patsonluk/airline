function buildFacility(facility, levelChange) {
	var url = "airlines/" + activeAirline.id + "/facilities/" + activeAirportId 
	var facilityData = { 
			"airportId" : parseInt(activeAirportId),
			"airlineId" : activeAirline.id,
			"level" : facility.level + levelChange,
			"name" : $("#facilityModal .facilityNameInput").val(),
			"type" : facility.type}
	$.ajax({
		type: 'PUT',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    data: JSON.stringify(facilityData),
	    dataType: 'json',
	    success: function(result) {
	    	if (result.nameRejection) {
	    		$('#facilityModal .facitilyNameWarningDiv .warning').text(result.nameRejection)
	    		$('#facilityModal .facitilyNameWarningDiv').show()
	    	} else {
	    		refreshPanels(activeAirline.id)
		    	if (result.level > 0) {
		    		showFacilityModal(result)
		    	}
		    	updateFacilityIcons(activeAirport)
	    	}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function removeFacility(facility) {
	buildFacility(facility, facility.level * -1)
}


function updateFacilityIcons(airport) {
	$('#airportDetailsFacilities').empty()
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/facilities/" + airport.id,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(facilityDetails) {
	    	if (facilityDetails.lounge) { //lounge
	    		var imageUrl
	    		var imageTitle
    			if (facilityDetails.lounge.status == 'ACTIVE') {
    				imageUrl = 'assets/images/icons/sofa.png'
					imageTitle = 'Active Lounge Level ' + facilityDetails.lounge.level + " - " + facilityDetails.lounge.name 	
    			} else {
    				imageUrl = 'assets/images/icons/sofa-grey.png'
    				imageTitle = 'No Active Lounge'
    			}
    			
	    		var iconImg = $("<img class='button' src='" + imageUrl +  "' onclick='showFacilityModal($(this).data(&quot;facility&quot;))' title='" + imageTitle + "'>")
    			//attach to modal
	    		iconImg.data("facility", facilityDetails.lounge)
	    		$('#airportDetailsFacilities').append(iconImg)
    		}	    	
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function showFacilityModal(currentFacility) {
	$('#facilityModal .facitilyNameWarningDiv').hide()
	var facitlityData = { 
			"airportId" : parseInt(currentFacility.airportId),
			"airlineId" : activeAirline.id,
			"name" : currentFacility.name,
			"level" : currentFacility.level,
			"type" : currentFacility.type}
	
	$.ajax({
		type: 'POST',
		url: "airlines/" + activeAirline.id + "/facility-consideration/" + currentFacility.airportId,
		contentType: 'application/json; charset=utf-8',
	    data: JSON.stringify(facitlityData),
	    dataType: 'json',
	    success: function(facilityConsideration) {
	    	$('#facilityModal .facilityType').text("Airport Lounge") //for now only one
	    	
	    	if (currentFacility.level) {
	    		$('#facilityModal .facilityLevel').empty()
	    		$('#facilityModal .facilityLevel').append(getLevelStarsImgs(currentFacility.level, 3))
	    	} else {
	    		$('#facilityModal .facilityLevel').text('-')
	    	}
	    	if (currentFacility.upkeep) {
	    		$('#facilityModal .facilityUpkeep').text('$' + commaSeparateNumber(currentFacility.upkeep))
	    	} else {
	    		$('#facilityModal .facilityUpkeep').text('-')
	    	}
	    	if (currentFacility.cost) {
	    		$('#facilityModal .facilityCost').text('$' + commaSeparateNumber(currentFacility.cost))
	    	} else {
	    		$('#facilityModal .facilityCost').text('-')
	    	}
	    	if (currentFacility.income) {
	    		$('#facilityModal .facilityIncome').text('$' + commaSeparateNumber(currentFacility.income))
	    	} else {
	    		$('#facilityModal .facilityIncome').text('-')
	    	}
	    	if (currentFacility.profit) {
	    		$('#facilityModal .facilityProfit').text('$' + commaSeparateNumber(currentFacility.profit))
	    	} else {
	    		$('#facilityModal .facilityProfit').text('-')
	    	}
	    	if (facilityConsideration.upgrade.cost) {
	    		$('#facilityModal .upgradeCost').text('$' + commaSeparateNumber(facilityConsideration.upgrade.cost))
	    	} else {
	    		$('#facilityModal .upgradeCost').text('-')
	    	}
	    	if (facilityConsideration.upgrade.upkeep) {
	    		$('#facilityModal .upgradeUpkeep').text('$' + commaSeparateNumber(facilityConsideration.upgrade.upkeep))
	    	} else {
	    		$('#facilityModal .upgradeUpkeep').text('-')
	    	}
	    	
	    	if (currentFacility.level == 0) {
	    		$('#facilityModal .facilityNameInput').val('')
	    		if (facilityConsideration.upgrade.rejection) {
	    			$('#facilityModal .facilityName').text('-')
		    		$('#facilityModal .facilityName').show()
		    		$('#facilityModal .facilityNameInputDiv').hide()
	    		} else { //allow inputing name
	    			$('#facilityModal .facilityName').hide()
		    		$('#facilityModal .facilityNameInputDiv').show()
	    		}
	    	} else {
	    		$('#facilityModal .facilityName').text(currentFacility.name)
	    		$('#facilityModal .facilityNameInput').val(currentFacility.name)
	    		$('#facilityModal .facilityName').show()
	    		$('#facilityModal .facilityNameInputDiv').hide()
			}
	    	
	    	if (!facilityConsideration.upgrade.rejection) {
	    		if (currentFacility.level == 0) {
	    			$('#facilityModal .buildButton').show()
	    			$('#facilityModal .upgradeButton').hide()
	    		} else {
	    			$('#facilityModal .buildButton').hide()
	    			$('#facilityModal .upgradeButton').show()
	    		}
	    		$('#facilityModal .upgradeRejectionDiv').hide()
	    	} else {
	    		$('#facilityModal .buildButton').hide()
    			$('#facilityModal .upgradeButton').hide()
    			$('#facilityModal .upgradeRejectionReason').text(facilityConsideration.upgrade.rejection)
    			$('#facilityModal .upgradeRejectionDiv').show()
    			
	    	}
	    	
	    	if (!facilityConsideration.downgrade.rejection) {
	    		if (currentFacility.level >= 2) {
	    			$('#facilityModal .downgradeButton').show()
	    			$('#facilityModal .removeButton').show()
	    		} else {
	    			$('#facilityModal .downgradeButton').hide()
	    			$('#facilityModal .removeButton').show()
	    		}
	    	} else {
	    		$('#facilityModal .downgradeButton').hide()
    			$('#facilityModal .removeButton').hide()
	    	}
	    	
				    	
	    	$('#facilityModal').data("facility", currentFacility)
	    	$('#facilityModal').fadeIn(200)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	
}	

function getLevelStarsImgs(level, maxLevel) {
	var html = ""
	for (i = 0 ; i < level; i ++) {
		html += "<img src='assets/images/icons/star.png'/>"
	}
	for (i = 0 ; i < maxLevel - level; i ++) {
		html += "<img src='assets/images/icons/star-empty.png'/>"
	}
	return html
}


function closeFacilityModal() {
	closeModal($('#facilityModal'))
	showAirportDetails(activeAirportId)
}
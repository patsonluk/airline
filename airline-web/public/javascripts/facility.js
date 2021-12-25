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
		    	} else {
		    	    closeModal($(".modal.facility"))
		    	}
		    	updateFacilityIcons(activeAirport)
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
    			} else if (facilityDetails.lounge.level > 0) {
    				imageUrl = 'assets/images/icons/sofa-grey.png'
    				imageTitle = 'Inactive Lounge Level ' + facilityDetails.lounge.level + " - " + facilityDetails.lounge.name
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

function showLoungeModal(currentFacility) {
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
	    	$('#facilityModal .facilityDescription').text("Serves Business and First class passengers taking flights offered by your airline or your alliance network. You charge alliance members for using of your lounge.")
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
	    		} else { //allow name input
	    			$('#facilityModal .facilityName').hide()
		    		$('#facilityModal .facilityNameInputDiv').show()
	    		}
	    	} else {
	    		$('#facilityModal .facilityName').text(currentFacility.name)
	    		$('#facilityModal .facilityNameInput').val(currentFacility.name)
	    		$('#facilityModal .facilityName').show()
	    		$('#facilityModal .facilityNameInputDiv').hide()
			}

            if (currentFacility.level == 0) {
                $('#facilityModal .buildButton').show()
                $('#facilityModal .upgradeButton').hide()
                $('#facilityModal .downgradeButton').hide()
                $('#facilityModal .removeButton').hide()
            } else if (currentFacility.level == 1){
                $('#facilityModal .buildButton').hide()
                $('#facilityModal .upgradeButton').show()
                $('#facilityModal .downgradeButton').hide()
                $('#facilityModal .removeButton').show()
            } else {
                $('#facilityModal .buildButton').hide()
                $('#facilityModal .upgradeButton').show()
                $('#facilityModal .downgradeButton').show()
                $('#facilityModal .removeButton').show()
            }

	    	if (!facilityConsideration.upgrade.rejection) {
	    	    $('#facilityModal .upgradeRejectionDiv').hide()
	    		enableButton($('#facilityModal .upgradeButton'))
	    		enableButton($('#facilityModal .buildButton'))
	    	} else {
	    		$('#facilityModal .upgradeRejectionDiv').show()
    			disableButton($('#facilityModal .upgradeButton'), facilityConsideration.upgrade.rejection)
    			disableButton($('#facilityModal .buildButton'), facilityConsideration.upgrade.rejection)
	    	}

	    	if (!facilityConsideration.downgrade.rejection) {
	    	    enableButton($('#facilityModal .downgradeButton'))
                enableButton($('#facilityModal .removeButton'))
	    	} else {
    	        disableButton($('#facilityModal .downgradeButton'), facilityConsideration.downgrade.rejection)
    		    disableButton($('#facilityModal .removeButton'), facilityConsideration.downgrade.rejection)
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


function showFacilityModal(currentFacility) {
	if (currentFacility.type == "LOUNGE") {
	    showLoungeModal(currentFacility)
	}
	
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



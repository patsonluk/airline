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
    		if (facilityDetails.shuttleService) { //lounge
                var imageUrl
                var imageTitle
                if (facilityDetails.shuttleService.level > 0) {
                    imageUrl = 'assets/images/icons/shuttle.png'
                    imageTitle = 'Shuttle Service Level ' + facilityDetails.shuttleService.level
                } else {
                    imageUrl = 'assets/images/icons/shuttle-grey.png'
                    imageTitle = 'No Shuttle Service'
                }

                var iconImg = $("<img class='button' src='" + imageUrl +  "' onclick='showFacilityModal($(this).data(&quot;facility&quot;))' title='" + imageTitle + "'>")
                //attach to modal
                iconImg.data("facility", facilityDetails.shuttleService)
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



function showShuttleModal(currentFacility) {
    var facilityData = {
			"airportId" : parseInt(currentFacility.airportId),
			"airlineId" : activeAirline.id,
			"name" : currentFacility.name,
			"level" : currentFacility.level,
			"type" : currentFacility.type}

	$.ajax({
		type: 'POST',
		url: "airlines/" + activeAirline.id + "/facility-consideration/" + currentFacility.airportId,
		contentType: 'application/json; charset=utf-8',
	    data: JSON.stringify(facilityData),
	    dataType: 'json',
	    success: function(facilityConsideration) {
	    	if (currentFacility.level) {
	    		$('#shuttleModal .facilityLevel').empty()
	    		$('#shuttleModal .facilityLevel').append(getLevelStarsImgs(currentFacility.level, 3))
	    		$('#shuttleModal .confirmButton').show()
	    	} else {
	    		$('#shuttleModal .facilityLevel').text('-')
	    		$('#shuttleModal .confirmButton').hide()
	    	}
	    	if (currentFacility.upkeep) {
	    		$('#shuttleModal .facilityUpkeep').text('$' + commaSeparateNumber(currentFacility.upkeep))
	    	} else {
	    		$('#shuttleModal .facilityUpkeep').text('-')
	    	}
	    	if (facilityConsideration.upgrade.cost) {
	    		$('#shuttleModal .upgradeCost').text('$' + commaSeparateNumber(facilityConsideration.upgrade.cost))
	    	} else {
	    		$('#shuttleModal .upgradeCost').text('-')
	    	}


	    	if (!facilityConsideration.upgrade.rejection) {
	    		if (currentFacility.level == 0) {
	    			$('#shuttleModal .buildButton').show()
	    			$('#shuttleModal .upgradeButton').hide()
	    		} else {
	    			$('#shuttleModal .buildButton').hide()
	    			$('#shuttleModal .upgradeButton').show()
	    			enableButton($('#shuttleModal .upgradeButton'))
	    		}
	    		$('#shuttleModal .upgradeRejectionDiv').hide()
	    	} else {
	    		$('#shuttleModal .buildButton').hide()
    			disableButton($('#shuttleModal .upgradeButton'), facilityConsideration.upgrade.rejection)
	    	}

            if (currentFacility.level <= 0) {
                    $('#shuttleModal .downgradeButton').hide()
                    $('#shuttleModal .removeButton').hide()
            } else {
                if (!facilityConsideration.downgrade.rejection) {
                    enableButton($('#shuttleModal .downgradeButton'))
                    enableButton($('#shuttleModal .removeButton'))
                    if (currentFacility.level == 1) {
                        $('#shuttleModal .downgradeButton').hide()
                        $('#shuttleModal .removeButton').show()
                    } else {
                        $('#shuttleModal .downgradeButton').show()
                        $('#shuttleModal .removeButton').show()
                    }
                } else {
                    if (currentFacility.level >= 2) {
                        disableButton($('#shuttleModal .downgradeButton'), facilityConsideration.downgrade.rejection)
                        $('#shuttleModal .downgradeButton').show()
                        $('#shuttleModal .removeButton').hide()
                    } else {
                        disableButton($('#shuttleModal .removeButton'), facilityConsideration.downgrade.rejection)
                        $('#shuttleModal .downgradeButton').hide()
                        $('#shuttleModal .removeButton').show()
                    }
                }
            }

            $('#shuttleModal').data("facility", currentFacility)
            loadShuttleLinks(currentFacility)

	    	$('#shuttleModal').fadeIn(200)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function loadShuttleLinks(currentFacility) {
    var $table = $('#shuttleModal .table.shuttles')
    $table.find('.table-row').remove()

    $.ajax({
        type: 'GET',
        url: "airlines/" + activeAirline.id + "/airport/" + currentFacility.airportId + "/shuttles",
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(shuttles) {
            $table.data('shuttles', shuttles)

            $.each(shuttles, function(index, shuttle) {
                $row = $('<div class="table-row" style="width: 100%"></div>')
                $row.append($('<div class="cell">' + shuttle.toAirportText + '</div>'))
                $row.append($('<div class="cell" align="right">' + commaSeparateNumber(shuttle.toAirportPopulation) + '</div>'))
                $row.append($('<div class="cell capacity" align="right">' + shuttle.capacity + '</div>'))
                $row.append($('<div class="cell" align="right">' + shuttle.passenger + '</div>'))

                var $controlCell = $('<div class="cell"></div>')
                if (currentFacility.level > 0) {
                    $('<div style="float: right; padding: 0 3px;" class="clickable" onclick="changeShuttle($(this), -100)"><img src="assets/images/icons/minus-button.png" title="Decrease Capacity"/></div>').appendTo($controlCell).data('shuttle', shuttle)
                    $('<div style="float: right; padding: 0 3px;" class="clickable" onclick="changeShuttle($(this), 100)"><img src="assets/images/icons/plus-button.png" title="Increase Capacity"/></div>').appendTo($controlCell).data('shuttle', shuttle)
                }
                $row.append($controlCell)

                $table.append($row)
            })
            if (shuttles.length == 0) {
                $table.append('<div class="table-row"><div class="cell">-</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div><div class="cell"></div></div>')
            }

            refreshShuttleSummary()
        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
	});
}

function refreshShuttleSummary() {
    var shuttleService = $('#shuttleModal').data("facility")
    var usedCapacity = 0
    var shuttles = $('#shuttleModal .table.shuttles').data('shuttles')
    var operationCost = 0
    $.each(shuttles, function(index, shuttle) {
        usedCapacity += shuttle.capacity
        operationCost += shuttle.capacity * shuttle.unitCost
    })

    $('#shuttleModal .facilityCost').text("$" + commaSeparateNumber(operationCost))
    $('#shuttleModal .totalCapacity').text(usedCapacity + "/" + shuttleService.capacity)
}

function changeShuttle($button, delta) {
    var shuttle = $button.data('shuttle')
    if (shuttle.capacity + delta < 0) {
        return
    }
    var shuttleService = $('#shuttleModal').data("facility")
    var shuttles = $('#shuttleModal .table.shuttles').data('shuttles')
    var usedCapacity = 0
    var maxCapacity = shuttleService.capacity
    $.each(shuttles, function(index, shuttle) {
        usedCapacity += shuttle.capacity
    })
    if (usedCapacity + delta > maxCapacity) {
        return
    }
    //ok to change
    shuttle.capacity += delta
    $button.closest('.table-row').find('.capacity').text(shuttle.capacity)
    refreshShuttleSummary()
}

function updateShuttles() {
    var shuttleService = $('#shuttleModal').data("facility")
    var shuttles = $('#shuttleModal .table.shuttles').data('shuttles')
    $.ajax({
        type: 'POST',
        url: "airlines/" + activeAirline.id + "/airport/" + shuttleService.airportId + "/shuttles",
        contentType: 'application/json; charset=utf-8',
        data: JSON.stringify(shuttles),
        dataType: 'json',
        success: function(result) {
            closeModal($('#shuttleModal'))
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
    })
}


function showFacilityModal(currentFacility) {
	if (currentFacility.type == "LOUNGE") {
	    showLoungeModal(currentFacility)
	} else if (currentFacility.type == "SHUTTLE") {
	    showShuttleModal(currentFacility)
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



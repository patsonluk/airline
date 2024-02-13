var airportLinkPaths = {}
var activeAirport
var activeAirportId
var activeAirportPopupInfoWindow
var airportMapMarkers = []
var airportMapCircle


function showAirportDetails(airportId) {
    setActiveDiv($("#airportCanvas"))

	//highlightTab($('#airportCanvasTab'))
	
	$('#main-tabs').children('.left-tab').children('span').removeClass('selected')
	//deselectLink()
	checkTutorial('airport')

	activeAirportId = airportId
	$('#airportDetailsAirportImage').empty()
    $('#airportDetailsCityImage').empty()
    $("#airportCanvas .rating").empty()
	
	$.ajax({
		type: 'GET',
		url: "airports/" + airportId + "?image=true",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airport) {
	        populateAirportDetails(airport)
//	    		$("#floatBackButton").show()
//	    		shimmeringDiv($("#floatBackButton"))
            updateAirportDetails(airport, airport.cityImageUrl, airport.airportImageUrl)
            updateAirportExtendedDetails(airport.id, airport.countryCode)
    		activeAirport = airport
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateAirportDetails(airport, cityImageUrl, airportImageUrl) {
	if (cityImageUrl) {
		$('#airportDetailsCityImage').append('<img src="' + cityImageUrl + '" style="width:100%;"/>')
	}
	if (airportImageUrl) {
		$('#airportDetailsAirportImage').append('<img src="' + airportImageUrl + '" style="width:100%;"/>')
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


    var $runwayTable = $('#airportDetails .runwayTable')
    $runwayTable.children('.table-row').remove()
	if (airport.runways) {
	    $.each(airport.runways, function(index, runway) {
        		var row = $("<div class='table-row'></div>")
        		row.append("<div class='cell'>" +  runway.code + "</div>")
        		row.append("<div class='cell'>" + runway.length + "&nbsp;m</div>")
        		row.append("<div class='cell'>" + runway.type + "</div>")
        		$runwayTable.append(row)
        });
	} else {
	    var row = $("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div></div>")
	}
	
	$("#airportDetailsCity").text(airport.city)
    $("#airportDetailsSize").text(airport.size)

    var $populationSpan = getBoostSpan(airport.population, airport.populationBoost, $('#populationDetailsTooltip'))
    $("#airportDetailsPopulation").html($populationSpan)

    var $incomeLevelSpan = getBoostSpan(airport.incomeLevel, airport.incomeLevelBoost, $('#incomeDetailsTooltip'))
    $("#airportDetailsIncomeLevel").html($incomeLevelSpan)

	$("#airportDetailsCountry").attr("onclick", "showCountryView('" + airport.countryCode + "')")
	$("#airportDetailsCountry").attr("data-link", 'country')
	$("#airportDetailsCountry").html("<span>" + loadedCountriesByCode[airport.countryCode].name + '&nbsp;</span>')
	var countryFlagUrl = getCountryFlagUrl(airport.countryCode)
	if (countryFlagUrl) {
		$("#airportDetailsCountry").append("<img src='" + countryFlagUrl + "' />")
	}
	$("#airportDetailsZone").text(zoneById[airport.zone])
	$("#airportDetailsOpenness").html(getOpennessSpan(loadedCountriesByCode[airport.countryCode].openness))
	
	refreshAirportExtendedDetails(airport)
	//updateAirportSlots(airport.id)

	updateAirportChampionDetails(airport)

    $('#airportDetailsStaff').removeClass('fatal')
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
	    			$('#airportDetailsBaseDelegatesRequired').text('-')
	    			$('#airportDetailsStaff').text('-')
	    			$('#airportBaseDetails .baseSpecializations').text('-')
	    			$('#airportDetailsFacilities').empty()
	    			disableButton($('#airportBaseDetails .specialization.button'), "This is not your airline base")

	    			$('#baseDetailsModal').removeData('scale')
	    		} else {
	    			$('#airportDetailsBaseType').text(airportBase.headquarter ? "Headquarters" : "Base")
	    			$('#airportDetailsBaseScale').text(airportBase.scale)
	    			if (airportBase.delegatesRequired == 0) {
	    			    $('#airportDetailsBaseDelegatesRequired').text('None')
                    } else {
                        $('#airportDetailsBaseDelegatesRequired').empty()
                        var $delegatesSpan = $('<span style="display: flex;"></span>')
                        for (i = 0 ; i < airportBase.delegatesRequired; i ++) {
                            var $delegateIcon = $('<img src="assets/images/icons/user-silhouette-available.png"/>')
                            $delegatesSpan.append($delegateIcon)
                        }
                        $('#airportDetailsBaseDelegatesRequired').append($delegatesSpan)
                    }


                    var capacityInfo = baseDetails.officeCapacity
                    var capacityText = capacityInfo.currentStaffRequired + "/" + capacityInfo.staffCapacity
                    var $capacitySpan = $('#airportDetailsStaff')

                    if (capacityInfo.staffCapacity < capacityInfo.currentStaffRequired) {
                        $capacitySpan.addClass('fatal')
                    }

                    if (capacityInfo.currentStaffRequired != capacityInfo.futureStaffRequired) {
                        capacityText += "(future : " + capacityInfo.futureStaffRequired + ")"
                    }
                    $capacitySpan.text(capacityText)


                    if (airportBase.specializations) {
                        var specializationList = $('<span></span>')
                        $.each(airportBase.specializations, function(index, specialization) {
                            //specializationList.append($('<li class="dot">' + specialization.label + '</li>'))
                            specializationList.append($('<img src="assets/images/icons/specialization/' + specialization.id + '.png" title="' + specialization.label + '" style="vertical-align: middle;">'))
                        })
                        $('#airportBaseDetails .baseSpecializations').empty()
                        $('#airportBaseDetails .baseSpecializations').append(specializationList)
                    } else {
                        $('#airportBaseDetails .baseSpecializations').text('-')
                    }


	    			$('#airportDetailsBaseUpkeep').text('$' + commaSeparateNumber(airportBase.upkeep))

	    			$('#baseDetailsModal').data('scale', airportBase.scale)
	    			updateFacilityIcons(airport)
	    			enableButton($('#airportBaseDetails .specialization.button'))
	    		}

		    	var targetBase = baseDetails.targetBase
		    	$('#airportDetailsBaseUpgradeCost').text('$' + commaSeparateNumber(targetBase.value))
    			$('#airportDetailsBaseUpgradeUpkeep').text('$' + commaSeparateNumber(targetBase.upkeep))

	    		
	    		//update buttons and reject reasons
	    		if (baseDetails.rejection) {
	    			$('#buildHeadquarterButton').hide()
	    			$('#buildBaseButton').hide()
                    $('#upgradeBaseButton').hide()
	    			if (!airportBase) {
	    			    disableButton($('#buildBaseButton'), baseDetails.rejection)
	    			    $('#buildBaseButton').show()
	    			} else {
	    			    disableButton($('#upgradeBaseButton'), baseDetails.rejection)
	    			    $('#upgradeBaseButton').show()
	    			}
	    		} else {
	    			if (!airportBase) {
	    				if (activeAirline.headquarterAirport) {
		    				$('#buildHeadquarterButton').hide()
		    				enableButton($('#buildBaseButton'))
		    				$('#buildBaseButton').show()
	    				} else {
	    				    enableButton($('#buildHeadquarterButton'))
	    					$('#buildHeadquarterButton').show()
		    				$('#buildBaseButton').hide()
	    				}
	    				$('#upgradeBaseButton').hide()
	    			} else {
	    				$('#buildHeadquarterButton').hide()
	    				$('#buildBaseButton').hide()
	    				enableButton($('#upgradeBaseButton'))
	    				$('#upgradeBaseButton').show()
	    			}
	    		}
		    	
		    	if (baseDetails.downgradeRejection) {
                    disableButton($('#downgradeBaseButton'), baseDetails.downgradeRejection)
	    			$('#downgradeBaseButton').show()
		    	} else {
		    		if (airportBase) {
                        enableButton($('#downgradeBaseButton'))
		    			$('#downgradeBaseButton').show()
		    		} else {
		    			$('#downgradeBaseButton').hide()
		    		}
		    	}

		    	if (baseDetails.deleteRejection) {
                    disableButton($('#deleteBaseButton'), baseDetails.deleteRejection)
                    $('#deleteBaseButton').show()
                } else {
                    if (!airportBase) {
                        $('#deleteBaseButton').hide()
                    } else {
                        enableButton($('#deleteBaseButton'))
                        $('#deleteBaseButton').show()
                    }
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
	populateNavigation($('#airportCanvas'))
}


function updateAirportChampionDetails(airport) {
	$('#airportDetailsChampionList').children('div.table-row').remove()

    var url = "airports/" + airport.id + "/champions"
    if (activeAirline) {
        url += "?airlineId=" + activeAirline.id
    }
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	        var champions = result.champions
	    	$(champions).each(function(index, championDetails) {
	    		var row = $("<div class='table-row clickable' data-link='rival' onclick=\"showRivalsCanvas('" + championDetails.airlineId + "');\"></div>")
	    		var icon = getRankingImg(championDetails.ranking)
	    		row.append("<div class='cell'>" + icon + "</div>")
                row.append("<div class='cell'>" + getAirlineSpan(championDetails.airlineId, championDetails.airlineName) + "</div>")
	    		row.append("<div class='cell' style='text-align: right'>" + commaSeparateNumber(championDetails.loyalistCount) + "</div>")
	    		row.append("<div class='cell' style='text-align: right'>" + championDetails.loyalty + "</div>")
	    		row.append("<div class='cell' style='text-align: right'>" + championDetails.reputationBoost + "</div>")
	    		$('#airportDetailsChampionList').append(row)
	    	})

	    	if (result.currentAirline) {
	    	    var row = $("<div class='table-row clickable' data-link='rival' onclick=\"showRivalsCanvas('" + result.currentAirline.airlineId + "');\"></div>")
                row.append("<div class='cell'>" + result.currentAirline.ranking + "</div>")
                row.append("<div class='cell'>" + getAirlineSpan(result.currentAirline.airlineId, result.currentAirline.airlineName) + "</div>")
                row.append("<div class='cell' style='text-align: right'>" + commaSeparateNumber(result.currentAirline.amount) + "</div>")
                row.append("<div class='cell' style='text-align: right'>" + result.currentAirline.loyalty + "</div>")
                row.append("<div class='cell' style='text-align: right'>-</div>")
                $('#airportDetailsChampionList').append(row)
	    	}

	    	populateNavigation($('#airportDetailsChampionList'))

	    	if ($(champions).length == 0) {
	    		var row = $("<div class='table-row'></div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell' style='text-align: right'>-</div>")
	    		row.append("<div class='cell' style='text-align: right'>-</div>")
	    		row.append("<div class='cell' style='text-align: right'>-</div>")
	    		$('#airportDetailsChampionList').append(row)
	    	}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});

}

function initAirportMap() { //only called once, see https://stackoverflow.com/questions/10485582/what-is-the-proper-way-to-destroy-a-map-instance
    airportMap = new google.maps.Map(document.getElementById('airportMap'), {
		//center: {lat: airport.latitude, lng: airport.longitude},
	   	//zoom : 6,
	   	minZoom : 2,
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

    if (christmasFlag) {
        //<div id="santaClausButton" class="googleMapIcon glow" onclick="showSantaClausAttemptStatus()" align="center" style="display: none; margin-bottom: 10px;"><span class="alignHelper"></span><img src='@routes.Assets.versioned("images/markers/christmas/santa-hat.png")' title='Santa, where are you?' style="vertical-align: middle;"/></div>-->
        var santaClausButton = $('<div id="santaClausButton" class="googleMapIcon glow" onclick="showSantaClausAttemptStatus()" align="center" style="margin-bottom: 10px;"><span class="alignHelper"></span><img src="assets/images/markers/christmas/santa-hat.png" title=\'Santa, where are you!\' style="vertical-align: middle;"/></div>')

        santaClausButton.index = 1
        airportMap.controls[google.maps.ControlPosition.RIGHT_BOTTOM].push(santaClausButton[0]);
    }
}


function populateAirportDetails(airport) {
    if (!airportMap) {
        initAirportMap()
    }

    // cleanup first
    for (var i = 0; i < airportMapMarkers.length; i++ ) {
        airportMapMarkers[i].setMap(null);
    }
    airportMapMarkers = []

    if (airportMapCircle) { //remove the old circle
        airportMapCircle.setMap(null)
    }



    if (airport) {
		addCityMarkers(airportMap, airport)
		airportMap.setZoom(6)
        airportMap.setCenter({lat: airport.latitude, lng: airport.longitude}); //this would eventually trigger an idle

		var airportMarkerIcon = $("#airportMap").data("airportMarker")
		var airportMarker = new google.maps.Marker({
		    position: {lat: airport.latitude, lng: airport.longitude},
		    map: airportMap,
		    title: airport.name,
		    icon : airportMarkerIcon,
		    zIndex : 999
		  });

		airportMapMarkers.push(airportMarker)

		
		airportMapCircle = new google.maps.Circle({
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
		loadGenericTransits(airport)
		updateAirportLoyalistDetails(airport)
		showAirportAssets(airport)

		google.maps.event.addListenerOnce(airportMap, 'idle', function() {
           setTimeout(function() { //set a timeout here, otherwise it might not render part of the map...
             airportMap.setCenter({lat: airport.latitude, lng: airport.longitude}); //this would eventually trigger an idle
             google.maps.event.trigger(airportMap, 'resize'); //this refreshes the map
           }, 2000);
        });

        if (christmasFlag) {
            initSantaClaus()
        }
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
	    	updateAirportRating(airportStatistics.rating)
	    	updateFacilityList(airportStatistics)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function loadGenericTransits(airport) {
    $.ajax({
        type: 'GET',
        url: "airports/" + activeAirportId + "/generic-transits",
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(transits) {
            $('#genericTransitModal .table.genericTransits').data('transits', transits) //set the loaded data to modal as well
            $('#airportDetailsNearbyAirportCount').text(transits.length)

        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
	});

}

function updateAirportRating(rating) {
    var fullStarSource = "assets/images/icons/star.png"
    var halfStarSource = "assets/images/icons/star-half.png"
    var fullFireSource = "assets/images/icons/fire.png"
    var halfFireSource = "assets/images/icons/fire-small.png"
    $("#airportCanvas .economicRating").append(getHalfStepImageBarByValue(fullStarSource, halfStarSource, 10, rating.economicRating).css({ 'display' : 'inline-block', 'vertical-align' : 'text-bottom'}))
    $("#airportCanvas .economicRating").append(getRatingSpan(rating.economicRating, true).css('margin-left', '5px'))
    $("#airportCanvas .countryRating").append(getHalfStepImageBarByValue(fullStarSource, halfStarSource, 10, rating.countryRating).css({ 'display' : 'inline-block', 'vertical-align' : 'text-bottom'}))
    $("#airportCanvas .countryRating").append(getRatingSpan(rating.countryRating, true).css('margin-left', '5px'))
    $("#airportCanvas .competitionRating").append(getHalfStepImageBarByValue(fullFireSource, halfFireSource, 10, rating.competitionRating).css({ 'display' : 'inline-block', 'vertical-align' : 'text-bottom'}))
    $("#airportCanvas .competitionRating").append(getRatingSpan(rating.competitionRating, false).css('margin-left', '5px'))
    $("#airportCanvas .difficulty").append(getHalfStepImageBarByValue(fullFireSource, halfFireSource, 10, rating.difficulty).css({ 'display' : 'inline-block', 'vertical-align' : 'text-bottom'}))
    $("#airportCanvas .difficulty").append(getRatingSpan(rating.difficulty, false).css('margin-left', '5px'))
}

//if inverse is true then higher the rating, easier it is
function getRatingSpan(rating, inverse) {
    var $span = $('<span></span')
    var value = inverse ? 100 - rating : rating
    var description
    if (value <= 30) {
        description = "very easy"
    } else if (value <= 50) {
        description = "easy"
    } else if (value <= 70) {
        description = "quite challenging"
    } else if (value <= 90) {
        description = "challenging"
    } else {
        description = "very challenging"
    }
    $span.text(rating + " (" + description + ")")

    return $span
}

function updateFacilityList(statistics) {
	$('#airportDetailsHeadquarterList').children('.table-row').remove()
	$('#airportDetailsBaseList').children('.table-row').remove()
	$('#airportDetailsLoungeList').children('.table-row').remove()
	
	
	var hasHeadquarters = false
	var hasBases = false
	var hasLounges = false
	$.each(statistics.bases, function(index, base) {
		var row = $("<div class='table-row clickable' data-link='rival'></div>")
		row.append("<div class='cell'>" +  getAirlineSpan(base.airlineId, base.airlineName) + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + getCountryFlagImg(base.airlineCountryCode) + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + base.scale + "</div>")
		row.click(function() {
		    showRivalsCanvas(base.airlineId)
		})

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
		var row = $("<div class='table-row clickable' data-link='rival'></div>")
		row.append("<div class='cell'>" +  getAirlineSpan(lounge.airlineId, htmlEncode(lounge.airlineName)) + "</div>")
		row.append("<div class='cell'>" + lounge.name + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + lounge.level + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + lounge.status + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(loungeStats.selfVisitors) + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(loungeStats.allianceVisitors) + "</div>")
		row.click(
		    function() {
        	   showRivalsCanvas(lounge.airlineId)
        })
		
		$('#airportDetailsLoungeList').append(row)
		hasLounges = true
	})

	populateNavigation($('#airportCanvas'))
	
	if (!hasHeadquarters) {
		var emptyRow = $("<div class='table-row'></div>")
		emptyRow.append("<div class='cell'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		$('#airportDetailsHeadquarterList').append(emptyRow)
	}
	if (!hasBases) {
		var emptyRow = $("<div class='table-row'></div>")
		emptyRow.append("<div class='cell'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		$('#airportDetailsBaseList').append(emptyRow)
	}
	if (!hasLounges) {
		var emptyRow = $("<div class='table-row'></div>")
		emptyRow.append("<div class='cell'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		$('#airportDetailsLoungeList').append(emptyRow)
	}
}


function getAirports() {
    markers = undefined
	$.getJSON( "airports", function( data ) {
	      airports = data
		  addMarkers(data)
	});
}

function addMarkers(airports) {
    var infoWindow = new google.maps.InfoWindow({
		maxWidth : 250
	})
	var originalOpacity = 0.7
	currentZoom = map.getZoom()

	google.maps.event.addListener(infoWindow, 'closeclick',function(){
       if (infoWindow.marker) {
        infoWindow.marker.setOpacity(originalOpacity)
       }
    });

	var resultMarkers = {}
	for (i = 0; i < airports.length; i++) {
		  var airportInfo = airports[i]
		  var position = {lat: airportInfo.latitude, lng: airportInfo.longitude};
		  var icon = getAirportIcon(airportInfo)
//          if (airportInfo.championAirlineName) {
//            console.log(airportInfo.name + "-> " + airportInfo.championAirlineName)
//          }

		  
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
                opacity: originalOpacity,
		  		airport : airportInfo,
		  		icon: icon,
		  		originalIcon: icon, //so we can flip back and forth
			  });
		  if (airportInfo.championAirlineId) {
            marker.championIcon = '/airlines/' + airportInfo.championAirlineId + '/logo'
            marker.championAirlineName = airportInfo.championAirlineName
            marker.contested = airportInfo.contested
          }

		  
		  var zIndex = airportInfo.size * 10 
		  var sizeAdjust = Math.floor(airportInfo.population / 1000000) //add something extra due to pop
		  if (sizeAdjust > 9) {
			sizeAdjust = 9;
		  }
		  zIndex += sizeAdjust
		  
		  marker.setZIndex(zIndex); //major airport should have higher index

		  marker.addListener('click', function() {
			  infoWindow.close();
			  if (infoWindow.marker && infoWindow.marker != this) {
			    infoWindow.marker.setOpacity(originalOpacity)
              }
			  
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
			  $("#airportPopupPopulation").text('-') //wait for extended details
			  $("#airportPopupIncomeLevel").text('-') //wait for extended details
			  $("#airportPopupOpenness").html(getOpennessSpan(loadedCountriesByCode[this.airport.countryCode].openness))
			  $("#airportPopupMaxRunwayLength").html(this.airport.runwayLength + "&nbsp;m")
			  updateAirportExtendedDetails(this.airport.id, this.airport.countryCode)
			  //updateAirportSlots(this.airport.id)
			  
			  $("#airportPopupId").val(this.airport.id)
			  var popup = $("#airportPopup").clone()
			  populateNavigation(popup)
			  popup.show()
			  infoWindow.setContent(popup[0])
			  infoWindow.open(map, this);
			  infoWindow.marker = this
			  
			  activeAirportPopupInfoWindow = infoWindow
			  
			  if (activeAirline) {
				  if (!activeAirline.headquarterAirport) {
					  $("#planToAirportButton").hide()
				  } else {
					  $("#planToAirportButton").show()
				  }
			  } else {
				  $("#planToAirportButton").hide()
			  }
			  
		  });

		  marker.addListener('mouseover', function(event) {
               this.setOpacity(0.9)
            })
            marker.addListener('mouseout', function(event) {
                if (infoWindow.marker != this) {
                    this.setOpacity(originalOpacity)
                }
            })


		  marker.setVisible(isShowMarker(marker, currentZoom))


		  resultMarkers[airportInfo.id] = marker
	}
	//now assign it to markers to indicate that it's ready
	markers = resultMarkers
}

function planToAirportFromInfoWindow() {
	closeAirportInfoPopup();
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
		  airportMapMarkers.push(marker)
		  
		  marker.addListener('click', function() {
			  infoWindow.close();
			  var city = this.cityInfo
			  $("#cityPopupName").text(city.name)
			  $("#cityPopupPopulation").text(commaSeparateNumber(city.population))
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

			var popup = $("#cityPopup").clone()
            popup.show()
            infoWindow.setContent(popup[0])
			infoWindow.open(airportMap, this);
			  
			  
			/////////////////////!!!!!!!!!!!!!!!
		 ///////////////
		  });
		  //marker.setVisible()
	});
}



function isShowMarker(marker, zoom) {
	if (championMapMode && !marker.championIcon) {
	    return false
    }
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

function updateAirportLoyalistDetails(airport) {
    var url = "airports/" + airport.id + "/loyalist-data"
    var $table = $('#airportCanvas .loyalistDelta')
    $table.find('.table-row').remove()

    if (activeAirline) {
        url += "?airlineId=" + activeAirline.id
    }
    $.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	        var currentData = result.current

            $.each(result.airlineDeltas, function(index, deltaEntry) {
                var airlineName = deltaEntry.airlineName
                var airlineId = deltaEntry.airlineId
                var deltaText = (deltaEntry.passengers >= 0) ? ("+" + deltaEntry.passengers) : deltaEntry.passengers
                var $row = $('<div class="table-row clickable" data-link="rival"><div class="cell">' + getAirlineSpan(airlineId, airlineName) + '</div><div class="cell" style="text-align:right">' + deltaText + '</div></div>')
                $row.click(function() {
                    showRivalsCanvas(deltaEntry.airlineId)
                })
                $table.append($row)
            })

	    	assignAirlineColors(currentData, "airlineId")

	    	plotPie(currentData, activeAirline ? activeAirline.name : null , $("#airportCanvas .loyalistPie"), "airlineName", "amount")
	    	plotLoyalistHistoryChart(result.history, $("#loyalistHistoryModal .loyalistHistoryChart"))
            populateNavigation($('#airportCanvas'))
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function showLoyalistHistoryModal() {
    $("#loyalistHistoryModal").fadeIn(500)
}

function refreshAirportExtendedDetails(airport) {
    //clear the old values
	if (activeAirline && activeAirline.headquarterAirport) { //if this airline has picked headquarter
	    var airlineId = activeAirline.id
        var hasMatch = false
        $.each(airport.appealList, function( key, appeal ) {
            if (appeal.airlineId == airlineId) {
                if (airport.bonusList[airlineId]) {
                    if (airport.bonusList[airlineId].loyalty > 0) {
                        $(".airportLoyaltyBonus").text("(+" + airport.bonusList[airlineId].loyalty + ")")
                        $(".airportLoyaltyBonus").show()
                        $('#appealBonusDetailsTooltip').data('loyaltyBreakdown', airport.bonusList[airlineId].loyaltyBreakdown)
                        $('.airportLoyaltyBonusTrigger').show()
                    } else {
                        $(".airportLoyaltyBonus").hide()
                        $('.airportLoyaltyBonusTrigger').hide()
                    }
                }

                var fullHeartSource = "assets/images/icons/heart.png"
                var halfHeartSource = "assets/images/icons/heart-half.png"
                var emptyHeartSource = "assets/images/icons/heart-empty.png"

                $(".airportLoyalty").empty()
                getPaddedHalfStepImageBarByValue(fullHeartSource, halfHeartSource, emptyHeartSource, 10, appeal.loyalty).css({'display' : 'inline-block', width: '85px'}).appendTo($("#airportCanvas .airportLoyalty"))

                $(".airportLoyalty").append(appeal.loyalty)

                hasMatch = true
            }
        });
        if (!hasMatch) {
            $(".airportLoyalty").text("0")
        }

//        var relationshipValue = loadedCountriesByCode[airport.countryCode].mutualRelationship
//        if (typeof relationshipValue != 'undefined') {
//            $(".airportRelationship").text(getCountryRelationshipDescription(relationshipValue))
//        } else {
//            $(".airportRelationship").text('-')
//        }
    }
    var $populationSpan = getBoostSpan(airport.population, airport.populationBoost, $('#populationDetailsTooltip'))
    $("#airportPopupPopulation").html($populationSpan)

    var $incomeLevelSpan = getBoostSpan(airport.incomeLevel, airport.incomeLevelBoost , $('#incomeDetailsTooltip'))
    $("#airportPopupIncomeLevel").html($incomeLevelSpan)

    $(".airportFeatures .feature").remove()
    $.each(airport.features, function(index, feature) {
        var $popupFeatureDiv = $("<div class='feature' style='display:inline-flex'><img src='assets/images/icons/airport-features/" + feature.type + ".png' title='" + feature.title + "'; style='vertical-align: bottom;'></div>").appendTo($("#airportPopup .airportFeatures"))
        var $popupFeatureSpan
        if (feature.boosts && feature.boosts.length > 0) {
            $popupFeatureSpan = getBoostSpan(feature.strength, feature.boosts, createIfNotExist($('#boostDetailsTooltipTemplate'), feature.type + "Tooltip"))
        } else {
            $popupFeatureSpan = $('<span>' + (feature.strength > 0 ? feature.strength : '') + '</span>')
        }
        $popupFeatureDiv.append($popupFeatureSpan)

        var $featureDiv = $("<div class='feature'><img src='assets/images/icons/airport-features/" + feature.type + ".png'; style='margin-right: 5px;'></div>")
        $featureDiv.css({ 'display' : "flex", 'align-items' : "center", 'padding' : "2px 0" })
        var featureText = feature.title
        if (feature.strength != 0) {
            if (feature.boosts && feature.boosts.length > 0) {
                featureText += " (strength: <span style='color: #41A14D'>" + feature.strength + "</span>)"
            } else {
                featureText += " (strength: " + feature.strength + ")"
            }
        }
        var $featureDescription = $('<span><span>').text(feature.title)

         if (feature.strength != 0) {
            var $featureStrengthSpan = $('<span>(strength:&nbsp;</span>')
            if (feature.boosts && feature.boosts.length > 0) {
                var $boostSpan = getBoostSpan(feature.strength, feature.boosts, createIfNotExist($('#boostDetailsTooltipTemplate'), feature.type + "Tooltip"))
                $featureStrengthSpan.append($boostSpan)
                $featureStrengthSpan.append('<span>)</span>')
            } else {
                $featureStrengthSpan.append('<span>' + feature.strength + ')</span>')
            }
            $featureDescription.append($featureStrengthSpan)
        }

        $featureDiv.append($featureDescription)
        $("#airportCanvas .airportFeatures").append($featureDiv)
    })
}

function updateAirportExtendedDetails(airportId, countryCode) {
	//clear the old values
	$(".airportLoyalty").text('-')
	$(".airportRelationship").text('-')
	$(".airportLoyaltyBonus").hide()
    $('.airportLoyaltyBonusTrigger').hide()
	$("#airportIcons .feature").hide()

    $.ajax({
        type: 'GET',
        url: "airports/" + airportId,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(airport) {
            refreshAirportExtendedDetails(airport)
        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });

    $("#airportCanvas .countryRelationship .total").text("-")
    $("#airportCanvas .airlineTitle").text("-")
    if (activeAirline) {
        $.ajax({
            type: 'GET',
            url: "/countries/" + countryCode + "/airline/" + activeAirline.id,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(info) {
                var relationship = info.relationship
                var relationshipSpan = getAirlineRelationshipDescriptionSpan(relationship.total)
                $("#airportCanvas .countryRelationship .total").html(relationshipSpan)
                var $relationshipDetailsIcon = $("#airportCanvas .countryRelationship .detailsIcon")
                $relationshipDetailsIcon.data("relationship", relationship)
                $relationshipDetailsIcon.data("title", info.title)
                $relationshipDetailsIcon.data("countryCode", countryCode)
                $relationshipDetailsIcon.show()

                var title = info.title
                updateAirlineTitle(title, $("#airportCanvas img.airlineTitleIcon"), $("#airportCanvas .airlineTitle"))
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
    } else {
        $("#airportCanvas .countryRelationship .detailsIcon").hide()
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

var championMapMode = false
var contestedMarkers = []
function toggleChampionMap() {
   var zoom = map.getZoom();
   championMapMode = !championMapMode
    $.each(markers, function(index, marker) {
        if (championMapMode) {
            if (marker.championIcon) {
                marker.previousIcon = marker.icon
                marker.previousTitle = marker.title
                //marker.setIcon(marker.championIcon)
                marker.setIcon(marker.championIcon)
                var title = marker.title + " - " + marker.championAirlineName
        //        google.maps.event.clearListeners(marker, 'mouseover');
        //        google.maps.event.clearListeners(marker, 'mouseout');
                if (marker.contested) {
                    addContestedMarker(marker)
                    title += " (contested by " + marker.contested + ")"
                }
                marker.setTitle(title)
            } else {
                marker.setVisible(false)
            }
        } else {

            if (marker.championIcon) {
                marker.setTitle(marker.previousTitle)
                marker.setIcon(marker.previousIcon)
            }
            while (contestedMarkers.length > 0) {
                var contestedMarker = contestedMarkers.pop()
                contestedMarker.setMap(null)
            }
            marker.setVisible(isShowMarker(marker, zoom))
            updateAirportMarkers(activeAirline)
        }
    })

}

function addContestedMarker(airportMarker) {
    var contestedMarker = new google.maps.Marker({
        position: airportMarker.getPosition(),
        map,
        title: "Contested",
        icon: { anchor: new google.maps.Point(-5,15), url: "assets/images/icons/fire.png" },
        zIndex: 500
      });
    //marker.setVisible(isShowMarker(airportMarker, zoom))
    contestedMarker.bindTo("visible", airportMarker)
    contestedMarker.setZIndex(airportMarker.getZIndex() + 1)
    contestedMarkers.push(contestedMarker)
}


function updateAirportBaseMarkers(newBaseAirports, relatedFlightPaths) {
    //reset baseMarkers
    $.each(baseMarkers, function(index, marker) {
        marker.setIcon(marker.originalIcon)
        marker.isBase = false
        marker.setVisible(isShowMarker(marker, map.getZoom()))
        marker.baseInfo = undefined
        google.maps.event.clearListeners(marker, 'mouseover');
        google.maps.event.clearListeners(marker, 'mouseout');

    })
    baseMarkers = []
    var headquarterMarkerIcon = $("#map").data("headquarterMarker")
    var baseMarkerIcon = $("#map").data("baseMarker")
    $.each(newBaseAirports, function(key, baseAirport) {
        var marker = markers[baseAirport.airportId]
        if (baseAirport.headquarter) {
            marker.setIcon(headquarterMarkerIcon)
        } else {
            marker.setIcon(baseMarkerIcon)
        }
        marker.setZIndex(999)
        marker.isBase = true
        marker.setVisible(true)
        marker.baseInfo = baseAirport
        var originalOpacity = marker.getOpacity()
        marker.addListener('mouseover', function(event) {
                    $.each(relatedFlightPaths, function(linkId, pathEntry) {
                        var path = pathEntry.path
                        var link = pathEntry.path.link
                        if (!$(path).data("originalOpacity")) {
                            $(path).data("originalOpacity", path.strokeOpacity)
                        }
                        if (link.fromAirportId != baseAirport.airportId || link.airlineId != baseAirport.airlineId) {
                            path.setOptions({ strokeOpacity : 0.1 })
                        } else {
                            path.setOptions({ strokeOpacity : 0.8 })
                        }
                    })
                })
        marker.addListener('mouseout', function(event) {
            $.each(relatedFlightPaths, function(linkId, pathEntry) {
                var path = pathEntry.path
                var originalOpacity = $(path).data("originalOpacity")
                if (originalOpacity !== undefined) {
                    path.setOptions({ strokeOpacity : originalOpacity })
                }
            })
        })

        baseMarkers.push(marker)
    })

    return baseMarkers
}

function updateAirportMarkers(airline) { //set different markers for head quarter and bases
	if (!markers) { //markers not ready yet, wait
		setTimeout(function() { updateAirportMarkers(airline) }, 100)
	} else {
	    if (airline) {
		    updateAirportBaseMarkers(airline.baseAirports, flightPaths)
		} else {
            updateAirportBaseMarkers([])
		}
    }
}

//airport links view

function toggleAirportLinksView() {
	clearAirportLinkPaths() //clear previous ones if exist
	deselectLink()

	toggleAirportLinks(activeAirport)
}

function closeAirportInfoPopup() {
    if (activeAirportPopupInfoWindow) {
        activeAirportPopupInfoWindow.close(map)
        if (activeAirportPopupInfoWindow.marker) {
            activeAirportPopupInfoWindow.marker.setOpacity(0.7)
        }
        activeAirportPopupInfoWindow = undefined
    }
}

function toggleAirportLinks(airport) {
	clearAllPaths()
	closeAirportInfoPopup()
	$.ajax({
		type: 'GET',
		url: "airports/" + airport.id + "/links",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(linksByRemoteAirport) {
	        $("#topAirportLinksPanel .topDestinations .table-row").remove()
	    	$.each(linksByRemoteAirport, function(index, entry) {
                drawAirportLinkPath(airport, entry)
                //populate top 5 destinations
                if (index < 5) {
                    var $destinationRow = $('<div class="table-row"></div>')
                    var $airportCell = $('<div class="cell"></div>')
                    $airportCell.append(getAirportSpan(entry.remoteAirport))
                    $destinationRow.append($airportCell)
                    $destinationRow.append('<div class="cell">' + toLinkClassValueString(entry.capacity) + '(' + entry.frequency + ')</div>')
                    var $operatorsCell = $('<div class="cell"></div>')
                    $.each(entry.operators, function(index, operator) {
                        var $airlineLogoSpan = $('<span></span>')
                    	$airlineLogoSpan.append(getAirlineLogoImg(operator.airlineId))
                    	$airlineLogoSpan.attr("title", operator.airlineName + ' ' + toLinkClassValueString(operator.capacity) + '(' + operator.frequency + ')')
                        $operatorsCell.append($airlineLogoSpan)
                    })
                    $destinationRow.append($operatorsCell)

                    $("#topAirportLinksPanel .topDestinations").append($destinationRow)
                }
            })
            if (linksByRemoteAirport.length == 0) {
                $("#topAirportLinksPanel .topDestinations").append("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div></div>")
            }

	    	$("#topAirportLinksPanel").show();
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


function drawAirportLinkPath(localAirport, details) {
    var remoteAirport = details.remoteAirport
    var from = new google.maps.LatLng({lat: localAirport.latitude, lng: localAirport.longitude})
	var to = new google.maps.LatLng({lat: remoteAirport.latitude, lng: remoteAirport.longitude})
	var pathKey = remoteAirport.id

    var totalCapacity = details.capacity.total

    var opacity
    if (totalCapacity < 2000) {
        opacity = 0.2 + totalCapacity / 2000 * (0.6)
    } else {
        opacity = 0.8
    }
	var airportLinkPath = new google.maps.Polyline({
			 geodesic: true,
		     strokeColor: "#DC83FC",
		     strokeOpacity: opacity,
		     strokeWeight: 2,
		     path: [from, to],
		     zIndex : 1100,
		     map: map,
		});
		
	var fromAirport = getAirportText(localAirport.city, localAirport.iata)
	var toAirport = getAirportText(remoteAirport.city, remoteAirport.iata)
	
	
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
	     details: details,
	     map: map,
	});
	polylines.push(airportLinkPath)
    polylines.push(shadowPath)
	
	airportLinkPath.shadowPath = shadowPath
	
	var infowindow;
	shadowPath.addListener('mouseover', function(event) {
	    highlightPath(airportLinkPath, false)
		$("#airportLinkPopupFrom").html(getCountryFlagImg(this.fromCountry) + this.fromAirport)
		$("#airportLinkPopupTo").html(getCountryFlagImg(this.toCountry) + this.toAirport)
		$("#airportLinkPopupCapacity").text(toLinkClassValueString(this.details.capacity) + "(" + this.details.frequency + ")")
		$("#airportLinkOperators").empty()
		$.each(this.details.operators, function(index, operator){
		    var $operatorDiv = $('<div></div>')
		    $operatorDiv.append(getAirlineLogoSpan(operator.airlineId, operator.airlineName))
            $operatorDiv.append('<span>' + operator.frequency + '&nbsp;flight(s) weekly&nbsp;' + toLinkClassValueString(operator.capacity) + '</span>')
		    $("#airportLinkOperators").append($operatorDiv)
		})


		infowindow = new google.maps.InfoWindow({
             maxWidth : 400
             });
        var popup = $("#airportLinkPopup").clone()
        popup.show()
        infowindow.setContent(popup[0])
		
		infowindow.setPosition(event.latLng);
		infowindow.open(map);
	})		
	shadowPath.addListener('mouseout', function(event) {
	    unhighlightPath(airportLinkPath)
		infowindow.close()
	})
	
	airportLinkPaths[pathKey] = airportLinkPath
}

function clearAirportLinkPaths() {
	$.each(airportLinkPaths, function(key, airportLinkPath) {
		airportLinkPath.setMap(null)
		airportLinkPath.shadowPath.setMap(null)
	})
	
	airportLinkPaths = {}
}


function hideAirportLinksView() {
	//printConsole('')
	clearAirportLinkPaths()
	updateLinksInfo() //redraw all flight paths
		
    $("#topAirportLinksPanel").hide();
}

function getAirportIcon(airportInfo) {
    var largeAirportMarkerIcon = $("#map").data("largeAirportMarker")
    var mediumAirportMarkerIcon = $("#map").data("mediumAirportMarker")
    var smallAirportMarkerIcon = $("#map").data("smallAirportMarker")
    var gatewayAirportMarkerIcon = $("#map").data("gatewayAirportMarker")

    if (airportInfo.isGateway) {
      icon = gatewayAirportMarkerIcon
    } else if (airportInfo.size <= 3) {
      icon = smallAirportMarkerIcon
    } else if (airportInfo.size <= 6) {
      icon = mediumAirportMarkerIcon
    } else {
      icon = largeAirportMarkerIcon
    }
    return icon
}
function hideAppealBreakdown() {
    $('#appealBonusDetailsTooltip').hide()
}

function showAppealBreakdown($icon, bonusDetails) {
    var yPos = $icon.offset().top - $(window).scrollTop() + $icon.height()
    var xPos = $icon.offset().left - $(window).scrollLeft() + $icon.width() - $('#appealBonusDetailsTooltip').width() / 2

    $('#appealBonusDetailsTooltip').css('top', yPos + 'px')
    $('#appealBonusDetailsTooltip').css('left', xPos + 'px')
    $('#appealBonusDetailsTooltip').show()


    $('#appealBonusDetailsTooltip .table .table-row').remove()
    $.each(bonusDetails, function(index, entry) {
        var $row = $('<div class="table-row"><div class="cell" style="width: 70%;">' + entry.description + '</div><div class="cell" style="width: 30%; text-align: right;">+' + entry.value + '</div></div>')
        $row.css('color', 'white')
        $('#appealBonusDetailsTooltip .table').append($row)
    })
}

function showSpecializationModal() {
    var $container = $('#baseSpecializationModal .container')
    $container.empty()
    $.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/bases/" + activeAirportId + "/specialization-info",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(info) {
            $.each(info.specializations, function(index, specializationsByScale) {
                var $scaleDiv = $('<div class="section"></div>').appendTo($container)
                $scaleDiv.append($('<h4>Hub Scale Requirement ' + specializationsByScale.scaleRequirement + '</h4>'))
                var $flexDiv = $('<div style="display: flex; flex-wrap: wrap;"></div>').appendTo($scaleDiv)
                $.each(specializationsByScale.specializations, function(index, specialization) {
                    var $specializationDiv = $('<div class="section specialization" style="min-width: 200px; flex:1;"></div>').appendTo($flexDiv)
                    $specializationDiv.data('id', specialization.id)
                    $specializationDiv.append($('<h4>' + specialization.label + '</h4>'))
                    var $descriptionList = $('<ul></ul>').appendTo($specializationDiv)
                    $.each(specialization.descriptions, function(index, description) {
                        $descriptionList.append($('<li class="dot">' + description + '</li>'))
                    })

                    if (specialization.available) {
                        $specializationDiv.addClass('available')
                        if (!specialization.free) {
                            $specializationDiv.on('click', function() {
                                $(this).siblings().removeClass('active')
                                $(this).toggleClass('active')
                            })
                        } else {
                            $specializationDiv.attr('title', 'Free at scale ' + specializationsByScale.scaleRequirement)
                        }
                    } else {
                        $specializationDiv.addClass('unavailable')
                        $specializationDiv.attr('title', 'Do not meet hub scale requirement: ' + specializationsByScale.scaleRequirement)
                    }

                    if (specialization.active) {
                        $specializationDiv.addClass('active')
                    }
                })
            })

            if (info.cooldown > 0) {
                disableButton($('#baseSpecializationModal .confirm'), info.cooldown + " more week(s) before another change")
            } else {
                enableButton($('#baseSpecializationModal .confirm'))
            }

            $('#baseSpecializationModal').data("defaultCooldown", info.defaultCooldown)

            $('#baseSpecializationModal').fadeIn(500)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});

}

function confirmSpecializations() {
    var defaultCooldown = $('#baseSpecializationModal').data("defaultCooldown")
    promptConfirm("Changes can only be made every " + defaultCooldown + " weeks, confirm?", function() {
        var airlineId = activeAirline.id
        var url = "airlines/" + airlineId + "/bases/" + activeAirportId + "/specializations"
        var selectedSpecializations = []
        $('#baseSpecializationModal .specialization.active').each(function(index) {
            selectedSpecializations.push($(this).data('id'))
        })

        $.ajax({
            type: 'PUT',
            data: JSON.stringify({
                "selectedSpecializations" : selectedSpecializations
            }),
            url: url,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(response) {
                closeModal($('#baseSpecializationModal'))
                showAirportDetails(activeAirportId)
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
    })
}

function showGenericTransitModal() {
    var $table = $('#genericTransitModal .table.genericTransits')
    $table.find('.table-row').remove()

    var transits = $table.data('transits')
    $.each(transits, function(index, transit) {
        $row = $('<div class="table-row" style="width: 100%"></div>')
        $row.append($('<div class="cell">' + transit.toAirportText + '</div>'))
        $row.append($('<div class="cell" align="right">' + commaSeparateNumber(transit.toAirportPopulation) + '</div>'))
        $row.append($('<div class="cell capacity" align="right">' + commaSeparateNumber(transit.capacity) + '</div>'))
        $row.append($('<div class="cell" align="right">' + commaSeparateNumber(transit.passenger) + '</div>'))

        $table.append($row)
    })
    if (transits.length == 0) {
        $table.append('<div class="table-row"><div class="cell">-</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div></div>')
    }
    $('#genericTransitModal').fadeIn(200)
}

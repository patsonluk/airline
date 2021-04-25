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
	
	$('#airportDetailsPopulation').text(commaSeparateNumber(airport.population))
    $("#airportDetailsCity").text(airport.city)
    $("#airportDetailsSize").text(airport.size)
    $("#airportDetailsIncomeLevel").text(airport.incomeLevel)
	$("#airportDetailsCountry").text(loadedCountriesByCode[airport.countryCode].name)
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
                        var specializationList = $('<ul></ul>')
                        $.each(airportBase.specializations, function(index, specialization) {
                            specializationList.append($('<li class="dot">' + specialization.label + '</li>'))
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
		    	
	    		
	    		if (!airportBase || airportBase.headquarter) {
	    			$('#deleteBaseButton').hide()
	    		} else {
	    			$('#deleteBaseButton').show()
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
}


function updateAirportChampionDetails(airport) {
	$('#airportDetailsChampionList').children('div.table-row').remove()

	$.ajax({
		type: 'GET',
		url: "airports/" + airport.id + "/champions",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(champions) {
	    	$(champions).each(function(index, championDetails) {
	    		var row = $("<div class='table-row clickable' data-link='rival' onclick=\"showRivalsCanvas('" + championDetails.airlineId + "');\"></div>")
	    		row.append("<div class='cell'>" + getRankingImg(championDetails.ranking) + "</div>")
	    		row.append("<div class='cell'>" + getAirlineLogoImg(championDetails.airlineId) + championDetails.airlineName + "</div>")
	    		row.append("<div class='cell' style='text-align: right'>" + commaSeparateNumber(championDetails.loyalistCount) + "</div>")
	    		row.append("<div class='cell' style='text-align: right'>" + championDetails.loyalty + "</div>")
	    		row.append("<div class='cell' style='text-align: right'>" + championDetails.reputationBoost + "</div>")
	    		$('#airportDetailsChampionList').append(row)
	    	})

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
		updateAirportLoyalistDetails(airport)

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
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell'>" +  getAirlineLogoImg(base.airlineId) + base.airlineName + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + getCountryFlagImg(base.airlineCountryCode) + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + base.scale + "</div>")
		
		
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
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell'>" +  getAirlineLogoImg(lounge.airlineId) + lounge.airlineName + "</div>")
		row.append("<div class='cell'>" + lounge.name + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + lounge.level + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + lounge.status + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(loungeStats.selfVisitors) + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(loungeStats.allianceVisitors) + "</div>")
		
		$('#airportDetailsLoungeList').append(row)
		hasLounges = true
	})
	
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
			  $("#airportPopupPopulation").text(commaSeparateNumber(this.airport.population))
			  $("#airportPopupIncomeLevel").text(this.airport.incomeLevel)
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
				  } else if (this.airport.id == activeAirline.headquarterAirport.airportId) {
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
			  $("#cityPopupPopulation").text(city.population)
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

                $table.append('<div class="table-row"><div class="cell">' + getAirlineLogoImg(airlineId) + airlineName + '</div><div class="cell" style="text-align:right">' + deltaText + '</div></div>')
            })

	    	assignAirlineColors(currentData, "airlineId")

	    	plotPie(currentData, activeAirline ? activeAirline.name : null , $("#airportCanvas .loyalistPie"), "airlineName", "amount")
	    	plotLoyalistHistoryChart(result.history, $("#airportCanvas .loyalistHistoryChart"))

	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function refreshAirportExtendedDetails(airport) {
    //clear the old values
	if (activeAirline && activeAirline.headquarterAirport) { //if this airline has picked headquarter
	    var airlineId = activeAirline.id
        var hasMatch = false
        $.each(airport.appealList, function( key, appeal ) {
            if (appeal.airlineId == airlineId) {
                if (airport.bonusList[airlineId]) {
                    if (airport.bonusList[airlineId].awareness > 0) {
                        $(".airportAwarenessBonus").text("(+" + airport.bonusList[airlineId].awareness + ")")
                        $(".airportAwarenessBonus").show()
                        $('#appealBonusDetailsTooltip').data('awarenessBreakdown', airport.bonusList[airlineId].awarenessBreakdown)
                        $('.airportAwarenessBonusTrigger').show()
                    } else {
                        $(".airportAwarenessBonus").hide()
                        $('.airportAwarenessBonusTrigger').hide()
                    }
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
                var fullStarSource = "assets/images/icons/star.png"
                var halfStarSource = "assets/images/icons/star-half.png"

                $(".airportAwareness").empty()
                $(".airportLoyalty").empty()
                getHalfStepImageBarByValue(fullStarSource, halfStarSource, 10, appeal.awareness).css({'display' : 'inline-block', width: '85px'}).appendTo($("#airportCanvas .airportAwareness"))
                getHalfStepImageBarByValue(fullHeartSource, halfHeartSource, 10, appeal.loyalty).css({'display' : 'inline-block', width: '85px'}).appendTo($("#airportCanvas .airportLoyalty"))

                $(".airportAwareness").append(appeal.awareness)
                $(".airportLoyalty").append(appeal.loyalty)

                hasMatch = true
            }
        });
        if (!hasMatch) {
            $(".airportAwareness").text("0")
            $(".airportLoyalty").text("0")
        }

//        var relationshipValue = loadedCountriesByCode[airport.countryCode].mutualRelationship
//        if (typeof relationshipValue != 'undefined') {
//            $(".airportRelationship").text(getCountryRelationshipDescription(relationshipValue))
//        } else {
//            $(".airportRelationship").text('-')
//        }
    }
    $(".airportFeatures .feature").remove()
    $.each(airport.features, function(index, feature) {
        $("#airportPopup .airportFeatures").append("<div class='feature' style='display:inline'><img src='assets/images/icons/airport-features/" + feature.type + ".png' title='" + feature.title + "'; style='vertical-align: bottom;'><span>" +  (feature.strength != 0 ? feature.strength : "") + "</span></div>")


        var $featureDiv = $("<div class='feature'><img src='assets/images/icons/airport-features/" + feature.type + ".png'; style='margin-right: 5px;'></div>")
        $featureDiv.css({ 'display' : "flex", 'align-items' : "center", 'padding' : "2px 0" })
        var featureText = feature.title
        if (feature.strength != 0) {
            featureText += " (strength: " + feature.strength + ")"
        }
        var $featureDescription = $('<span><span>').text(featureText)
        $featureDiv.append($featureDescription)
        $("#airportCanvas .airportFeatures").append($featureDiv)
    })
}

function updateAirportExtendedDetails(airportId, countryCode) {
	//clear the old values
	$(".airportAwareness").text('-')
	$(".airportLoyalty").text('-')
	$(".airportRelationship").text('-')
	$(".airportAwarenessBonus").hide()
    $('.airportAwarenessBonusTrigger').hide()
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

	$("#hideAirportLinksButton").show();

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
		url: "airports/" + airport.id + "/link-consumptions",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(passengersByRemoteAirport) {
	    	if (!jQuery.isEmptyObject(passengersByRemoteAirport)) {
	    		$.each(passengersByRemoteAirport, function(index, remoteAirportPassengers) {
	    			drawAirportLinkPath(airport, remoteAirportPassengers.remoteAirport, remoteAirportPassengers.passengers)
	    		})
	    		showAirportLinkPaths()
	    		//printConsole('Showing all routes with passenger volume flying from or to this airport')
	    	} else {
	    		//printConsole('No routes with passenges yet flying from or to this airport')
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


function drawAirportLinkPath(localAirport, remoteAirport, passengers) {
	var from = new google.maps.LatLng({lat: localAirport.latitude, lng: localAirport.longitude})
	var to = new google.maps.LatLng({lat: remoteAirport.latitude, lng: remoteAirport.longitude})
	var pathKey = remoteAirport.id
	
	var airportLinkPath = new google.maps.Polyline({
			 geodesic: true,
		     strokeColor: "#DC83FC",
		     strokeOpacity: 0.8,
		     strokeWeight: 2,
		     path: [from, to],
		     zIndex : 1100,
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
	     passengers : passengers,
	});
	
	airportLinkPath.shadowPath = shadowPath
	
	var infowindow;
	shadowPath.addListener('mouseover', function(event) {
		$("#airportLinkPopupFrom").html(getCountryFlagImg(this.fromCountry) + this.fromAirport)
		$("#airportLinkPopupTo").html(getCountryFlagImg(this.toCountry) + this.toAirport)
		$("#airportLinkPopupPassengers").text(this.passengers)

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
		infowindow.close()
	})
	
	airportLinkPaths[pathKey] = airportLinkPath
}

function showAirportLinkPaths() {
	$.each(airportLinkPaths, function(key, airportLinkPath) {
		var totalPassengers = airportLinkPath.shadowPath.passengers
		if (totalPassengers < 2000) {
			var newOpacity = 0.2 + totalPassengers / 2000 * (airportLinkPath.strokeOpacity - 0.2)
			airportLinkPath.setOptions({strokeOpacity : newOpacity})
		}
			
		airportLinkPath.setMap(map)
		airportLinkPath.shadowPath.setMap(map)
		polylines.push(airportLinkPath)
		polylines.push(airportLinkPath.shadowPath)
	})
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
		
    $("#hideAirportLinksButton").hide();
	
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


    $('#appealBonusDetailsTooltip .table .table-row').empty()
    $.each(bonusDetails, function(index, entry) {
        var $row = $('<div class="table-row"><div class="cell" style="width: 70%;">' + entry.description + '</div><div class="cell" style="width: 70%;">+' + entry.value + '</div></div>')
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
            $.each(info, function(index, specializationsByScale) {
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
            $('#baseSpecializationModal').fadeIn(500)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});

}

function confirmSpecializations() {
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
}
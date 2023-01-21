var loadedRivals = []
var loadedRivalsById = {}
var loadedRivalLinks = []
var hideInactive = true

function showRivalsCanvas(selectedAirline) {
	setActiveDiv($("#rivalsCanvas"))
	highlightTab($('.rivalsCanvasTab'))
	$('#rivalDetails').hide()
	loadAllRivals(selectedAirline)
}

function toggleHideInactive(flagValue) {
    hideInactive = flagValue
    var selectedSortHeader = $('#rivalsTableSortHeader .table-header .cell.selected')
    updateRivalsTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'), null)
}

function loadAllRivals(selectedAirline) {
	var getUrl = "airlines?loginStatus=true"

	loadedRivals = []
	loadedRivalsById = {}
	$.ajax({
		type: 'GET',
		url: getUrl,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airlines) {
	    	loadedRivals = airlines
	    	$.each(airlines, function(index, airline) {
	    		loadedRivalsById[airline.id] = airline
	    	})
	    	
	    	var selectedSortHeader = $('#rivalsTableSortHeader .table-header .cell.selected')
	    	updateRivalsTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'), selectedAirline)
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

function updateRivalsTable(sortProperty, sortOrder, selectedAirline) {
	if (!selectedAirline) {
		selectedAirline = $("#rivalsCanvas #rivalsTable div.table-row.selected").data('airline-id')
	}
	var rivalsTable = $("#rivalsCanvas #rivalsTable")
	
	rivalsTable.children("div.table-row").remove()

	//filter if necessary
	var displayRivals
	if (hideInactive) {
	    displayRivals = loadedRivals.filter(function(rival) {
                                    	    		  return rival.loginStatus < 3
                                    	    	});
	} else {
	    displayRivals = loadedRivals
    }

	//sort the list
	displayRivals.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	var selectedRow
	$.each(displayRivals, function(index, airline) {
		var row = $("<div class='table-row clickable' data-airline-id='" + airline.id + "' onclick=\"loadRivalDetails($(this), '" + airline.id + "')\"></div>")
//		var countryFlagImg = ""
//		if (airline.countryCode) {
//			countryFlagImg = getCountryFlagImg(airline.countryCode)
//		}

		row.append("<div class='cell'><img src='" + getStatusLogo(airline.loginStatus) + "' title='" + getStatusTitle(airline.loginStatus) + "' style='vertical-align:middle;'/>")
		var $nameDiv = $("<div class='cell' style='vertical-align:unset;'>" + getAirlineSpan(airline.id, airline.name) + getUserLevelImg(airline.userLevel) + getAdminImg(airline.adminStatus) + getUserModifiersSpan(airline.userModifiers) + getAirlineModifiersSpan(airline.airlineModifiers)
				+ (airline.isGenerated ? "<img src='assets/images/icons/robot.png' title='AI' style='vertical-align:middle;'/>" : "") + "</div>").appendTo(row)
		addAirlineTooltip($nameDiv, airline.id, airline.slogan, airline.name)
		if (airline.headquartersAirportName) {
			row.append("<div class='cell'>" + getCountryFlagImg(airline.countryCode) + getAirportText(airline.headquartersCity, airline.headquartersAirportIata) + "</div>")
		} else {
			row.append("<div class='cell'>-</div>")
		}
		row.append("<div class='cell' align='right'>" + airline.reputation + "</div>")
		row.append("<div class='cell' align='right'>" + airline.baseCount + "</div>")

		if (selectedAirline == airline.id) {
			row.addClass("selected")
			selectedRow = row
		}
		
		rivalsTable.append(row)
	});
	
	if (selectedRow) {
	    selectedRow[0].scrollIntoView()
		loadRivalDetails(selectedRow, selectedAirline)
	}
}

function getStatusLogo(status) {
    if (status == 0) {
      return "assets/images/icons/12px/status-green.png"
    } else if (status == 1) {
      return "assets/images/icons/12px/status-yellow.png"
    } else if (status == 2) {
      return "assets/images/icons/12px/status-orange.png"
    } else {
      return "assets/images/icons/12px/status-grey.png"
    }
}

function getStatusTitle(status) {
    if (status == 0) {
      return "Online"
    } else if (status == 1) {
      return "Active within last 7 days"
    } else if (status == 2) {
      return "Active within last 30 days"
    } else {
      return "Inactive"
    }
}


function toggleRivalsTableSortOrder(sortHeader) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}
	
	$('#rivalsTableSortHeader .cell').removeClass("selected")
	$('#rivalIdSortButton').removeClass("selected")
	sortHeader.addClass("selected")
	
	updateRivalsTable(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
}

function loadRivalDetails(row, airlineId) {
    if (row === null) { //find the row
        row = $('#rivalsCanvas #rivalsTable div.table-row[data-airline-id="' + airlineId + '"]');
    }
	//update table
	row.siblings().removeClass("selected")
	row.addClass("selected")
	
	
	updateRivalBasicsDetails(airlineId)
	updateRivalFormerNames(airlineId)
	updateRivalFleet(airlineId)
	updateRivalCountriesAirlineTitles(airlineId)
	updateRivalChampionedAirportsDetails(airlineId)
	updateHeadquartersMap($('#rivalDetails .headquartersMap'), airlineId)
	loadRivalLinks(airlineId)
	
	updateRivalBaseList(airlineId)

    if (isAdmin()) {
        showAdminActions(loadedRivalsById[airlineId])
    }

	$('#rivalDetails').data("airlineId", airlineId)

	$('#rivalDetails').fadeIn(200)
}

function loadRivalLinks(airlineId) {
	var airlineLinksTable = $("#rivalsCanvas #rivalLinksTable")
	airlineLinksTable.children("div.table-row").remove()
	
	var getUrl = "airlines/" + airlineId + "/links"
	loadedRivalLinks = undefined
	$.ajax({
		type: 'GET',
		url: getUrl,
		contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(links) {
	    	loadedRivalLinks = links
	    	updateRivalLinksTable()
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateRivalLinksTable(sortProperty, sortOrder) {
	var rivalLinksTable = $("#rivalsCanvas #rivalLinksTable")
	
	rivalLinksTable.children("div.table-row").remove()
	
	//sort the list
	loadedRivalLinks.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	$.each(loadedRivalLinks, function(index, link) {
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell'>" + getCountryFlagImg(link.fromCountryCode) + getAirportText(link.fromAirportCity, link.fromAirportCode) + "</div>")
		row.append("<div class='cell'>" + getCountryFlagImg(link.toCountryCode) + getAirportText(link.toAirportCity, link.toAirportCode) + "</div>")
		row.append("<div class='cell' align='right'>" + link.distance + "km</div>")
		link.totalCapacity = link.capacity.economy + link.capacity.business + link.capacity.first
		row.append("<div class='cell' align='right'>" + link.totalCapacity + "</div>")
		
		rivalLinksTable.append(row)
	});
}

function toggleRivalLinksTableSortOrder(sortHeader) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}
	
	sortHeader.siblings().removeClass("selected")
	sortHeader.addClass("selected")
	
	updateRivalLinksTable(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
}

function updateRivalBasicsDetails(airlineId) {
	var rival = loadedRivalsById[airlineId]
	$("#rivalsCanvas .airlineName").text(rival.name)
	$("#rivalsCanvas .airlineCode").text(rival.airlineCode)
	var color = airlineColors[airlineId]
	if (!color) {
		$("#rivalsCanvas .airlineColorDot").hide()
	} else {
		$("#rivalsCanvas .airlineColorDot").css('background-color', color);
		$("#rivalsCanvas .airlineColorDot").show()
	}
	
	$("#rivalsCanvas .airlineGrade").html(getGradeStarsImgs(rival.gradeValue))
	$("#rivalsCanvas .airlineGrade").attr('title', rival.gradeDescription)

	$("#rivalsCanvas .alliance").data("link", "alliance")
	populateNavigation($("#rivalsCanvas .alliance"))
	if (rival.allianceName) {
		$("#rivalsCanvas .alliance").text(rival.allianceName)
		$("#rivalsCanvas .alliance").addClass("clickable")
		$("#rivalsCanvas .alliance").on("click.showAlliance", function() {
		    showAllianceCanvas(rival.allianceId)
		})
	} else {
		$("#rivalsCanvas .alliance").text('-')
		$("#rivalsCanvas .alliance").removeClass("clickable")
		$("#rivalsCanvas .alliance").off("click.showAlliance")
	}

}

function updateRivalFormerNames(airlineId) {
    $('#rivalDetails .formerNames').children('div.table-row').remove()

	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/former-names",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	$(result).each(function(index, formerName) {
                var row = $("<div class='table-row'></div>")
                row.append("<div class='cell'>" + formerName + "</div>")
                $('#rivalDetails .formerNames').append(row)
            })
            if (result.length == 0) {
                $('#rivalDetails .formerNameRow').hide()
            } else {
                $('#rivalDetails .formerNameRow').show()
            }
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});

}

function updateRivalFleet(airlineId) {
    $('#rivalDetails .fleetList').children('div.table-row').remove()

	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/fleet",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	$(result).each(function(index, modelDetails) {
                var row = $("<div class='table-row'></div>")
                row.append("<div class='cell'>" + modelDetails.name + "</div>")
                row.append("<div class='cell'>" + modelDetails.quantity + "</div>")
                $('#rivalDetails .fleetList').append(row)
            })
            if (result.length == 0) {
                $('#rivalDetails .fleetList').append('<div class="cell"></div><div class="cell"></div>')
            }
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});

}


function updateRivalChampionedAirportsDetails(airlineId) {
	$('#rivalChampionedAirportsList').children('div.table-row').remove()

	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/championed-airports",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(championedAirports) {
	    	$(championedAirports).each(function(index, championDetails) {
                var row = $("<div class='table-row clickable' data-link='airport' onclick=\"showAirportDetails('" + championDetails.airportId + "');\"></div>")
                row.append("<div class='cell'>" + getRankingImg(championDetails.ranking) + "</div>")
                row.append("<div class='cell'>" + getCountryFlagImg(championDetails.countryCode) + championDetails.airportText + "</div>")
                row.append("<div class='cell'>" + championDetails.reputationBoost + "</div>")
                $('#rivalChampionedAirportsList').append(row)
            })

            populateNavigation($('#rivalChampionedAirportsList'))
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateRivalCountriesAirlineTitles(airlineId) {
	$('#rivalsCanvas .nationalAirlineCountryList').children('div.table-row').remove()
	$('#rivalsCanvas .partneredAirlineCountryList').children('div.table-row').remove()

	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/country-airline-titles",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(titles) {
	    	$(titles.nationalAirlines).each(function(index, entry) {
	    	    var country = loadedCountriesByCode[entry.countryCode]
	    		var row = $("<div class='table-row clickable' onclick=\" showCountryView('" + country.countryCode + "');\"></div>")
	    		row.append("<div class='cell'>" + getCountryFlagImg(entry.countryCode) + country.name + "</div>")
	    		row.append("<div class='cell'>" + entry.bonus + "</div>")
	    		$('#rivalsCanvas .nationalAirlineCountryList').append(row)
	    	})

	    	if (titles.nationalAirlines.length == 0) {
	    		$('#rivalsCanvas .nationalAirlineCountryList').append($("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div></div>"))
	    	}

	    	$(titles.partneredAirlines).each(function(index, entry) {
                var country = loadedCountriesByCode[entry.countryCode]
                var row = $("<div class='table-row clickable' onclick=\"showCountryView('" + country.countryCode + "');\"></div>")
                row.append("<div class='cell'>" + getCountryFlagImg(entry.countryCode) + country.name + "</div>")
                row.append("<div class='cell'>" + entry.bonus + "</div>")
                $('#rivalsCanvas .partneredAirlineCountryList').append(row)
            })

            if (titles.partneredAirlines.length == 0) {
	    		$('#rivalsCanvas .partneredAirlineCountryList').append($("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div></div>"))
	    	}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateRivalBaseList(airlineId) {
    updateAirlineBaseList(airlineId, $('#rivalBases'))
}

var rivalMapAirlineId

function showRivalMap() {
    var airlineId = $('#rivalDetails').data("airlineId")
	clearAllPaths()
	deselectLink()
    rivalMapAirlineId = airlineId
	var paths = []

    var getUrl = "airlines/" + airlineId + "/links"
	$.ajax({
		type: 'GET',
		url: getUrl,
		contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(links) {
	    	$.each(links, function(index, link) {
                    var path = drawFlightPath(link, "#DC83FC")
                    paths.push(path)
                    //remove on click add on hover
                    google.maps.event.clearInstanceListeners(path.shadow);
                    var infoWindow
                    path.shadow.addListener('mouseover', function(event) {
                        var fromAirport = getAirportText(link.fromAirportCity, link.fromAirportCode)
                    	var toAirport = getAirportText(link.toAirportCity, link.toAirportCode)
                        $("#linkPopupFrom").html(getCountryFlagImg(link.fromCountryCode) + "&nbsp;" + fromAirport)
                        $("#linkPopupTo").html(getCountryFlagImg(link.toCountryCode) + "&nbsp;" + toAirport)
                        $("#linkPopupCapacity").html(link.capacity.total)
                        $("#linkPopupAirline").html(getAirlineSpan(link.airlineId, link.airlineName))

                        infoWindow = new google.maps.InfoWindow({
                             maxWidth : 1200});

                        var popup = $("#linkPopup").clone()
                        popup.show()
                        infoWindow.setContent(popup[0])

                        infoWindow.setPosition(event.latLng);
                        infoWindow.open(map);
                    })
                    path.shadow.addListener('mouseout', function(event) {
                        infoWindow.close();
                        infoWindow.setMap(null);
                    })
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



    $.ajax({
    		type: 'GET',
    		url: "airlines/" + airlineId + "/bases",
    	    contentType: 'application/json; charset=utf-8',
    	    dataType: 'json',
    	    success: function(bases) {
    	    	updateAirportBaseMarkers(bases, paths)
    	    },
    	    error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    });
    window.setTimeout(function() {
        if (map.controls[google.maps.ControlPosition.TOP_CENTER].getLength() > 0) {
            map.controls[google.maps.ControlPosition.TOP_CENTER].clear()
        }
        map.controls[google.maps.ControlPosition.TOP_CENTER].push(createMapButton(map, 'Exit Rival Flight Map', 'hideRivalMap()', 'hideRivalMapButton')[0]);
    }, 1000); //delay otherwise it doesn't push to center
    switchMap()
    $("#worldMapCanvas").data("initCallback", function() { //if go back to world map, re-init the map
    	map.controls[google.maps.ControlPosition.TOP_CENTER].clear()
    	clearAllPaths()
        updateAirportMarkers(activeAirline)
        updateLinksInfo() //redraw all flight paths
    })
}

function hideRivalMap() {
	map.controls[google.maps.ControlPosition.TOP_CENTER].clear()
	clearAllPaths()
	updateAirportBaseMarkers([]) //revert base markers
	rivalMapAirlineId = undefined
	setActiveDiv($("#rivalsCanvas"))
}
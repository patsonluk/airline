var loadedOlympicsEvents = []
var loadedAlerts = []


function showEventCanvas() {
	setActiveDiv($("#eventCanvas"))
	highlightTab($('.eventCanvasTab'))
	loadAllOlympics()
}

function loadAllOlympics() {
	var url = "event/olympics"
	
	loadedOlympicsEvents = []

	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(olympicsEvents) {
	    	loadedOlympicsEvents = olympicsEvents
	    	var selectedSortHeader = $('#olympicsTableSortHeader .table-header .cell.selected')
	    	updateOlympicTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function updateOlympicTable(sortProperty, sortOrder) {
	var olympicsTable = $("#eventCanvas #olympicsTable")
	
	olympicsTable.children("div.table-row").remove()
	
	//sort the list
	loadedOlympicsEvents.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	$.each(loadedOlympicsEvents, function(index, event) {
		var row = $("<div class='table-row clickable'></div>")
		row.append("<div class='cell'>" + event.startCycle + "</div>")
		if (event.hostCity) {
		    row.append("<div class='cell'>" + event.hostCity + "</div>")
        } else {
            row.append("<div class='cell'>(Voting)</div>")
        }
		row.append("<div class='cell'>" + event.remainingDuration + " week(s)</div>")
		row.append("<div class='cell'>" + event.status + "</div>")

		row.click(function() {
		   loadOlympicsDetails(event.id)
		})

		olympicsTable.append(row)
	});

	if (loadedOlympicsEvents.length == 0) {
	    olympicsTable.append("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div></div>")
	}
}

function loadOlympicsDetails(eventId) {
    var candidatesTable = $('#olympicsCandidatesTable')
  	candidatesTable.children("div.table-row").remove()

  	var votingRoundsTable = $("#olympicsVotingRoundsTable")
  	votingRoundsTable.empty()

  	$("#olympicsDetails .result").empty()

    $.ajax({
    		type: 'GET',
    		url: "event/olympics/" + eventId,
    	    contentType: 'application/json; charset=utf-8',
    	    dataType: 'json',
    	    success: function(details) {
    	        var votingRoundHeaderRow = $("<div class='table-header'></div>")
    	        var votingRoundColumns = []
    	        votingRoundHeaderRow.append($("<div class='cell' style='width: 9%'>Round</div>"))

    	        $.each(details.candidates, function(index, candidate) {
                    var candidateRow = $("<div class='table-row'></div>")
                    candidateRow.append("<div class='cell'>" + getCountryFlagImg(candidate.countryCode) + candidate.city + "</div>")
               		candidatesTable.append(candidateRow)

                    //populate header for votingRoundsTable
                    votingRoundHeaderRow.append("<div class='cell' style='width: 13%'>" + getCountryFlagImg(candidate.countryCode) + candidate.city + "</div>")
                    votingRoundColumns.push(candidate.airportId)
                })
                votingRoundHeaderRow.append("<div class='cell' style='width: 13%'>Eliminated</div>")
                votingRoundsTable.append(votingRoundHeaderRow)

                if (details.votingRounds) {
                    $.each(details.votingRounds, function(index, votingRound) {
                        var votes = votingRound.votes
                        var row = $("<div class='table-row'><div class='cell'>" + (index + 1) + "</div></div>")
                        $.each(votingRoundColumns, function(columnIndex, airportId) {
                            if (votingRound.has(airportId)) {
                                row.append($("<div class='cell'>" + votes[airportId] + "</div>"))
                            } else {
                                row.append("<div class='cell'>-</div>")
                            }
                        })
                        row.append("<div class='cell'>" + votingRound.eliminatedAirport.city + "</div>")
                        votingRoundsTable.append(row)
                    })
                    $("#olympicsDetails .result").text(details.selectedAirport.city)
                } else { //fill with empty rows
                    for (i = 1 ; i < details.candidates.length; i ++) {
                        var emptyRow = $("<div class='table-row'></div>")
                        emptyRow.append($("<div class='cell'>" + i + "</div>"))
                        for (j = 0 ; j < details.candidates.length; j ++) {
                            emptyRow.append($("<div class='cell'>-</div>"))
                        }
                        emptyRow.append($("<div class='cell'>-</div>")) //eliminated column
                        votingRoundsTable.append(emptyRow)
                    }
                }

                if (activeAirline) {
                    $("#olympicsDetails .button.vote").show();
                    $("#olympicsDetails .button.vote").off("click").on("click", function() {
                        populateCityVoteModal(eventId, details.candidates)
                        showOlympicsVoteModal()
                    })
                } else {
                    $("#olympicsDetails .button.vote").hide();
                }

    	    	$("#olympicsDetails").fadeIn(200)
    	    },
    	    error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    	});
}

function populateCityVoteModal(eventId, candidates) {
    $("#olympicsVoteModal").data("eventId", eventId)
    $.ajax({
        type: 'GET',
        url: "event/olympics/" + eventId + "/airlines/" + activeAirline.id + "/votes",
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            //TODO weight
            var table = $("#olympicsCityVoteTable")

            if (!olympicsVoteMaps) {
                var mapDivs = initVoteLayout(table, candidates.length)
                initOlympicsVoteMaps(mapDivs)
            }

            var airport = candidates[0]


            table.find(".cityName").each(function(index) {
                $(this).html(getCountryFlagImg(candidates[index].countryCode) + candidates[index].city)
            })

            $.each(olympicsMapElements, function() { this.setMap(null)})
            olympicsMapElements = []

            table.find(".number-button").each(function(index) {
                var airportId = candidates[index].id
                $(this).data("airportId", airportId)
                $(this).empty()
                if (result.precedence) {
                    $(this).data("precedence", result.precedence[airportId])
                    $(this).append("<span>" + result.precedence[airportId] + "</span>")
                }
            })

            $.each(olympicsVoteMaps, function(index, map) {
                var candidateInfo = candidates[index]
                populateOlympicsCityMap(map, candidateInfo)
            })
        },
        error: function(jqXHR, textStatus, errorThrown) {
        	            console.log(JSON.stringify(jqXHR));
        	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        	    }
    })


}

var currentVotePrecedence = 1

var olympicsMapElements = []
var candidateCount

function populateOlympicsCityMap(map, candidateInfo) {
    var principalAirport = candidateInfo
    if (principalAirport.latitude > 45 || principalAirport.latitude < -45) {
        map.setZoom(6)
    } else {
        map.setZoom(7)
    }
    map.setCenter({lat: principalAirport.latitude, lng: principalAirport.longitude}); //this would eventually trigger an idle

    var airportMapCircle = new google.maps.Circle({
                center: {lat: principalAirport.latitude, lng: principalAirport.longitude},
                radius: 80000, //in meter
                strokeColor: "#32CF47",
                strokeOpacity: 0.2,
                strokeWeight: 2,
                fillColor: "#32CF47",
                fillOpacity: 0.3,
                map: map
            });
    olympicsMapElements.push(airportMapCircle)

    $.each(candidateInfo.affectedAirports, function(index, airport) {
        var icon = getAirportIcon(airport)
        var position = {lat: airport.latitude, lng: airport.longitude};
          var marker = new google.maps.Marker({
                position: position,
                map: map,
                airport: airport,
                icon : icon
              });

            var infowindow
           	marker.addListener('mouseover', function(event) {
           		$("#olympicAirportPopup .airportName").text(airport.name + "(" + airport.iata + ")")
           		infowindow = new google.maps.InfoWindow({
           		       content: $("#olympicAirportPopup").html(),
           		       maxWidth : 800,
                       disableAutoPan : true
                 });


           		infowindow.open(map, marker);
           	})
           	marker.addListener('mouseout', function(event) {
           		infowindow.close()
           		infowindow.setMap(null)
           	})
           	olympicsMapElements.push(marker)

     })



    google.maps.event.addListenerOnce(map, 'idle', function() {
        setTimeout(function() { //set a timeout here, otherwise it might not render part of the map...
            map.setCenter({lat: principalAirport.latitude, lng: principalAirport.longitude}); //this would eventually trigger an idle
            google.maps.event.trigger(map, 'resize'); //this refreshes the map
        }, 2000);
    });
}

function voteOlympicsCity(numberButton) {
    var airportId = numberButton.data("airportId")

    if (!numberButton.data("precedence")) {
        numberButton.data("precedence", currentVotePrecedence)
        currentVotePrecedence ++
    }

    if (currentVotePrecedence > candidateCount) {
        enableButton($("#olympicsVoteModal .confirm"))
    } else {
        disableButton($("#olympicsVoteModal .confirm"), "Must fill in precedence for all cities")
    }

}

function initVoteLayout(table, candidateCount) {
    var mapDivs = []
    this.candidateCount = candidateCount
    for (i = 0 ; i < candidateCount; i ++) {
        voteDiv = $("<div style='float: left; width: 33%; padding: 5px; box-sizing: border-box;'></div>")
        var titleDiv = $("<div style='display: flex; align-items: center;'></div>") //button + city name
        var numberButton = $("<a href='#' class='round-button number-button' onclick=voteOlympicsCity($(this))></a>")

        numberButton.hover(
            function() {
                var $this = $(this); // caching $(this)
                if (!$this.data("precedence")) {
                    $this.append("<span>" + currentVotePrecedence + "</span>");
                }
            },
            function() {
                var $this = $(this); // caching $(this)
                if (!$this.data("precedence")) {
                    $this.empty();
                }
            }
        );
        titleDiv.append(numberButton)
        titleDiv.append("<span class='cityName'></span>")
        voteDiv.append(titleDiv)
        var mapDiv = $("<div class='olympicsMap' style='width: 100%; padding-top: 100%;'></div>")
        voteDiv.append(mapDiv)
        table.append(voteDiv)

        mapDivs.push(mapDiv)
    }

    return mapDivs
}

var olympicsVoteMaps
function initOlympicsVoteMaps(mapDivs) { //only called once, see https://stackoverflow.com/questions/10485582/what-is-the-proper-way-to-destroy-a-map-instance
    olympicsVoteMaps = []
    for (i = 0 ; i < mapDivs.length; i ++) {
        olympicsVoteMaps.push(new google.maps.Map(mapDivs[i][0], {
                        gestureHandling: 'none',
                        disableDefaultUI: true,
                        styles: getMapStyles()
                    }))
    }


}


function toggleTableSortOrder(sortHeader, updateTableFunction) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}
	
	sortHeader.siblings().removeClass("selected")
	sortHeader.addClass("selected")
	
	updateTableFunction(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
}

function toggleOlympicTableSortOrder(sortHeader) {
    toggleTableSortOrder(sortHeader, updateOlympicTable)
}

function confirmOlympicsVotes() {
    var airlineId = activeAirline.id
	var data = { } //airportId : precedence
	$("#olympicsVoteModal .number-button").each(function() {
	    data[$(this).data("airportId")] = $(this).data("precedence")
	})

    var eventId = $("#olympicsVoteModal").data("eventId")
	$.ajax({
		type: 'PUT',
		url: "event/olympics/" + eventId + "/airlines/" + activeAirline.id + "/votes",
	    data: JSON.stringify(data),
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airline) {
	        closeModal($("#olympicsVoteModal"))
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function revertOlympicsVotes() {
    currentVotePrecedence = 1
    $("#olympicsCityVoteTable .number-button").each(function() {
        $(this).empty()
        $(this).removeData()
    })


    enableButton($("#olympicsVoteModal .button.confirm"))
}

function showOlympicsVoteModal() {
    $("#olympicsVoteModal").fadeIn(200)
}

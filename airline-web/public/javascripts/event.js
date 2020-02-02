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



    	    	$("#olympicsDetails").fadeIn(200)
    	    },
    	    error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    	});
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

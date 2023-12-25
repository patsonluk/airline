function showRankingCanvas() {
	setActiveDiv($("#rankingCanvas"))
	highlightTab($('.rankingCanvasTab'))
	
	loadRanking()
}

function loadRanking() {
	var airlineId = activeAirline ? activeAirline.id : null
    var url = activeAirline ? "rankings/" + airlineId : "rankings" 

	$('#rankingCanvas .table').hide() //hide all tables until they are loaded
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(allRankings) {
	    	$.each(allRankings, function(rankingType, rankings) {
	    		updateRankingTable(rankingType, rankings)
	    	})
	    	$('#rankingCanvas .table').fadeIn(200)
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

function updateRankingTable(rankingType, rankings) {
	//locate which table
	var rankingTable;
	if (rankingType == "PASSENGER") {
		rankingTable = $('#passengerRank')
	} else if (rankingType == "PASSENGER_MILE") {
		rankingTable = $('#passengerMileRank')
	} else if (rankingType == "REPUTATION") {
		rankingTable = $('#reputationRank')
	} else if (rankingType == "SERVICE_QUALITY") {
		rankingTable = $('#serviceQualityRank')
	} else if (rankingType == "LINK_COUNT") {
		rankingTable = $('#linkCountRank')
	} else if (rankingType == "LINK_PROFIT") {
		rankingTable = $('#linkProfitRank')
	} else if (rankingType == "LOUNGE") {
		rankingTable = $('#loungeRank')
	} else if (rankingType == "AIRPORT") {
		rankingTable = $('#airportRank')
    } else if (rankingType == "INTERNATIONAL_PAX") {
        rankingTable = $('#internationPaxRank')
    } else if (rankingType == "DOMESTIC_PAX") {
        rankingTable = $('#domesticPaxRank')
	} else if (rankingType == "PASSENGER_AS") {
		rankingTable = $('#passengerRankAs')
	} else if (rankingType == "PASSENGER_AF") {
		rankingTable = $('#passengerRankAf')
	} else if (rankingType == "PASSENGER_OC") {
		rankingTable = $('#passengerRankOc')
	} else if (rankingType == "PASSENGER_EU") {
		rankingTable = $('#passengerRankEu')
	} else if (rankingType == "PASSENGER_NA") {
		rankingTable = $('#passengerRankNa')
	} else if (rankingType == "PASSENGER_SA") {
		rankingTable = $('#passengerRankSa')
	} else {
		console.log("Unknown ranking type " + rankingType)
	}
	
	if (rankingTable) {
		rankingTable.children('.table-row').remove()
		var maxEntry = 20
		var currentAirlineRanking;
		$.each(rankings, function(index, ranking) {
			if (index < maxEntry) {
				rankingTable.append(getRankingRow(ranking))
			}
			if (activeAirline && !currentAirlineRanking && ranking.airlineId == activeAirline.id) {
				currentAirlineRanking = ranking
			}
		})
		
		if (currentAirlineRanking) {
			rankingTable.append(getDividerRow())
			rankingTable.append(getRankingRow(currentAirlineRanking)) //lastly append a row of current airline
		}
		
	}
}

function getRankingRow(ranking) {
	var row = $("<div class='table-row'></div>")
	row.append("<div class='cell'>" + ranking.rank + "</div>")
	row.append("<div class='cell'>" + getMovementLabel(ranking.movement) + "</div>")
	if (ranking.airlineId) {
		var entry = getAirlineSpan(ranking.airlineId, ranking.airlineName)
		if (ranking.rankInfo) {
		    if (ranking.rankInfo.from && ranking.rankInfo.to) {
		        entry += ' : ' + "<span style='vertical-align:bottom'>" + getAirportSpan(ranking.rankInfo.from) + "<img style='vertical-align:bottom; margin:0 3px;' src='assets/images/icons/12px/arrow-double.png'/>" + getAirportSpan(ranking.rankInfo.to) + "</span>"
		    } else {
			    entry += ' : ' + ranking.rankInfo
            }
		}
		var $airlineDiv = $("<div class='cell'>" + entry + "</div>").appendTo(row)
		addAirlineTooltip($airlineDiv, ranking.airlineId, ranking.airlineSlogan, ranking.airlineName)
	} else if (ranking.airportId) {
		var entry = getCountryFlagImg(ranking.countryCode) + ranking.iata + " : " + ranking.airportName 
		row.append("<div class='cell'>" + entry + "</div>")
	} else if (ranking.airport1 && ranking.airport2) {
		var entry = getAirportSpan(ranking.airport1) + "<img style='vertical-align:bottom; margin:0 3px;' src='assets/images/icons/12px/arrow-double.png'/>" + getAirportSpan(ranking.airport2)
        row.append("<div class='cell'>" + entry + "</div>")
	}
	row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(ranking.rankedValue) + "</div>")
	
	
	return row
}

function getDividerRow() {
	var row = $("<div class='table-row'></div>")
	row.append("<div class='cell' style='border-top: 1px solid #6093e7; padding: 0;'></div>")
	row.append("<div class='cell' style='border-top: 1px solid #6093e7; padding: 0;'></div>")
	row.append("<div class='cell' style='border-top: 1px solid #6093e7; padding: 0;'></div>")
	row.append("<div class='cell' style='border-top: 1px solid #6093e7; padding: 0;'></div>")
	
	return row
}


function getMovementLabel(movement) {
	if (movement == 0) {
		return '-'
	} else if (movement < 0) { //going up in ranking
		return "<img src='assets/images/icons/12px/arrow-090.png'/>" + movement * -1
	} else {
		return "<img src='assets/images/icons/12px/arrow-270.png'/>" + movement
	}
}
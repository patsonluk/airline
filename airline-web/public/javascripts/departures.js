function showDeparturesCanvas() {
	setActiveDiv($("#departuresCanvas"))
	$('#main-tabs').children('.left-tab').children('span').removeClass('selected')
	loadDepartures($('#airportPopupId').val())
}

function loadDepartures(airportId) {
	//$('#rankingCanvas .table').hide() //hide all tables until they are loaded
	$('.departures').children('div.table-row').remove()
	var currentTime = new Date()
	currentMinute =  currentTime.getMinutes()
	currentHour = currentTime.getHours()
	currentDay = currentTime.getDay()
	
	$.ajax({
		type: 'GET',
		url: "/airports/" + airportId + "/departures/" + currentDay + "/" + currentHour + "/" + currentMinute,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(departures) {
	    	updateDepartures(departures)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateDepartures(allDepartures) {
	var boardSize = 30
	for (var i = 0 ; i < 3; i++) { //how many boards
		var tableId = '#departures' + (i + 1)
		var departuresTable = $(tableId)
		
		var departures = allDepartures.slice(i * boardSize, (i + 1) * boardSize)
		
		var filledRows = departures.length
		$.each(departures, function(index, departure){
			var row = $("<div class='table-row' style='position:relative;'></div>")
			row.append("<div class='cell'>" + departure.timeSlotTime + "</div>")
			row.append("<div class='cell'>" + departure.flightCode + "</div>")
			//row.append("<div class='cell' style='width:50%'>" + departure.destination + "</div>")
			row.append("<div class='cell' style='overflow:hidden; float:left; text-overflow: clip; white-space: nowrap;'>" + departure.destination + "</div>")
			var statusDiv = $("<div class='cell'>" + departure.statusText + "</div>")
			if (departure.statusCode == 'FINAL_CALL') {
				 statusDiv.addClass('blink')
			}
			row.append(statusDiv) 
			
			
			departuresTable.append(row)
		})
		
		for (var j = 0; j < boardSize - filledRows ; j ++) {
			var emptyRow = $("<div class='table-row' style='position:relative;'></div>")
			emptyRow.append("<div class='cell'>&nbsp;</div>")
			emptyRow.append("<div class='cell'>&nbsp;</div>")
			emptyRow.append("<div class='cell'>&nbsp;</div>")
			emptyRow.append("<div class='cell'>&nbsp;</div>")
			
			departuresTable.append(emptyRow)
		}
		
	}	
	
}
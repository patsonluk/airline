function showDeparturesCanvas() {
	setActiveDiv($("#departuresCanvas"))
	$('#main-tabs').children('.left-tab').children('span').removeClass('selected')
	loadDepartures($('#airportPopupId').val())
}

function loadDepartures(airportId) {
	//$('#rankingCanvas .table').hide() //hide all tables until they are loaded
	$('.departures').children('div.table-row').remove()
	$('#weather').hide()
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
	    	updateWeather(departures.weatherIcon, departures.temperature, departures.weatherDescription)
	    	updateDepartures(departures.timeslots)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateWeather(weatherIcon, temperature, weatherDescription) {
	$('#weatherIcon').attr('src', weatherIcon)
	$('#temperature').html(temperature + ' &#8451;')
	$('#weatherDescription').text(weatherDescription)
	$('#weather').show()
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
			row.append("<div class='cell' style='vertical-align:middle;'>" + departure.timeSlotTime + "</div>")
			row.append("<div class='cell' style='vertical-align:middle;'>" + getAirlineLogoImg(departure.airlineId) + "&nbsp;" +  departure.flightCode + "</div>")
		    row.append("<div class='cell' style='vertical-align:middle;'><div style=' white-space: nowrap;  text-overflow: ellipsis; display: block; overflow: hidden; width: 155px; height: 14px;'>" + departure.destination + "</div></div>")
		    
			//row.append("<div class='cell' style='overflow:hidden; float:left; text-overflow: clip; white-space: nowrap; height: 100%;'>" + departure.destination + "</div>")
			var statusDiv = $("<div class='cell' style='vertical-align:middle;'><span>" + departure.statusText + "" +
					"</span></div>")
			if (departure.statusCode == 'FINAL_CALL') {
				 statusDiv.children('span').addClass('blink')
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
var christmasFlag = true

function initSantaClaus() {
    $('#santaClausModal').hide()
    $("#santaClausButton").hide() //valid target
    updateSantaClausModal()
}

function updateSantaClausModal() {
    var table = $("#santaClausGuessTable")
    table.children(".table-row").remove()

    var url = "santa-claus/attempt-status/" + activeAirportId + "/" + activeAirline.id
    $.ajax({
    		type: 'GET',
    		url: url,
    	    contentType: 'application/json; charset=utf-8',
    	    dataType: 'json',
    	    success: function(result) {
    	        if ($.isEmptyObject(result)) { //not a valid target
    	            return
    	        }
    	        $.each(result.guesses, function(index, guess) {
                         	var row = $("<div class='table-row'></div>")
                    		row.append("<div class='cell'>" + getAirportText(guess.city, guess.airportName) + "</div>")
                            row.append("<div class='cell'>" + guess.distanceText + "</div>")
                    		table.append(row)
                    	});


                 $("#santaClausAttemptsLeft").text(result.attemptsLeft)

                 var awardOptionsDiv = $("#santaClausRewardOptions")
                 var pickedRewardDiv = $("#santaClausPickedReward")
                 if (result.found) {
                    $("#santaClausModal .guessButton").hide()
                    if (result.pickedAwardDescription) { //found and award is chosen
                        awardOptionsDiv.hide()
                        $("#santaClausPickedRewardText").text(result.pickedAwardDescription)
                        pickedRewardDiv.show()
                    } else { //show award options
                        pickedRewardDiv.hide()
                        getAwardOptionsTable()
                        awardOptionsDiv.show()
                    }
                 } else {
                    pickedRewardDiv.hide()
                    awardOptionsDiv.hide()
                    if (result.attemptsLeft <= 0) {
                        $("#santaClausModal .guessButton").hide()
                        $("#santaClausExhausted").show()
                    } else {
                        $("#santaClausModal .guessButton").show()
                    }
                 }
                 $("#santaClausButton").show() //valid target

    	    },
            error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    	});
}

function showSantaClausAttemptStatus() {
    $('#santaClausModal').fadeIn(200)

}

function getAwardOptionsTable() {
    var url = "santa-claus/award-options/" + activeAirline.id
    var table = $("#santaClausRewardOptionsTable")
    table.children(".table-row").remove()

    $.ajax({
    		type: 'GET',
    		url: url,
    	    contentType: 'application/json; charset=utf-8',
    	    dataType: 'json',
    	    success: function(result) {
    	    	$.each(result, function(index, option) {
                    var row = $("<div class='table-row'></div>")
                    row.append("<div class='cell'><div class='button' onclick=pickSantaClassAward(" + option.id + ")>X</div></div>")
                    row.append("<div class='cell label'>" + option.description + "</div>")
                    table.append(row)
                });
    	    },
            error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    	});
}

function pickSantaClassAward(optionId) {
	var url = "santa-claus/pick-award/" + activeAirline.id + "/" + optionId
	$.ajax({
        		type: 'GET',
        		url: url,
        	    contentType: 'application/json; charset=utf-8',
        	    dataType: 'json',
        	    success: function(result) {
        	    	updateSantaClausModal() //this refresh the table
                    updateAirlineInfo(activeAirline.id)
        	    },
                error: function(jqXHR, textStatus, errorThrown) {
        	            console.log(JSON.stringify(jqXHR));
        	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        	    }
        	});
}

function guessSantaClaus() {
	var url = "santa-claus/guess/" + activeAirportId + "/" + activeAirline.id

	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	updateSantaClausModal() //this refresh the modal and shows reward if it's the correct guess
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

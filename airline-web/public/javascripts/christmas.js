var christmasFlag = false
var santaFound = false

function initSantaClaus() {
    $('#santaClausModal').hide()
    $("#santaClausButton").hide() //valid target
    if (activeAirline) {
        updateSantaClausModal()
    }
}

function closeSantaClausModal() {

    closeModal($('#santaClausModal'))
    removeSnowflakes($('#santaClausModal'))
    removeConfetti($('#santaClausModal'))
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
                    		row.append("<div class='cell label'>" + getAirportText(guess.city, guess.airportCode) + "</div>")
                            row.append("<div class='cell label'>" + guess.distanceText + "</div>")
                    		table.append(row)
                    	});


                 $("#santaClausAttemptsLeft").text(result.attemptsLeft)

                 var awardOptionsDiv = $("#santaClausRewardOptions")
                 var pickedRewardDiv = $("#santaClausPickedReward")
                 var exhaustedDiv = $("#santaClausExhausted")
                 var guessButton = $("#santaClausModal .guessButton")
                 awardOptionsDiv.hide()
                 pickedRewardDiv.hide()
                 exhaustedDiv.hide()
                 guessButton.hide()

                 var flipped = santaFound != result.found
                 santaFound = result.found
                 if ($("#santaClausModal").is(":visible") && flipped) { //a flip of result, and current modal is visible, apply confetti/snowflake
                    refreshBackgroundAnimation()
                 }

                 if (result.found) {
                    if (result.pickedAwardDescription) { //found and award is chosen
                        $("#santaClausPickedRewardText").text(result.pickedAwardDescription)
                        pickedRewardDiv.show()
                    } else { //show award options
                        getAwardOptionsTable()
                        awardOptionsDiv.show()
                    }
                 } else {
                    if (result.attemptsLeft <= 0) {
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
    refreshBackgroundAnimation()

    $('#santaClausModal').fadeIn(200)

}

function refreshBackgroundAnimation() {
    removeSnowflakes($('#santaClausModal'))
    removeConfetti($('#santaClausModal'))
    if (santaFound) {
        showConfetti($("#santaClausModal"))
    } else {
        putSnowflakes($("#santaClausModal"), snowflakeCount)
    }
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
                    //row.append("<div class='cell'><a href='#' class='round-button tick' onclick=pickSantaClassAward(" + option.id + ")></a></div>")
                    row.append("<div class='cell'><a href='#' class='round-button tick' onclick=pickSantaClassAward(" + option.id + ")></a></div>")
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



var christmasMarker = false
var snowflakeCount = 50
function toggleChristmasMarker() {
	if (!christmasMarker) {
		currentAnimationStatus = true
		christmasMarker = true
		document.getElementById('christmasMusic').play()
		$("#canvas").addClass('christmas')

        putSnowflakes($("#main"), snowflakeCount)
	} else {
		christmasMarker = false
		document.getElementById('christmasMusic').pause()
		$.each(flightMarkers, function(index, markersByLinkId) {
			$.each(markersByLinkId.markers, function(index2, marker) {
				marker.icon = {
			        url: "assets/images/markers/dot.png",
			        origin: new google.maps.Point(0, 0),
			        anchor: new google.maps.Point(6, 6),
			    };
			})
		})
		$("#canvas").removeClass('christmas')
		$("#main").children(".snowflake").remove()
	}
}

//.snowflake:nth-of-type(1){left:10%;-webkit-animation-delay:1s,1s;animation-delay:1s,1s}
//.snowflake:nth-of-type(2){left:20%;-webkit-animation-delay:6s,.5s;animation-delay:6s,.5s}


function putSnowflakes(container ,snowflakeCount) {
    removeSnowflakes(container)
    for (i = 0 ; i < snowflakeCount; i++) {
        var snowflake = $("<div class='snowflake'>❅</div>").appendTo(container)

        //snowflake.css("{left:" + i * 10 + "'%; animation-delay:" + i + "s, " + i + "s}")

        //snowflake.css("animation-name", "snowflakes-fall,snowflakes-shake-0")

        var depth = Math.floor(Math.random() * 5) + 1 //1-5
        snowflake.css("animation-name", "snowflakes-fall,snowflakes-shake-" + depth + ", snowflakes-shimmer")
        snowflake.css("animation-duration", depth * 10 + "s," + depth * 3 + "s," + depth * 10 + "s");
        snowflake.css("left", Math.floor(Math.random() * 101) + "%") //; animation-delay:" + i + "s, " + i + "s}")
        snowflake.css("animation-delay", Math.random() * 10 + "s," + Math.random() * -10 + "s")
        snowflake.css("font-size", (7 - depth) / 2 + "em")
        snowflake.css("text-shadow", "#fff 0 0 " +  ((5 - depth) * 0.5 + 2) +  "px")

    }
}

function removeSnowflakes(container) {
    container.children(".snowflake").remove()
}



var flightMarkerImageWeight = {
	"assets/images/markers/dot.png" : 2000,
	"assets/images/markers/christmas/snowflake.png" : 200,
	"assets/images/markers/christmas/star.png" : 50,
	"assets/images/markers/christmas/holly.png" : 20,
	"assets/images/markers/christmas/bauble.png" : 20,
	"assets/images/markers/christmas/candy-cane.png" : 10,
	"assets/images/markers/christmas/gingerbread-man.png" : 10,
	"assets/images/markers/christmas/santa-hat.png" : 2,
}

var flightMarkerWeightTotal = 0

$.each(flightMarkerImageWeight, function(image, weight) {
	flightMarkerWeightTotal += weight
})

function randomFlightMarker() {
	var random = Math.random()
	var acc = 0
	var pickedImage = ""
	$.each(flightMarkerImageWeight, function(image, weight) {
		acc += weight / flightMarkerWeightTotal
		if (acc >= random) {
			pickedImage = image
			return false;
		}
	})
	return pickedImage
}






















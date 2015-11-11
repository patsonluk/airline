function updateAirlineInfo(airlineId) {
	$("#ownedAircraftInfo").empty()
	$.ajax({
		type: 'GET',
		url: "http://localhost:9001/airplanes?airlineId=" + airlineId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airplanes) {
	    	$.each(airplanes, function( key, airplane ) {
	    		$("#ownedAircraftInfo").append($("<div></div>").text(airplane.name))
	  		});
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}	
	
	
	
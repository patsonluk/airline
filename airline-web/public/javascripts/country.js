var noFlags = ["BL", "CW", "IM", "GG", "JE", "BQ", "MF", "SS", "SX", "XK"]

function showCountryView() {
	setActiveDiv($("#countryCanvas"))
	highlightTab($('#countryCanvasTab'))
	
	$.ajax({
		type: 'GET',
		url: "countries",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(countries) {
	    	$("#countryList").empty()
	    	$.each(countries, function(index, country) {
	    		//var itemDiv = $("<div onclick='loadCountryDetails('" + country.countryCode +"')'><span class='label'>" + country.name + "</span></div>")
	   
	    		var itemDiv = $("<a href='javascript:void(0)' onclick='loadCountryDetails(\"" + country.countryCode + "\")'></a>").text(country.name)
				$("#countryList").append(itemDiv)
	    		if ($.inArray(country.countryCode, noFlags) == -1) {
	    			$("#countryList").append("<img src='assets/images/flags/" + country.countryCode + ".png'/>")
	    		}
				$("#countryList").append("<br/>")
	    		
	    	})
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function loadCountryDetails(countryId) {
	$("#countryDetails .value").empty()
	$.ajax({
		type: 'GET',
		url: "countries/" + countryId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(country) {
	    	$("#countryDetailsName").text(country.name)
	    	$("#countryDetailsIncomeLevel").text(country.incomeLevel)
	    	$("#countryDetailsOpenness").text(country.openness)
	    	$("#countryDetailsLargeAirportCount").text(country.largeAirportCount)
	    	$("#countryDetailsMediumAirportCount").text(country.mediumAirportCount)
	    	$("#countryDetailsSmallAirportCount").text(country.smallAirportCount)
	    	
	    	if (country.headquarters.length > 0) {
		    	$.each(country.headquarters, function(index, headquarter) {
		    		$("#countryDetailsAirlineHeadquarters").append("<div>" + headquarter.airlineName + "(" + getAirportText(headquarter.city, headquarter.airportName) + ")</div>")
		    	})
	    	} else {
	    		$("#countryDetailsAirlineHeadquarters").text("-")
	    	}
	    	
	    	if (country.bases.length > 0) {
		    	$.each(country.bases, function(index, base) {
		    		$("#countryDetailsAirlineBases").append("<div>" + base.airlineName + "(" + getAirportText(base.city, base.airportName) + ")</div>")
		    	})
	    	} else {
	    		$("#countryDetailsAirlineBases").text("-")
	    	}
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


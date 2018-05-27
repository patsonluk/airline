var noFlags = ["BL", "CW", "IM", "GG", "JE", "BQ", "MF", "SS", "SX", "XK"]
var loadedCountries = []

function showCountryView() {
	setActiveDiv($("#countryCanvas"))
	highlightTab($('#countryCanvasTab'))
	

	$.ajax({
		type: 'GET',
		url: "countries",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(countries) {
	    	loadedCountries = countries;
	    	$("#countryList").empty()
	    	
	    	var selectedSortHeader = $('#countryTable .table-header .cell.selected') 
		    updateCountryTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateCountryTable(sortProperty, sortOrder) {
	var selectedCountry = $("#countryCanvas #countryTable div.table-row.selected").data('country-code')
	var countryTable = $("#countryCanvas #countryTable")
	
	countryTable.children("div.table-row").remove()
	
	//sort the list
	loadedCountries.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	$.each(loadedCountries, function(index, country) {
		var row = $("<div class='table-row clickable' data-country-code='" + country.countryCode + "' onclick=\"loadCountryDetails('" + country.countryCode + "')\"></div>")
		if ($.inArray(country.countryCode, noFlags) != -1) {
			row.append("<div class='cell'></div>")
		} else {
			row.append("<div class='cell'><img src='assets/images/flags/" + country.countryCode + ".png'/></div>")
		}
		
		row.append("<div class='cell'>" + country.name + "</div>")
		row.append("<div class='cell' align='right'>" + country.airportPopulation + "</div>")
		row.append("<div class='cell' align='right'>" + country.incomeLevel + "</div>")
		row.append("<div class='cell' align='right'>" + country.openness + "</div>")
		
		if (selectedCountry == country.countryCode) {
			row.addClass("selected")
		}
		
		countryTable.append(row)
	});
}

function toggleCountryTableSortOrder(sortHeader) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}
	
	sortHeader.siblings().removeClass("selected")
	sortHeader.addClass("selected")
	
	updateCountryTable(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
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
	    	$("#countryDetails").fadeIn(200);
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


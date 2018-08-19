var loadedCountries = []
var loadedCountriesByCode = {}
var zoneById = { "AS" : "Asia", "NA" : "North America", "SA" : "South America", "AF" : "Africa", "OC" : "Oceania", "EU" : "Europe" }

function showCountryView() {
	setActiveDiv($("#countryCanvas"))
	highlightTab($('#countryCanvasTab'))
	
	$("#countryList").empty()
   	var selectedSortHeader = $('#countryTableHeader .table-header .cell.selected') 
    updateCountryTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
}

function loadAllCountries() {
	var getUrl = "countries"
	if (activeAirline && activeAirline.headquarterAirport) {
		getUrl += "?homeCountryCode=" + activeAirline.headquarterAirport.countryCode
	}
	
	loadedCountries = []
	loadedCountriesByCode = {}
	$.ajax({
		type: 'GET',
		url: getUrl,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    async: false,
	    success: function(countries) {
	    	$.each(countries, function(index, country) {
	    		loadedCountriesByCode[country.countryCode] = country
	    		loadedCountries.push(country)
	    	});
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
		var countryFlagUrl = getCountryFlagUrl(country.countryCode)
		if (countryFlagUrl) {
			row.append("<div class='cell'><img src='" + countryFlagUrl + "'/></div>")
		} else {
			row.append("<div class='cell'></div>")
		}
		row.append("<div class='cell'>" + country.name + "</div>")
		row.append("<div class='cell' align='right'>" + country.airportPopulation + "</div>")
		row.append("<div class='cell' align='right'>" + country.incomeLevel + "</div>")
		row.append("<div class='cell' align='right'>" + country.openness + "</div>")
		var mutualRelationship = 0
		if (country.mutualRelationship) {
			mutualRelationship = country.mutualRelationship 
		}
		row.append("<div class='cell' align='right'>" + getRelationshipDescription(mutualRelationship) + "</div>")
		
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
	$("#countryDetailsSharesChart").hide()
	$.ajax({
		type: 'GET',
		url: "countries/" + countryId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(country) {
	    	$("#countryDetailsName").text(country.name)
	    	$("#countryDetailsIncomeLevel").text(country.incomeLevel)
	    	$("#countryDetailsOpenness").html(getOpennessSpan(country.openness))
	    	
	    	var loadedCountry = loadedCountries.filter(function(obj) {
	    		  return obj.countryCode == countryId;
	    	})[0];
	    	
	    	var mutualRelationship
	    	if (typeof loadedCountry.mutualRelationship != 'undefined') {
	    		mutualRelationship = getRelationshipDescription(loadedCountry.mutualRelationship)
	    	} else {
	    		mutualRelationship = '-'
	    	}
	    	
	    	$("#countryDetailsMutualRelationship").text(mutualRelationship)
	    	$("#countryDetailsLargeAirportCount").text(country.largeAirportCount)
	    	$("#countryDetailsMediumAirportCount").text(country.mediumAirportCount)
	    	$("#countryDetailsSmallAirportCount").text(country.smallAirportCount)
	    	
//	    	if (country.headquarters.length > 0) {
//		    	$.each(country.headquarters, function(index, headquarter) {
//		    		$("#countryDetailsAirlineHeadquarters").append("<div>" + headquarter.airlineName + "(" + getAirportText(headquarter.city, headquarter.airportName) + ")</div>")
//		    	})
	    		$("#countryDetailsAirlineHeadquarters").text(country.headquarters.length)
//	    	} else {
//	    		$("#countryDetailsAirlineHeadquarters").text("-")
//	    	}
	    	
//	    	if (country.bases.length > 0) {
//		    	$.each(country.bases, function(index, base) {
//		    		$("#countryDetailsAirlineBases").append("<div>" + base.airlineName + "(" + getAirportText(base.city, base.airportName) + ")</div>")
//		    	})
	    		$("#countryDetailsAirlineBases").text(country.bases.length)
//	    	} else {
//	    		$("#countryDetailsAirlineBases").text("-")
//	    	}
	    	if (country.champions) {
	    		var championDivs = ""
	    			
    			$.each(country.champions, function(index, champion) {
    				championDivs += "<div>" + getRankingImg(champion.ranking) + getAirlineLogoImg(champion.airline.id) + "&nbsp;<span style='font-weight: bold;'>" + champion.airline.name + "</span> (" + champion.passengerCount + " passengers, " + champion.reputationBoost + " reputation bonus)</div>"
    			})
	    		
	    		$("#countryDetailsChampion").html(championDivs)
	    	} else {
	    		$("#countryDetailsChampion").text("-")
	    	}
	    	assignAirlineColors(country.marketShares, "airlineId")
	    	plotPie(country.marketShares, activeAirline ? activeAirline.name : null , $("#countryDetailsSharesChart"), "airlineName", "passengerCount")
	    	$("#countryDetailsSharesChart").show()
	    	
	    	$("#countryDetails").fadeIn(200);
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function getRelationshipDescription(value) {
	var description;
	if (value >= 5) {
		description = "Home Country"
    } else if (value == 4) {
		description = "Alliance"
	} else if (value == 3) {
		description = "Close"
	} else if (value == 2) {
		description = "Friendly"
	} else if (value == 1) { 
		description = "Warm"
	} else if (value == 0) {
		description = "Neutral"
	} else if (value == -1) {
		description = "Cold"
	} else if (value == -2) {
		description = "Hostile"
	} else if (value == -3) {
		description = "In Conflict"
	} else if (value <= -4) {
		description = "War"
	}
	
	return description + ' (' + value + ')'
}
var loadedCountries = []
var loadedCountriesByCode = {}
var zoneById = { "AS" : "Asia", "NA" : "North America", "SA" : "South America", "AF" : "Africa", "OC" : "Oceania", "EU" : "Europe" }

function showCountryView(selectedCountry) {
	highlightTab($('.countryCanvasTab'))
	
	$("#countryList").empty()
   	var selectedSortHeader = $('#countryTableHeader .table-header .cell.selected') 
    updateCountryTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'), selectedCountry)

    var callback
    if (selectedCountry) {
        callback = function() {
            $("#countryCanvas #countryTable div.table-row.selected")[0].scrollIntoView()
        }
    }
    setActiveDiv($("#countryCanvas"), callback)


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

function updateCountryTable(sortProperty, sortOrder, selectedCountry) {
    if (!selectedCountry) {
        selectedCountry = $("#countryCanvas #countryTable div.table-row.selected").data('country-code')
    }
	var countryTable = $("#countryCanvas #countryTable")
	
	countryTable.children("div.table-row").remove()
	
	//sort the list
	loadedCountries.sort(sortByProperty(sortProperty, sortOrder == "ascending"))

	var selectedRow
	$.each(loadedCountries, function(index, country) {
		var row = $("<div class='table-row clickable' data-country-code='" + country.countryCode + "' onclick=\"selectCountry('" + country.countryCode + "', false)\"></div>")
		var countryFlagUrl = getCountryFlagUrl(country.countryCode)

		if (countryFlagUrl) {
		    row.append("<div class='cell'><img src='" + countryFlagUrl + "'/>&nbsp;" + country.name + "</div>")
		} else {
		    row.append("<div class='cell'>" + country.name + "</div>")
        }
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
		    selectedRow = row
		}
		
		countryTable.append(row)
	});

	if (selectedRow) {
        loadCountryDetails(selectedCountry)
	}
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

function selectCountry(countryCode) {
    $("#countryCanvas #countryTable div.selected").removeClass("selected")
	//highlight the selected country
	$("#countryCanvas #countryTable div[data-country-code='" + countryCode +"']").addClass("selected")
	loadCountryDetails(countryCode)
}

function loadCountryDetails(countryCode) {
	$("#countryDetailsSharesChart").hide()
	$.ajax({
		type: 'GET',
		url: "countries/" + countryCode,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(country) {
	    	$("#countryDetailsName").text(country.name)
	    	$("#countryDetailsIncomeLevel").text(country.incomeLevel)
	    	$("#countryDetailsOpenness").html(getOpennessSpan(country.openness))

	    	var loadedCountry = loadedCountries.filter(function(obj) {
	    		  return obj.countryCode == countryCode;
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
	    	$("#countryDetailsSmallAirportCount").text(country.smallAirportCount)
    		$("#countryDetailsAirlineHeadquarters").text(country.headquarters.length)
    		$("#countryDetailsAirlineBases").text(country.bases.length)

    		$("#countryCanvas .nationalAirlines").empty()
            if (country.nationalAirlines && country.nationalAirlines.length > 0) {
                $.each(country.nationalAirlines, function(index, nationalAirline) {
                    var championRow = $("<div class='table-row'><div class='cell'><img src='assets/images/icons/star.png' style='vertical-align:middle;'>" + getAirlineLogoImg(nationalAirline.airlineId) + "<span style='font-weight: bold;'>" + nationalAirline.airlineName + "</span> (" + nationalAirline.passengerCount + " passengers, " + nationalAirline.loyaltyBonus + " loyalty bonus)</div></div>")
                    $("#countryCanvas .nationalAirlines").append(championRow)
                })
            } else {
                $("#countryCanvas .nationalAirlines").append($("<div class='table-row'><div class='cell'>-</div></div>"))
            }

            $("#countryCanvas .partneredAirlines").empty()
            if (country.partneredAirlines && country.partneredAirlines.length > 0) {
                $.each(country.partneredAirlines, function(index, partneredAirline) {
                    var championRow = $("<div class='table-row'><div class='cell'><img src='assets/images/icons/hand-shake.png' style='vertical-align:middle;'>" + getAirlineLogoImg(partneredAirline.airlineId) + "<span style='font-weight: bold;'>" + partneredAirline.airlineName + "</span> (" + partneredAirline.passengerCount + " passengers, " + partneredAirline.loyaltyBonus + " loyalty bonus)</div></div>")
                    $("#countryCanvas .partneredAirlines").append(championRow)
                })
            } else {
                $("#countryCanvas .partneredAirlines").append($("<div class='table-row'><div class='cell'>-</div></div>"))
            }

            $("#countryCanvas .countryDetailsChampion").empty()
	    	if (country.champions) {
	    		$.each(country.champions, function(index, champion) {
	    		    var championRow = $("<div class='table-row'><div class='cell'>" + getRankingImg(champion.ranking) + getAirlineLogoImg(champion.airlineId) + "<span style='font-weight: bold;'>" + champion.airlineName + "</span> (" + champion.passengerCount + " passengers, " + champion.reputationBoost + " reputation bonus)</div></div>")
    				$("#countryCanvas .countryDetailsChampion").append(championRow)
    			})
	    	} else {
	    		$("#countryCanvas .countryDetailsChampion").append($("<div class='table-row'><div class='cell'>-</div></div>"))
	    	}

	    	assignAirlineColors(country.marketShares, "airlineId")
	    	plotPie(country.marketShares, activeAirline ? activeAirline.name : null , $("#countryDetailsSharesChart"), "airlineName", "passengerCount")
	    	$("#countryDetailsSharesChart").show()

	    	$("#countryCanvas .sidePanel").fadeIn(200);
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

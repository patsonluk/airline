var loadedRivals = []
var loadedRivalLinks = []

function showRivalsCanvas() {
	setActiveDiv($("#rivalsCanvas"))
	highlightTab($('#rivalsCanvasTab'))
	$('#rivalDetails').hide()
	loadAllRivals()
}

function loadAllRivals() {
	var getUrl = "airlines"
	
	loadedRivals = []
	
	$.ajax({
		type: 'GET',
		url: getUrl,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airlines) {
	    	loadedRivals = airlines
	    	var selectedSortHeader = $('#rivalsTableSortHeader .table-header .cell.selected')
	    	updateRivalsTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateRivalsTable(sortProperty, sortOrder) {
	var selectedAirline = $("#rivalsCanvas #rivalsTable div.table-row.selected").data('airline-id')
	var rivalsTable = $("#rivalsCanvas #rivalsTable")
	
	rivalsTable.children("div.table-row").remove()
	
	//sort the list
	loadedRivals.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	$.each(loadedRivals, function(index, airline) {
		var row = $("<div class='table-row clickable' data-airline-id='" + airline.id + "' onclick=\"loadRivalDetails($(this), '" + airline.id + "')\"></div>")
//		var countryFlagImg = ""
//		if (airline.countryCode) {
//			countryFlagImg = getCountryFlagImg(airline.countryCode)
//		}
		
		row.append("<div class='cell'>" + getAirlineLogoImg(airline.id) + airline.name + "</div>")
		if (airline.headquartersAirportName) {
			row.append("<div class='cell'>" + getAirportText(airline.headquartersCity, airline.headquartersAirportName) + "</div>")
		} else {
			row.append("<div class='cell'>-</div>")
		}
		row.append("<div class='cell' align='right'>" + airline.reputation + "</div>")
		row.append("<div class='cell' align='right'>" + airline.baseCount + "</div>")
		
		if (selectedAirline == airline.countryCode) {
			row.addClass("selected")
		}
		
		rivalsTable.append(row)
	});
}

function toggleRivalsTableSortOrder(sortHeader) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}
	
	sortHeader.siblings().removeClass("selected")
	sortHeader.addClass("selected")
	
	updateRivalsTable(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
}

function loadRivalDetails(row, airlineId) {
	//update table
	row.siblings().removeClass("selected")
	row.addClass("selected")
	
	updateRivalChampionedCountriesDetails(airlineId)
	loadRivalLinks(airlineId)
	
	updateRivalBaseList(airlineId)
	
	$('#rivalDetails').fadeIn(200)
}

function loadRivalLinks(airlineId) {
	var airlineLinksTable = $("#rivalsCanvas #rivalLinksTable")
	airlineLinksTable.children("div.table-row").remove()
	
	var getUrl = "airlines/" + airlineId + "/links"
	loadedRivalLinks = []
	$.ajax({
		type: 'GET',
		url: getUrl,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(links) {
	    	loadedRivalLinks = links
	    	updateRivalLinksTable()
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateRivalLinksTable(sortProperty, sortOrder) {
	var rivalLinksTable = $("#rivalsCanvas #rivalLinksTable")
	
	rivalLinksTable.children("div.table-row").remove()
	
	//sort the list
	loadedRivalLinks.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	$.each(loadedRivalLinks, function(index, link) {
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell'>" + getCountryFlagImg(link.fromCountryCode) + getAirportText(link.fromAirportCity, link.fromAirportCode) + "</div>")
		row.append("<div class='cell'>" + getCountryFlagImg(link.toCountryCode) + getAirportText(link.toAirportCity, link.toAirportCode) + "</div>")
		row.append("<div class='cell' align='right'>" + link.distance + "km</div>")
		link.totalCapacity = link.capacity.economy + link.capacity.business + link.capacity.first
		row.append("<div class='cell' align='right'>" + link.totalCapacity + "</div>")
		
		rivalLinksTable.append(row)
	});
}

function toggleRivalLinksTableSortOrder(sortHeader) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}
	
	sortHeader.siblings().removeClass("selected")
	sortHeader.addClass("selected")
	
	updateRivalLinksTable(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
}

function updateRivalChampionedCountriesDetails(airlineId) {
	$('#rivalChampionedCountriesList').children('div.table-row').remove()
	
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/championed-countries",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(championedCountries) {
	    	$(championedCountries).each(function(index, championDetails) {
	    		var country = championDetails.country
	    		var rankingIcon
	    		var rankingTitle
	    		if (championDetails.ranking == 1) {
	    			rankingIcon = "assets/images/icons/crown.png"
	    			rankingTitle = "1st place"
	    		} else if (championDetails.ranking == 2) {
	    			rankingIcon = "assets/images/icons/crown-silver.png"
		    		rankingTitle = "2nd place"
	    		} else if (championDetails.ranking == 3) {
	    			rankingIcon = "assets/images/icons/crown-bronze.png"
			    	rankingTitle = "3rd place"
	    		}
	    		var row = $("<div class='table-row clickable' onclick=\"loadCountryDetails('" + country.countryCode + "'); showCountryView();\"></div>")
	    		row.append("<div class='cell'><img src='" + rankingIcon + "' title='" + rankingTitle + "'/></div>")
	    		row.append("<div class='cell'>" + getCountryFlagImg(country.countryCode) + country.name + "</div>")
	    		row.append("<div class='cell'>" + championDetails.reputationBoost + "</div>") 
	    		$('#rivalChampionedCountriesList').append(row)
	    	})
	    	
	    	if ($(championedCountries).length == 0) {
	    		var row = $("<div class='table-row'></div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		$('#rivalChampionedCountriesList').append(row)
	    	}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateRivalBaseList(airlineId) {
	$('#rivalHeadquarters').children('.table-row').remove()
	$('#rivalBases').children('.table-row').remove()
	
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/bases",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(bases) {
	    	var hasHeadquarters = false
	    	var hasBases = false
	    	$(bases).each(function(index, base) {
	    		var row = $("<div class='table-row'></div>")
	    		row.append("<div class='cell'>" + getCountryFlagImg(base.countryCode) + getAirportText(base.city, base.airportName) + "</div>")
	    		
	    		if (base.headquarter) {
	    			$('#rivalHeadquarters').append(row)
	    			hasHeadquarters = true
	    		} else {
	    			$('#rivalBases').append(row)
	    			hasBases = true
	    		}
	    	})
	    	var emtpyRow = $("<div class='table-row'></div>")
			emtpyRow.append("<div class='cell'>-</div>")
			
			if (!hasHeadquarters) {
    			$('#rivalHeadquarters').append(emtpyRow)
    		}
    		if (!hasBases) {
    			$('#rivalBases').append(emtpyRow)
    		}
	    	
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	
	

	

}

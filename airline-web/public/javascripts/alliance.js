var loadedAlliances = []
var loadedAlliancesById = {}

function showAllianceCanvas() {
	setActiveDiv($("#allianceCanvas"))
	highlightTab($('#allianceCanvasTab'))
	if (!activeAirline) {
		$('#currentAirlineMemberDetails').hide()
	} else {
		loadCurrentAirlineMemberDetails()
		$('#currentAirlineMemberDetails').show()
	}
	$('#allianceDetails').hide()
	loadAllAlliances()
}

function loadCurrentAirlineMemberDetails() {
	$('#currentAirlineMemberDetails .allianceName').show()
	$('#toggleFormAllianceButton').hide()
	$('#formAllianceSpan').hide()
	
	var getUrl = "airlines/" + activeAirline.id + "/alliance-details"
	$('#currentAirlineAllianceHistory').empty()
	$.ajax({
		type: 'GET',
		url: getUrl,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(allianceDetails) {
	    	if (allianceDetails.alliance) {
	    		$('#currentAirlineMemberDetails .allianceName').text(allianceDetails.alliance.name)
	    		$('#currentAirlineMemberDetails .allianceRole').text(allianceDetails.allianceRole)
	    		$('#currentAirlineMemberDetails .allianceStatus').text(allianceDetails.alliance.status)
	    		$('#toggleFormAllianceButton').hide()
	    	} else {
	    		$('#currentAirlineMemberDetails .allianceName').text('-')
	    		$('#currentAirlineMemberDetails .allianceRole').text('-')
	    		$('#currentAirlineMemberDetails .allianceStatus').text('-')
	    		$('#toggleFormAllianceButton').show()
	    	}
	    	
	    
	    	if (allianceDetails.history) {
	    		$.each(allianceDetails.history, function(index, entry) {
	    			var row = $("<div class='table-row'><div class='cell value' style='width: 30%;'>Week " + entry.cycle + "</div><div class='cell value' style='width: 70%;'>" + entry.description + "</div></div>")
	    			$('#currentAirlineAllianceHistory').append(row)
	    		})
	    	} else {
	    		var row = $("<div class='table-row'><div class='cell value' style='width: 30%;'>-</div><div class='cell value' style='width: 70%;'>-</div></div>")
	    		$('#currentAirlineAllianceHistory').append(row)
	    	}
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function loadAllAlliances() {
	var getUrl = "alliances"
	
	loadedAlliances = []
	loadedAlliancesById = {}
	$.ajax({
		type: 'GET',
		url: getUrl,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(alliances) {
	    	loadedAlliances = alliances
	    	$.each(alliances, function(index, alliance) {
	    		loadedAlliancesById[alliance.id] = alliance
	    	})
	    	
	    	var selectedSortHeader = $('#allianceTableSortHeader .table-header .cell.selected')
	    	updateAllianceTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateAllianceTable(sortProperty, sortOrder) {
	var selectedAlliance = $("#allianceCanvas #allianceTable div.table-row.selected").data('alliance-id')
	var allianceTable = $("#allianceCanvas #allianceTable")
	
	allianceTable.children("div.table-row").remove()
	
	//sort the list
	loadedAlliances.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	$.each(loadedAlliances, function(index, alliance) {
		var row = $("<div class='table-row clickable' data-alliance-id='" + alliance.id + "' onclick=\"loadAllianceDetails($(this), '" + alliance.id + "')\"></div>")
//		var countryFlagImg = ""
//		if (airline.countryCode) {
//			countryFlagImg = getCountryFlagImg(airline.countryCode)
//		}
		
		row.append("<div class='cell'>" + alliance.name + "</div>")
		row.append("<div class='cell'>" + getAirlineLogoImg(alliance.leader.id) + alliance.leader.name + "</div>")
		row.append("<div class='cell' align='right'>" + alliance.members.length + "</div>")
		
		if (typeof alliance.championedPoints != 'undefined') {
			row.append("<div class='cell' align='right'>" + alliance.championedPoints + "</div>")
		} else {
			row.append("<div class='cell' align='right'>-</div>")
		}
		
		if (selectedAlliance == alliance.id) {
			row.addClass("selected")
		}
		
		allianceTable.append(row)
	});
}

function toggleAllianceTableSortOrder(sortHeader) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}
	
	sortHeader.siblings().removeClass("selected")
	sortHeader.addClass("selected")
	
	updateAllianceTable(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
}

function loadAllianceDetails(row, allianceId) {
	//update table
	row.siblings().removeClass("selected")
	row.addClass("selected")
	
	
	updateAllianceBasicsDetails(allianceId)
	updateAllianceChampionedCountriesDetails(allianceId)
	updateAllianceHistory(allianceId)
	$('#allianceDetails').fadeIn(200)
}


function updateAllianceBasicsDetails(allianceId) {
	var alliance = loadedAlliancesById[allianceId]
	$("#allianceCanvas .allianceName").text(alliance.name)
	$("#allianceCanvas .allianceStatus").text(alliance.status)
	$("#allianceMemberList").children("div.table-row").remove()
	
	$.each(alliance.members, function(index, member) {
		if (member.allianceRole != "Applicant") {
			var row = $("<div class='table-row'></div>")
			row.append("<div class='cell'>" + getAirlineLogoImg(member.airlineId) + member.airlineName + "</div>")
			row.append("<div class='cell'>" + member.allianceRole + "</div>")
			if (member.airlineId == activeAirline.id) {
				row.append("<div class='cell'><img src='assets/images/icons/cross.png' class='button' title='Leave Alliance' onclick='promptConfirm(\"Leave Alliance?\", removeAllianceMember, " + activeAirline.id + ")'></div>")
			} else if (alliance.leader.id == activeAirline.id) {
				row.append("<div class='cell'><img src='assets/images/icons/cross.png' class='button' title='Remove Member' onclick='promptConfirm(\"Remove " + member.airlineName + " from Alliance?\", removeAllianceMember, " + member.airlineId + ")'></div>")
			}
			$("#allianceMemberList").append(row)
		}
	});
}

function updateAllianceChampionedCountriesDetails(allianceId) {
//	$('#rivalChampionedCountriesList').children('div.table-row').remove()
//	
//	$.ajax({
//		type: 'GET',
//		url: "airlines/" + airlineId + "/championed-countries",
//	    contentType: 'application/json; charset=utf-8',
//	    dataType: 'json',
//	    success: function(championedCountries) {
//	    	$(championedCountries).each(function(index, championDetails) {
//	    		var country = championDetails.country
//	    		var rankingIcon
//	    		var rankingTitle
//	    		if (championDetails.ranking == 1) {
//	    			rankingIcon = "assets/images/icons/crown.png"
//	    			rankingTitle = "1st place"
//	    		} else if (championDetails.ranking == 2) {
//	    			rankingIcon = "assets/images/icons/crown-silver.png"
//		    		rankingTitle = "2nd place"
//	    		} else if (championDetails.ranking == 3) {
//	    			rankingIcon = "assets/images/icons/crown-bronze.png"
//			    	rankingTitle = "3rd place"
//	    		}
//	    		var row = $("<div class='table-row clickable' onclick=\"loadCountryDetails('" + country.countryCode + "'); showCountryView();\"></div>")
//	    		row.append("<div class='cell'><img src='" + rankingIcon + "' title='" + rankingTitle + "'/></div>")
//	    		row.append("<div class='cell'>" + getCountryFlagImg(country.countryCode) + country.name + "</div>")
//	    		row.append("<div class='cell'>" + championDetails.reputationBoost + "</div>") 
//	    		$('#rivalChampionedCountriesList').append(row)
//	    	})
//	    	
//	    	if ($(championedCountries).length == 0) {
//	    		var row = $("<div class='table-row'></div>")
//	    		row.append("<div class='cell'>-</div>")
//	    		row.append("<div class='cell'>-</div>")
//	    		row.append("<div class='cell'>-</div>")
//	    		$('#rivalChampionedCountriesList').append(row)
//	    	}
//	    },
//        error: function(jqXHR, textStatus, errorThrown) {
//	            console.log(JSON.stringify(jqXHR));
//	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
//	    }
//	});
}

function updateAllianceHistory(allianceId) {
}


function toggleFormAlliance() {
	$('#currentAirlineMemberDetails .allianceName').hide()
	$('#toggleFormAllianceButton').hide()
	$('#formAllianceWarning').hide()
	$('#formAllianceSpan').show()
}

function formAlliance(allianceName) {
	var url = "airlines/" + activeAirline.id + "/form-alliance"
	$.ajax({
		type: 'POST',
		url: url,
		data: { 'allianceName' : allianceName } ,
	    dataType: 'json',
	    success: function(newAlliance) {
	    	if (!newAlliance.rejection) {
	    		showAllianceCanvas()
	    	} else {
	    		$('#formAllianceWarning').text(newAlliance.rejection)
	    		$('#formAllianceWarning').show()
	    	}
	    	
	    },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function removeAllianceMember(removeAirlineId) {
	var url = "airlines/" + activeAirline.id + "/remove-alliance-member/" + removeAirlineId
	$.ajax({
		type: 'DELETE',
		url: url,
		contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}
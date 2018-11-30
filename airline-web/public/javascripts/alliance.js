var loadedAlliances = []
var loadedAlliancesById = {}
var selectedAlliance

$( document ).ready(function() {
	loadAllAlliances()
})

function showAllianceCanvas() {
	setActiveDiv($("#allianceCanvas"))
	highlightTab($('#allianceCanvasTab'))
	loadAllAlliances()
	if (!activeAirline) {
		$('#currentAirlineMemberDetails').hide()
	} else {
		loadCurrentAirlineMemberDetails()
		$('#currentAirlineMemberDetails').show()
	}
}

function loadCurrentAirlineAlliance(callback) {
	var getUrl = "airlines/" + activeAirline.id + "/alliance-details"
	$.ajax({
		type: 'GET',
		url: getUrl,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    async: false,
	    success: function(allianceDetails) {
	    	callback(allianceDetails)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function loadCurrentAirlineMemberDetails() {
	$('#currentAirlineMemberDetails .allianceName').show()
	$('#toggleFormAllianceButton').hide()
	$('#formAllianceSpan').hide()
	
	
	$('#currentAirlineAllianceHistory').empty()
	loadCurrentAirlineAlliance(function(allianceDetails) {
		if (allianceDetails.allianceId) {
    		var alliance = loadedAlliancesById[allianceDetails.allianceId]
    		$('#currentAirlineMemberDetails .allianceName').text(alliance.name)
    		$('#currentAirlineMemberDetails .allianceRole').text(allianceDetails.allianceRole)
    		if (alliance.ranking) {
	    		var rankingImg = getRankingImg(alliance.ranking)
	    		if (rankingImg) {
	    			$('#currentAirlineMemberDetails .allianceRanking').html(rankingImg)
	    		} else {
	    			$('#currentAirlineMemberDetails .allianceRanking').text(alliance.ranking)
	    		}
    		} else {
    			$('#currentAirlineMemberDetails .allianceRanking').text('-')
    		}
    		
    		if (alliance.status == 'Forming') {
				$("#currentAirlineMemberDetails .allianceStatus").text(alliance.status + " - need 3 approved members")
			} else {
				$("#currentAirlineMemberDetails .allianceStatus").text(alliance.status)
			}
    		
    		
    		$('#toggleFormAllianceButton').hide()
    	} else {
    		$('#currentAirlineMemberDetails .allianceName').text('-')
    		$('#currentAirlineMemberDetails .allianceRole').text('-')
    		$('#currentAirlineMemberDetails .allianceRanking').text('-')
    		$('#currentAirlineMemberDetails .allianceStatus').text('-')
    		if (activeAirline.headquarterAirport) {
    			$('#toggleFormAllianceButton').show()
    		} else {
    			$('#toggleFormAllianceButton').hide()
    		}
    	}
    	
    	$('#currentAirlineAllianceHistory').children("div.table-row").remove()
    	if (allianceDetails.history) {
    		$.each(allianceDetails.history, function(index, entry) {
    			var row = $("<div class='table-row'><div class='cell value' style='width: 30%;'>Week " + entry.cycle + "</div><div class='cell value' style='width: 70%;'>" + entry.description + "</div></div>")
    			$('#currentAirlineAllianceHistory').append(row)
    		})
    	} else {
    		var row = $("<div class='table-row'><div class='cell value' style='width: 30%;'>-</div><div class='cell value' style='width: 70%;'>-</div></div>")
    		$('#currentAirlineAllianceHistory').append(row)
    	}
	})
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
	    async: false,
	    success: function(alliances) {
	    	loadedAlliances = alliances
	    	$.each(alliances, function(index, alliance) {
	    		loadedAlliancesById[alliance.id] = alliance
	    		alliance.memberCount = alliance.members.length
	    		alliance.leaderAirlineName = alliance.leader.name
	    		if (alliance.championPoints) {
	    			alliance.championPointsValue = alliance.championPoints
	    		} else {
	    			alliance.championPointsValue = 0
	    		}
	    	})
	    	
	    	var selectedSortHeader = $('#allianceTableSortHeader .table-header .cell.selected')
	    	updateAllianceTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
	    	
	    	if (selectedAlliance) {
	    		if (loadedAlliancesById[selectedAlliance.id]) {
	    			loadAllianceDetails(selectedAlliance.id)
	    		} else { //alliance was just deleted
	    			selectedAlliance = undefined
	    			$('#allianceDetails').hide()
	    		}
			} else {
				$('#allianceDetails').hide()
			}
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateAllianceTable(sortProperty, sortOrder) {
	var allianceTable = $("#allianceCanvas #allianceTable")
	
	allianceTable.children("div.table-row").remove()
	
	//sort the list
	loadedAlliances.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	$.each(loadedAlliances, function(index, alliance) {
		var row = $("<div class='table-row clickable' data-alliance-id='" + alliance.id + "' onclick=\"selectAlliance($(this), '" + alliance.id + "')\"></div>")
//		var countryFlagImg = ""
//		if (airline.countryCode) {
//			countryFlagImg = getCountryFlagImg(airline.countryCode)
//		}
		
		row.append("<div class='cell'>" + alliance.name + "</div>")
		row.append("<div class='cell'>" + getAirlineLogoImg(alliance.leader.id) + alliance.leader.name + "</div>")
		row.append("<div class='cell' align='right'>" + alliance.members.length + "</div>")
		if (alliance.championPoints) {
			row.append("<div class='cell' align='right'>" + alliance.championPoints + "</div>")
		} else {
			row.append("<div class='cell' align='right'>-</div>")
		}
		
		if (selectedAlliance && selectedAlliance.id == alliance.id) {
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

function selectAlliance(row, allianceId) {
	//update table
	row.siblings().removeClass("selected")
	row.addClass("selected")
	loadAllianceDetails(allianceId)
}

function loadAllianceDetails(allianceId) {
	updateAllianceBasicsDetails(allianceId)
	updateAllianceBonus(allianceId)
	updateAllianceChampionContries(allianceId)
	updateAllianceHistory(allianceId)
	$('#allianceDetails').fadeIn(200)
}


function updateAllianceBasicsDetails(allianceId) {
	var alliance = loadedAlliancesById[allianceId]
	selectedAlliance = alliance
	$("#allianceDetails .allianceName").text(alliance.name)
	if (alliance.status == 'Forming') {
		$("#allianceDetails .allianceStatus").text(alliance.status + " - need 3 approved members")
	} else {
		$("#allianceDetails .allianceStatus").text(alliance.status)
	}
	
	if (alliance.ranking) {
		var rankingImg = getRankingImg(alliance.ranking)
		if (rankingImg) {
			$('#allianceDetails .allianceRanking').html(rankingImg)
		} else {
			$('#allianceDetails .allianceRanking').text(alliance.ranking)
		}
	} else {
		$('#allianceDetails .allianceRanking').text('-')
	}
	$("#allianceMemberList").children("div.table-row").remove()
	
	$.each(alliance.members, function(index, member) {
		var row = $("<div class='table-row clickable' style='height: 20px;' onclick='showRivalsCanvas(" + member.airlineId + ")'></div>")
		row.append("<div class='cell' style='vertical-align: middle;'>" + getAirlineLogoImg(member.airlineId) + member.airlineName + "</div>")
		row.append("<div class='cell' style='vertical-align: middle;'>" + member.allianceRole + "</div>")
		if (activeAirline) {
			if (member.airlineId == activeAirline.id) {
				row.append("<div class='cell' style='vertical-align: middle;'><img src='assets/images/icons/cross.png' class='button' title='Leave Alliance' onclick='promptConfirm(\"Leave Alliance?\", removeAllianceMember, " + activeAirline.id + ")'></div>")
			} else if (alliance.leader.id == activeAirline.id) {
				if (member.allianceRole == "Applicant") {
					var acceptQuestion = "Accept application from " + member.airlineName + "?"
					var rejectQuestion = "Reject application from " + member.airlineName + "?"
					row.append("<div class='cell' style='vertical-align: middle;'><img src='assets/images/icons/tick.png' class='button' title='Accept Member' onclick='promptConfirm(\"" + acceptQuestion + "\", acceptAllianceMember, " + member.airlineId + ")'><img src='assets/images/icons/cross.png' class='button' title='Remove Member' onclick='promptConfirm(\"" + rejectQuestion + "\", removeAllianceMember, " + member.airlineId + ")'></div>")
				} else {
					row.append("<div class='cell' style='vertical-align: middle;'><img src='assets/images/icons/cross.png' class='button' title='Remove Member' onclick='promptConfirm(\"Remove " + member.airlineName + " from alliance?\", removeAllianceMember, " + member.airlineId + ")'></div>")
				}
			} else {
				row.append("<div class='cell' style='vertical-align: middle;'></div>")
			}
		}
		$("#allianceMemberList").append(row)
	});
	
	
	if (activeAirline && selectedAlliance) {
		$.ajax({
			type: 'GET',
			url: "airlines/" + activeAirline.id + "/evaluate-alliance/" + selectedAlliance.id,
		    contentType: 'application/json; charset=utf-8',
		    dataType: 'json',
		    success: function(result) {
		    	if (!result.isMember && !result.rejection) {
		    		$('#applyForAllianceButton').show()
		    		$('#applyForAllianceRejectionSpan').hide();
		    	} else {
		    		$('#applyForAllianceButton').hide();
		    		if (result.rejection) {
		    			$('#applyForAllianceRejection').text(result.rejection)
			    		$('#applyForAllianceRejectionSpan').show()
		    		} else if (result.isMember){
		    			$('#applyForAllianceButton').hide();
			    		$('#applyForAllianceRejectionSpan').hide();
		    		}
		    	}
		    },
		    error: function(jqXHR, textStatus, errorThrown) {
		            console.log(JSON.stringify(jqXHR));
		            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
		    }
		});
	} else {
		$('#applyForAllianceButton').hide()
		$('#applyForAllianceRejectionSpan').hide();
	}
		
}

function updateAllianceBonus(allianceId) {
	var alliance = loadedAlliancesById[allianceId]
	
	if (alliance.status == "Forming") {
		$('#allianceCodeShareBonus').hide();
		$('#allianceMaxFrequencyBonus').hide();
		$('#allianceReputationBonus').hide();
		$('#allianceNoneBonus').show();
		
	} else {
		$('#allianceCodeShareBonus').show();
		$('#allianceNoneBonus').hide();
		if (alliance.maxFrequencyBonus) {
			$('#allianceMaxFrequencyBonusValue').text(alliance.maxFrequencyBonus)
			$('#allianceMaxFrequencyBonus').show();
		} else {
			$('#allianceMaxFrequencyBonus').hide();
		}
		
		if (alliance.reputationBonus) {
			$('#allianceReputationBonusValue').text(alliance.reputationBonus)
			$('#allianceReputationBonus').show();
		} else {
			$('#allianceReputationBonus').hide();
		}
	}
}
function updateAllianceChampionContries(allianceId) {
	$('#allianceChampionList').children('div.table-row').remove()
	
	$.ajax({
		type: 'GET',
		url: "alliances/" + allianceId + "/championed-countries",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(championedCountries) {
	    	$(championedCountries).each(function(index, championDetails) {
	    		var country = championDetails.country
	    		var row = $("<div class='table-row clickable' onclick=\"loadCountryDetails('" + country.countryCode + "'); showCountryView();\"></div>")
	    		row.append("<div class='cell' style='width : 10%'>" + getRankingImg(championDetails.ranking) + "</div>")
	    		row.append("<div class='cell' style='width : 30%'>" + getCountryFlagImg(country.countryCode) + country.name + "</div>")
	    		row.append("<div class='cell' style='width : 50%'>" + getAirlineLogoImg(championDetails.airlineId) + championDetails.airlineName + "</div>")
	    		row.append("<div class='cell' style='width : 10%'>" + championDetails.reputationBoost + "</div>") 
	    		$('#allianceChampionList').append(row)
	    	})
	    	
	    	if ($(championedCountries).length == 0) {
	    		var row = $("<div class='table-row'></div>")
	    		row.append("<div class='cell' style='width : 10%'>-</div>")
	    		row.append("<div class='cell' style='width : 30%'>-</div>")
	    		row.append("<div class='cell' style='width : 50%'>-</div>")
	    		row.append("<div class='cell' style='width : 10%'>-</div>")
	    		$('#allianceChampionList').append(row)
	    	}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateAllianceHistory(allianceId) {
	var alliance = loadedAlliancesById[allianceId]
	$('#allianceHistory').children("div.table-row").remove()
	$.each(alliance.history, function(index, entry) {
		var row = $("<div class='table-row'><div class='cell value' style='width: 30%;'>Week " + entry.cycle + "</div><div class='cell value' style='width: 70%;'>" + entry.description + "</div></div>")
		$('#allianceHistory').append(row)
	})
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
		type: 'GET',
		url: url,
		contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	showAllianceCanvas()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function acceptAllianceMember(acceptAirlineId) {
	var url = "airlines/" + activeAirline.id + "/accept-alliance-member/" + acceptAirlineId
	$.ajax({
		type: 'GET',
		url: url,
		contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	showAllianceCanvas()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function applyForAlliance() {
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/apply-for-alliance/" + selectedAlliance.id,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	showAllianceCanvas()
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function showAllianceMap() {
	clearAllPaths()
	deselectLink()
	
	$.ajax({
		type: 'GET',
		url: "alliances/" + selectedAlliance.id + "/links",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(links) {
				$.each(links, function(index, link) {
					drawAllianceLink(link)
				})
				showWorldMap();
				window.setTimeout(addExitButton , 1000); //delay otherwise it doesn't push to center
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function addExitButton() {
	if (map.controls[google.maps.ControlPosition.TOP_CENTER].getLength() == 0) {
		map.controls[google.maps.ControlPosition.TOP_CENTER].push(createMapButton(map, 'Exit Alliance Flight Map', 'hideAllianceMap()', 'hideAllianceMapButton')[0]);
	}
}

function drawAllianceLink(link) {
	var from = new google.maps.LatLng({lat: link.fromLatitude, lng: link.fromLongitude})
	var to = new google.maps.LatLng({lat: link.toLatitude, lng: link.toLongitude})
	//var pathKey = link.id
	
	var strokeColor = airlineColors[link.airlineId]
	if (!strokeColor) {
		strokeColor = "#DC83FC"
	}
		
		
	var linkPath = new google.maps.Polyline({
			 geodesic: true,
		     strokeColor: strokeColor,
		     strokeOpacity: 0.6,
		     strokeWeight: 2,
		     path: [from, to],
		     zIndex : 1100,
		});
		
	var fromAirport = getAirportText(link.fromAirportCity, link.fromAirportName)
	var toAirport = getAirportText(link.toAirportCity, link.toAirportName)
	
	
	shadowPath = new google.maps.Polyline({
		 geodesic: true,
	     strokeColor: strokeColor,
	     strokeOpacity: 0.0001,
	     strokeWeight: 25,
	     path: [from, to],
	     zIndex : 401,
	     fromAirport : fromAirport,
	     fromCountry : link.fromCountryCode, 
	     toAirport : toAirport,
	     toCountry : link.toCountryCode,
	     airlineName : link.airlineName,
	     airlineId : link.airlineId
	});
	
	linkPath.shadowPath = shadowPath
	
	var infowindow; 
	shadowPath.addListener('mouseover', function(event) {
		$("#allianceLinkPopupFrom").html(this.fromAirport + "&nbsp;" + getCountryFlagImg(this.fromCountry))
		$("#allianceLinkPopupTo").html(this.toAirport + "&nbsp;" + getCountryFlagImg(this.toCountry))
		$("#allianceLinkPopupAirline").html(getAirlineLogoImg(this.airlineId) + "&nbsp;" + this.airlineName)
		
		
		infowindow = new google.maps.InfoWindow({
             content: $("#allianceLinkPopup").html(),
             maxWidth : 600});
		
		infowindow.setPosition(event.latLng);
		infowindow.open(map);
	})		
	shadowPath.addListener('mouseout', function(event) {
		infowindow.close()
	})
	
	linkPath.setMap(map)
	linkPath.shadowPath.setMap(map)
	polylines.push(linkPath)
	polylines.push(linkPath.shadowPath)
}


function hideAllianceMap() {
	clearAllPaths()
	updateLinksInfo() //redraw all flight paths
		
	map.controls[google.maps.ControlPosition.TOP_CENTER].clear()
}

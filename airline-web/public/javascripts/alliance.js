var loadedAlliances = []
var loadedAlliancesById = {}
var selectedAlliance

$( document ).ready(function() {
	loadAllAlliances()
})

function showAllianceCanvas() {
	setActiveDiv($("#allianceCanvas"))
	highlightTab($('.allianceCanvasTab'))
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
	if (activeAirline) {
	    getUrl += "?airlineId=" + activeAirline.id
	}
	
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
	    		alliance.memberCount = 0
	    		$.each(alliance.members, function(index, member) {
	    		    if (member.allianceRole != 'Applicant') {
	    		        alliance.memberCount ++
	    		    }
	    		})
			if (alliance.leader) {
	    			alliance.leaderAirlineName = alliance.leader.name
			} else {
				alliance.leaderAirlineName = '-'
			}
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
		if (alliance.leader) {
			row.append("<div class='cell'>" + getAirlineLogoImg(alliance.leader.id) + alliance.leader.name + "</div>")
		} else {
			row.append("<div class='cell'>-</div>")
		}
		row.append("<div class='cell' align='right'>" + alliance.memberCount + "</div>")
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
	updateAllianceChampions(allianceId)
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
		var row = $("<div class='table-row clickable' style='height: 20px;' onclick='showAllianceMemberDetails($(this).data(\"member\"))'></div>")
		row.data("member", member)
		row.append("<div class='cell' style='vertical-align: middle;'>" + getAirlineLogoImg(member.airlineId) + member.airlineName + "</div>")
		if (member.allianceRole == "Applicant") {
			row.append("<div class='cell warning' style='vertical-align: middle;'>" + member.allianceRole + "</div>")
		} else {
			row.append("<div class='cell' style='vertical-align: middle;'>" + member.allianceRole + "</div>")
		}
		if (activeAirline) {
			if (member.airlineId == activeAirline.id) {
				row.append("<div class='cell' style='vertical-align: middle;'><img src='assets/images/icons/cross.png' class='button' title='Leave Alliance' onclick='promptConfirm(\"Leave Alliance?\", removeAllianceMember, " + activeAirline.id + ")'></div>")
			} else if (alliance.leader.id == activeAirline.id) {
				if (member.allianceRole == "Applicant") {
					var acceptQuestion = "Accept application from " + member.airlineName + "?"
					var rejectQuestion = "Reject application from " + member.airlineName + "?"
					if (member.rejection) {
					   row.append("<div class='cell' style='vertical-align: middle;'><img src='assets/images/icons/exclamation-circle.png' class='button disabled' title='Cannot accept member : " + member.rejection + "'><img src='assets/images/icons/cross.png' class='button' title='Remove Member' onclick='promptConfirm(\"" + rejectQuestion + "\", removeAllianceMember, " + member.airlineId + ")'></div>")
					} else {
					   row.append("<div class='cell' style='vertical-align: middle;'><img src='assets/images/icons/tick.png' class='button' title='Accept Member' onclick='promptConfirm(\"" + acceptQuestion + "\", acceptAllianceMember, " + member.airlineId + ")'><img src='assets/images/icons/cross.png' class='button' title='Remove Member' onclick='promptConfirm(\"" + rejectQuestion + "\", removeAllianceMember, " + member.airlineId + ")'></div>")
                    }
				} else {
					row.append("<div class='cell' style='vertical-align: middle;'><img src='assets/images/icons/user-promote.png' class='button' title='Promote Member to Leader' onclick='promptConfirm(\"Promote " + member.airlineName + " as alliance leader?<br/><br/><b>Your airline will lose the leadership and be demoted to member!!</b>\", promoteAllianceMember, " + member.airlineId + ")'><img src='assets/images/icons/cross.png' class='button' title='Remove Member' onclick='promptConfirm(\"Remove " + member.airlineName + " from alliance?\", removeAllianceMember, " + member.airlineId + ")'></div>")
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

		
		if (alliance.reputationBonus) {
			$('#allianceReputationBonusValue').text(alliance.reputationBonus)
			$('#allianceReputationBonus').show();
		} else {
			$('#allianceReputationBonus').hide();
		}
	}
}

function updateAllianceChampions(allianceId) {
    updateAllianceAirportChampions(allianceId)
    updateAllianceCountryChampions(allianceId)
}
function updateAllianceAirportChampions(allianceId) {
	$('#allianceChampionAirportList').children('div.table-row').remove()
	
	$.ajax({
		type: 'GET',
		url: "alliances/" + allianceId + "/championed-airports",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	var approvedMembersChampions = result.members
	    	var applicantChampions = result.applicants
	    	$(approvedMembersChampions).each(function(index, championDetails) {

	    		var row = $("<div class='table-row clickable' data-link='airport' onclick=\"showAirportDetails('" + championDetails.airportId + "');\"></div>")
	    		row.append("<div class='cell'>" + getRankingImg(championDetails.ranking) + "</div>")
	    		row.append("<div class='cell'>" + getCountryFlagImg(championDetails.countryCode) + championDetails.airportText + "</div>")
	    		row.append("<div class='cell'>" + getAirlineLogoImg(championDetails.airlineId) + championDetails.airlineName + "</div>")
	    		row.append("<div class='cell' align='right'>" + commaSeparateNumber(championDetails.loyalistCount) + "</div>")
	    		row.append("<div class='cell' align='right'>" + championDetails.reputationBoost + "</div>") 
	    		$('#allianceChampionAirportList').append(row)
	    	})
	    	
	    	$(applicantChampions).each(function(index, championDetails) {
	    		var row = $("<div class='table-row clickable' data-link='airport' onclick=\"showAirportDetails('" + championDetails.airportId + "');\"></div>")
	    		row.append("<div class='cell'>" + getRankingImg(championDetails.ranking) + "</div>")
                row.append("<div class='cell'>" + getCountryFlagImg(championDetails.countryCode) + championDetails.airportText + "</div>")
                row.append("<div class='cell'>" + getAirlineLogoImg(championDetails.airlineId) + championDetails.airlineName + "</div>")
                row.append("<div class='cell' align='right'>" + commaSeparateNumber(championDetails.loyalistCount) + "</div>")
                row.append("<div class='cell warning' align='right'><img src='assets/images/icons/information.png' title='Points not counted as this airline is not an approved member yet'>" + championDetails.reputationBoost + "</div>")
	    		$('#allianceChampionAirportList').append(row)
	    	})

	    	populateNavigation($('#allianceChampionAirportList'))
	    	
	    	if ($(approvedMembersChampions).length == 0 && $(applicantChampions).length == 0) {
	    		var row = $("<div class='table-row'></div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell' align='right'>-</div>")
	    		row.append("<div class='cell' align='right'>-</div>")
	    		$('#allianceChampionAirportList').append(row)
	    	}
	    	$('#allianceCanvas .totalReputation').text(result.totalReputation)
	    	$('#allianceCanvas .reputationTruncatedEntries').text(result.truncatedEntries)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateAllianceCountryChampions(allianceId) {
	$('#allianceChampionCountryList').children('div.table-row').remove()

	$.ajax({
		type: 'GET',
		url: "alliances/" + allianceId + "/championed-countries",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(championedCountries) {
	    	$(championedCountries).each(function(index, championDetails) {
                var country = championDetails.country
                var row = $("<div class='table-row clickable' data-link='country' onclick=\"showCountryView('" + country.countryCode + "');\"></div>")
                row.append("<div class='cell'>" + getRankingImg(championDetails.ranking) + "</div>")
                row.append("<div class='cell'>" + getCountryFlagImg(country.countryCode) + country.name + "</div>")
                row.append("<div class='cell'>" + getAirlineLogoImg(championDetails.airlineId) + championDetails.airlineName + "</div>")
                $('#allianceChampionCountryList').append(row)
            })

            populateNavigation($('#allianceChampionCountryList'))

            if ($(championedCountries).length == 0) {
	    		var row = $("<div class='table-row'></div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		$('#allianceChampionCountryList').append(row)
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
	    		activeUser.allianceId = newAlliance.id
	    		activeUser.allianceName = newAlliance.name
	    		updateChatTabs()
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
	    	if (activeAirline.id == removeAirlineId) { //leaving alliance
	    	    activeUser.allianceId = undefined
	    	    activeUser.allianceName = undefined
	    	    updateChatTabs()
	    	}
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

function promoteAllianceMember(promoteAirlineId) {
	var url = "airlines/" + activeAirline.id + "/promote-alliance-member/" + promoteAirlineId
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
	    	activeUser.allianceId = result.allianceId
	    	//activeUser.allianceName = result.allianceName //not yet a member
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

	var alliancePaths = []

	$.ajax({
		type: 'GET',
		url: "alliances/" + selectedAlliance.id + "/details",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
				$.each(result.links, function(index, link) {
					alliancePaths.push(drawAllianceLink(link))
				})
				var allianceBases = []
				 $.each(result.members, function(index, airline) {
                    $.merge(allianceBases, airline.bases)
                })

				var airportMarkers = updateAirportBaseMarkers(allianceBases, alliancePaths)
				//now add extra listener for alliance airports
				$.each(airportMarkers, function(key, marker) {
                        marker.addListener('mouseover', function(event) {
                            closeAlliancePopups()
                            var baseInfo = marker.baseInfo
                            $("#allianceBasePopup .city").html(getCountryFlagImg(baseInfo.countryCode) + "&nbsp;" + baseInfo.city)
                            $("#allianceBasePopup .airportName").text(baseInfo.airportName)
                            $("#allianceBasePopup .iata").html(baseInfo.airportCode)
                            $("#allianceBasePopup .airlineName").html(getAirlineLogoImg(baseInfo.airlineId) + "&nbsp;" + baseInfo.airlineName)
                            $("#allianceBasePopup .baseScale").html(baseInfo.scale)

                            var infoWindow = new google.maps.InfoWindow({ maxWidth : 1200});
                            var popup = $("#allianceBasePopup").clone()
                            popup.show()
                            infoWindow.setContent(popup[0])
                            //infoWindow.setPosition(event.latLng);
                            infoWindow.open(map, marker);
                            map.allianceBasePopup = infoWindow
                        })
                        marker.addListener('mouseout', function(event) {
                            closeAlliancePopups()
                        })
                    })


				switchMap();
				$("#worldMapCanvas").data("initCallback", function() { //if go back to world map, re-init the map
				        map.controls[google.maps.ControlPosition.TOP_CENTER].clear()
				        clearAllPaths()
                        updateAirportMarkers(activeAirline)
                        updateLinksInfo() //redraw all flight paths
                        closeAlliancePopups()
                })

				window.setTimeout(addExitButton , 1000); //delay otherwise it doesn't push to center
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    },
	    beforeSend: function() {
            $('body .loadingSpinner').show()
        },
        complete: function(){
            $('body .loadingSpinner').hide()
        }
	});
}

function addExitButton() {
    if (map.controls[google.maps.ControlPosition.TOP_CENTER].getLength() > 0) {
        map.controls[google.maps.ControlPosition.TOP_CENTER].clear()
    }
    map.controls[google.maps.ControlPosition.TOP_CENTER].push(createMapButton(map, 'Exit Alliance Flight Map', 'hideAllianceMap()', 'hideAllianceMapButton')[0]);
}

function drawAllianceLink(link) {
	var from = new google.maps.LatLng({lat: link.fromLatitude, lng: link.fromLongitude})
	var to = new google.maps.LatLng({lat: link.toLatitude, lng: link.toLongitude})
	//var pathKey = link.id
	
	var strokeColor = airlineColors[link.airlineId]
	if (!strokeColor) {
		strokeColor = "#DC83FC"
	}

    var maxOpacity = 0.7
    var minOpacity = 0.1
    var standardCapacity = 10000
    var strokeOpacity
	if (link.capacity.total < standardCapacity) {
        strokeOpacity = minOpacity + link.capacity.total / standardCapacity * (maxOpacity - minOpacity)
    } else {
        strokeOpacity = maxOpacity
    }
		
	var linkPath = new google.maps.Polyline({
			 geodesic: true,
		     strokeColor: strokeColor,
		     strokeOpacity: strokeOpacity,
		     strokeWeight: 2,
		     path: [from, to],
		     zIndex : 90,
		     link : link
		});
		
	var fromAirport = getAirportText(link.fromAirportCity, link.fromAirportCode)
	var toAirport = getAirportText(link.toAirportCity, link.toAirportCode)
	
	
	shadowPath = new google.maps.Polyline({
		 geodesic: true,
	     strokeColor: strokeColor,
	     strokeOpacity: 0.0001,
	     strokeWeight: 25,
	     path: [from, to],
	     zIndex : 100,
	     fromAirport : fromAirport,
	     fromCountry : link.fromCountryCode, 
	     toAirport : toAirport,
	     toCountry : link.toCountryCode,
	     capacity : link.capacity.total,
	     airlineName : link.airlineName,
	     airlineId : link.airlineId
	});
	
	linkPath.shadowPath = shadowPath
	

	shadowPath.addListener('mouseover', function(event) {
	    if (!map.allianceBasePopup) { //only do this if it is not hovered over base icon. This is a workaround as zIndex does not work - hovering over base icon triggers onmouseover event on the link below the icon
            $("#linkPopupFrom").html(getCountryFlagImg(this.fromCountry) + "&nbsp;" + this.fromAirport)
            $("#linkPopupTo").html(getCountryFlagImg(this.toCountry) + "&nbsp;" + this.toAirport)
            $("#linkPopupCapacity").html(this.capacity)
            $("#linkPopupAirline").html(getAirlineLogoImg(this.airlineId) + "&nbsp;" + this.airlineName)


            var infowindow = new google.maps.InfoWindow({
                 maxWidth : 1200});

            var popup = $("#linkPopup").clone()
            popup.show()
            infowindow.setContent(popup[0])

            infowindow.setPosition(event.latLng);
            infowindow.open(map);
            map.allianceLinkPopup = infowindow
        }
	})		
	shadowPath.addListener('mouseout', function(event) {
        closeAllianceLinkPopup()
	})
	
	linkPath.setMap(map)
	linkPath.shadowPath.setMap(map)
	polylines.push(linkPath)
	polylines.push(linkPath.shadowPath)

    var resultPath = { path : linkPath, shadow : shadowPath } //kinda need this so it has consistent data structure as the normal flight paths
    return resultPath
}

function showAllianceMemberDetails(allianceMember) {
    $("#allianceMemberModal").data("airlineId", allianceMember.airlineId)

    $("#allianceMemberModal .airlineName").html(getAirlineLogoImg(allianceMember.airlineId) + allianceMember.airlineName)
    $("#allianceMemberModal .allianceMemberStatus").text(allianceMember.allianceRole)
    updateAirlineBaseList(allianceMember.airlineId, $("#allianceMemberModal .baseList"))
    $("#allianceMemberModal").fadeIn(200)
}

function closeAlliancePopups() {
    if (map.allianceBasePopup) {
        map.allianceBasePopup.close()
        map.allianceBasePopup.setMap(null)
        map.allianceBasePopup = undefined
    }
    closeAllianceLinkPopup()
}

function closeAllianceLinkPopup() {
    if (map.allianceLinkPopup) {
        map.allianceLinkPopup.close()
        map.allianceLinkPopup.setMap(null)
        map.allianceLinkPopup = undefined
    }
}


function hideAllianceMap() {
    map.controls[google.maps.ControlPosition.TOP_CENTER].clear()
    clearAllPaths()
    updateAirportBaseMarkers([]) //revert base markers
    closeAlliancePopups()
    setActiveDiv($("#allianceCanvas"))
}

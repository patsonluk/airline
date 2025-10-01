var loadedAlliances = []
var loadedAlliancesById = {}
var selectedAlliance

$( document ).ready(function() {
	loadAllAlliances()
})

function showAllianceCanvas(selectedAllianceId) {
    //not the most ideal point to recheck (since current pending actions could include other canvas irrelevant to this). but this is the easiest for now
    checkPendingActions()

    setActiveDiv($("#allianceCanvas"))
	highlightTab($('.allianceCanvasTab'))
	if (!selectedAllianceId) {
        if (activeAirline) {
            selectedAllianceId = activeAirline.allianceId
        }
    }

	if (!activeAirline) {
	    loadAllAlliances(selectedAllianceId)
		$('#currentAirlineMemberDetails').hide()
	} else {
		loadAllAlliances(selectedAllianceId, true)
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

function loadCurrentAirlineMemberDetails(loadedAlliancesById) {
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
	    		$('#currentAirlineMemberDetails .allianceRanking').html(rankingImg)
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

    	if (allianceDetails.stats) {
        	$('#currentAirlineMemberDetails .stats .totalPax').text(toLinkClassValueString(allianceDetails.stats.pax))
            $('#currentAirlineMemberDetails .stats .totalLoyalist').text(commaSeparateNumber(allianceDetails.stats.loyalist))
            $('#currentAirlineMemberDetails .stats .totalRevenue').text("$" + commaSeparateNumber(allianceDetails.stats.revenue))
            $('#currentAirlineMemberDetails .stats .totalLoungeVisit').text(commaSeparateNumber(allianceDetails.stats.loungeVisit))
            $('#currentAirlineMemberDetails .stats .championedAirports').text(commaSeparateNumber(allianceDetails.stats.championedAirports))
            $('#currentAirlineMemberDetails .stats .championedCountries').text(commaSeparateNumber(allianceDetails.stats.championedCountries))

            var $airportSummary = $('#currentAirlineMemberDetails .stats .allianceChampionAirportSummary')
            $airportSummary.children("div.table-row").remove()

            var $currentRow
            $.each(allianceDetails.stats.airportStats, function(index, entry) {
                var $row
                if (index % 4 == 0) {
                    $currentRow = $('<div class="table-row">')
                    $airportSummary.append($currentRow)
                    $currentRow.append('<div class="cell">' +  entry.scale + '</div>')
                }
                $currentRow.append('<div class="cell">' +  entry.count + '</div>')
            })

             var $countrySummary = $('#currentAirlineMemberDetails .stats .allianceChampionCountrySummary')
            $countrySummary.children("div.table-row").remove()

            $.each(allianceDetails.stats.countryStats, function(index, entry) {
                if (index % 4 == 0) {
                    $currentRow = $('<div class="table-row">')
                    $countrySummary.append($currentRow)
                    $currentRow.append('<div class="cell">' +  entry.population + '</div>')
                }
                $currentRow.append('<div class="cell">' +  entry.count + '</div>')
            })

    	} else {
    	    $('#currentAirlineMemberDetails .stats .value').text('-')
    	}

        updateAllianceMission(allianceDetails.current, allianceDetails.previous, allianceDetails.isAdmin)

    	
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

function updateAllianceMission(current, previous, isAdmin) {
    if (current && current.selectedMission) {
        $('#currentAirlineMemberDetails .mission .description').text(current.selectedMission.description)
        $('#currentAirlineMemberDetails .mission .progress').text(current.selectedMission.progress + "%")
        $('#currentAirlineMemberDetails .mission .status').text(current.selectedMission.statusText)
    } else {
        $('#currentAirlineMemberDetails .mission .value').text('-')
    }

//    if (current && current.missionCandidates && current.missionCandidates.length > 0) {
//        $('#currentAirlineMemberDetails .button.currentMission').show()
//        $('#currentAirlineMemberDetails .button.currentMission').unbind('click').bind('click', function() {
//            showAllianceMissionModal(current.missionCandidates, current.selectedMission, isAdmin)
//        })
//    } else {
//        $('#currentAirlineMemberDetails .button.currentMission').hide()
//    }
    $('#currentAirlineMemberDetails .button.currentMission').show()
    disableButton($('#currentAirlineMemberDetails .button.currentMission'), "Alliance mission is disabled for now. Stay tuned!")

    if (previous && previous.missionCandidates && previous.selectedMission) {
        $('#currentAirlineMemberDetails .button.previousMission').show()
        $('#currentAirlineMemberDetails .button.previousMission').unbind('click').bind('click', function() {
            showAllianceMissionModal(previous.missionCandidates, previous.selectedMission, isAdmin)
        })
    } else {
        $('#currentAirlineMemberDetails .button.previousMission').hide()
    }
}

function loadAllAlliances(selectedAllianceId, loadCurrentAirlineDetails) {
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
	    async: true,
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
	    		if (!loadedAlliancesById[selectedAlliance.id]) { //alliance was just deleted
	    			selectedAlliance = undefined
	    			$('#allianceDetails').hide()
	    		}
			} else {
				$('#allianceDetails').hide()
			}

			if (loadCurrentAirlineDetails) {
			    loadCurrentAirlineMemberDetails(loadedAlliancesById)
			}


			if (selectedAllianceId) {
                selectAlliance(selectedAllianceId, true)
			}
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

function updateAllianceTable(sortProperty, sortOrder) {
	var allianceTable = $("#allianceCanvas #allianceTable")
	
	allianceTable.children("div.table-row").remove()
	
	//sort the list
	loadedAlliances.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	$.each(loadedAlliances, function(index, alliance) {
		var row = $("<div class='table-row clickable' data-alliance-id='" + alliance.id + "' onclick=\"selectAlliance('" + alliance.id + "')\"></div>")
//		var countryFlagImg = ""
//		if (airline.countryCode) {
//			countryFlagImg = getCountryFlagImg(airline.countryCode)
//		}
		row.append("<div class='cell'>" + alliance.name + "</div>")
		if (alliance.leader) {
			row.append("<div class='cell'>" + getAirlineSpan(alliance.leader.id, alliance.leader.name) + "</div>")
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

function selectAlliance(allianceId, isScrollToRow) {
	//update table
	var $row = $("#allianceCanvas #allianceTable .table-row[data-alliance-id='" + allianceId + "']")
	$row.siblings().removeClass("selected")
	$row.addClass("selected")
	loadAllianceDetails(allianceId)

    if (isScrollToRow) {
        scrollToRow($row, $("#allianceCanvas #allianceTableContainer"))
    }
}

function loadAllianceDetails(allianceId) {
	updateAllianceBasicsDetails(allianceId)
	updateAllianceBonus(allianceId)
	updateAllianceChampions(allianceId)
	updateAllianceHistory(allianceId)
	updateAllianceTagColor(allianceId)
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
		$('#allianceDetails .allianceRanking').html(rankingImg)
	} else {
		$('#allianceDetails .allianceRanking').text('-')
	}
	$("#allianceMemberList").children("div.table-row").remove()

	var isAdmin = false
	$.each(alliance.members, function(index, member) {
        if (activeAirline && member.airlineId == activeAirline.id) {
            isAdmin = member.isAdmin
        }
	})


    $.ajax({
        type: 'GET',
        url: "alliances/" + allianceId + "/member-login-status",
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(loginStatusByAirlineId) {
            $.each(alliance.members, function(index, member) {
                var row = $("<div class='table-row clickable' style='height: 20px;' onclick='showAllianceMemberDetails($(this).data(\"member\"))'></div>")
                row.data("member", member)
                row.attr("data-airline-id", member.airlineId)
                let loginStatus = loginStatusByAirlineId[member.airlineId]
                row.append("<div class='cell' style='vertical-align:middle; width: 10px;'><img src='" + getStatusLogo(loginStatus) + "' title='" + getStatusTitle(loginStatus) + "' style='vertical-align:middle;'/>")
                row.append("<div class='cell' style='vertical-align: middle;'>" + getAirlineSpan(member.airlineId, member.airlineName) + "</div>")
                if (member.allianceRole == "Applicant") {
                    row.append("<div class='cell warning' style='vertical-align: middle;'>" + member.allianceRole + "</div>")
                } else {
                    row.append("<div class='cell' style='vertical-align: middle;'>" + member.allianceRole + "</div>")
                }
                if (activeAirline) {
                    var $actionCell = $("<div class='cell action' style='vertical-align: middle;'></div>")

                    row.append($actionCell)

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

                        if (result.memberActions) {
                            $.each(result.memberActions, function(index, entry) {
                                var $cell = $("#allianceMemberList .table-row[data-airline-id='" + entry.airlineId + "'] .action")


                                if (entry.acceptRejection) {
                                    $cell.append("<img src='assets/images/icons/exclamation-circle.png' class='button disabled' title='Cannot accept member : " + entry.rejection + "'>")
                                } else if (entry.acceptPrompt) {
                                    var $icon = $("<img src='assets/images/icons/tick.png' class='button' title='Accept Member'>")
                                    $icon.click(function(event) {
                                        event.stopPropagation()
                                        promptConfirm(entry.acceptPrompt, acceptAllianceMember, entry.airlineId)
                                    })
                                    $cell.append($icon)
                                }

                                if (!entry.promoteRejection && entry.promotePrompt) {
                                    var $icon = $("<img src='assets/images/icons/user-promote.png' class='button' title='Promote Member'>")
                                    $icon.click(function(event) {
                                        event.stopPropagation()
                                        promptConfirm(entry.promotePrompt, promoteAllianceMember, entry.airlineId)
                                    })
                                    $cell.append($icon)
                                }
                                if (!entry.demoteRejection && entry.demotePrompt) {
                                    var $icon = $("<img src='assets/images/icons/user-demote.png' class='button' title='Demote Member'>")
                                    $icon.click(function(event) {
                                        event.stopPropagation()
                                        promptConfirm(entry.demotePrompt, demoteAllianceMember, entry.airlineId)
                                    })
                                    $cell.append($icon)
                                }
                                if (!entry.removeRejection && entry.removePrompt) {
                                    var $icon = $("<img src='assets/images/icons/cross.png' class='button' title='Remove Member'>")
                                    $icon.click(function(event) {
                                        event.stopPropagation()
                                        promptConfirm(entry.removePrompt, removeAllianceMember, entry.airlineId)
                                    })
                                    $cell.append($icon)
                                }
                            })
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
        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
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

function updateAllianceTagColor(allianceId) {
    if (activeAirline) {
        $('#allianceDetails .tagColor.picker').off("change.setColor")

        $('#allianceDetails .tagColor.picker').on("change.setColor", function() {
            var newColor = $(this).val()

           checkAllianceLabelColorAction(allianceId, function(airlineOverride) {
            setAllianceLabelColor(allianceId, newColor, function() {
                var selectedSortHeader = $('#allianceTableSortHeader .table-header .cell.selected')
                updateAllianceTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
                updateAllianceBasicsDetails(allianceId)
            }, airlineOverride)
           })

        });

        $.ajax({
            type: 'GET',
            url: "airlines/" + activeAirline.id + "/alliance-label-color?allianceId=" + allianceId,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(result) {
                if (result.color) {
                    $('#allianceDetails .tagColor.picker').val('#' + result.color)
                } else {
                    $('#allianceDetails .tagColor.picker').val('')
                }
                $('#allianceDetails .tagColor').show()
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });


    } else {
        $('#allianceDetails .tagColor').hide()
    }
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
	    		activeAirline.allianceId = newAlliance.id
	    		activeAirline.allianceName = newAlliance.name
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
	    	    activeAirline.allianceId = undefined
	    	    activeAirline.allianceName = undefined
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

function demoteAllianceMember(promoteAirlineId) {
	var url = "airlines/" + activeAirline.id + "/demote-alliance-member/" + promoteAirlineId
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
	        activeAirline.allianceId = result.allianceId
	    	showAllianceCanvas()
	    	//activeAirline.allianceName = result.allianceName //not yet a member
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
			//group links by endpoints
			var groupedLinks = {};
			$.each(result.links, function(index, link) {
				var key = [link.fromAirportId, link.toAirportId].sort().join('-');

				if (!groupedLinks[key]) {
					groupedLinks[key] = {
						links: [],
						totalCapacity: 0,
						fromLatitude: link.fromLatitude,
						fromLongitude: link.fromLongitude,
						toLatitude: link.toLatitude,
						toLongitude: link.toLongitude,
						fromAirportCity: link.fromAirportCity,
						fromAirportCode: link.fromAirportCode,
						fromCountryCode: link.fromCountryCode,
						toAirportCity: link.toAirportCity,
						toAirportCode: link.toAirportCode,
						toCountryCode: link.toCountryCode
					};
				}
				groupedLinks[key].links.push(link);
				groupedLinks[key].totalCapacity += link.capacity.total;
			});

			$.each(groupedLinks, function(key, groupedLink) {
				alliancePaths.push(drawAllianceLink(groupedLink));
			});

			//consolidate information for airports with multiple alliance bases
			var rawAllianceMembersBases = []
			$.each(result.members, function(index, airline) {
				if (airline.role !== "APPLICANT") {
					$.merge(rawAllianceMembersBases, airline.bases)
				}
			})

			var basesByAirportId = {}
			$.each(rawAllianceMembersBases, function(index, base) {
				if (!basesByAirportId[base.airportId]) {
					basesByAirportId[base.airportId] = []
				}
				basesByAirportId[base.airportId].push(base)
			})

			var allianceBasesForMap = []
			$.each(basesByAirportId, function(airportId, basesAtThisAirport) {
				if (basesAtThisAirport.length === 1) {
					allianceBasesForMap.push(basesAtThisAirport[0])
				} else if (basesAtThisAirport.length === 2) {
					var base1 = basesAtThisAirport[0]
					var base2 = basesAtThisAirport[1]
					var primaryBase = base1.headquarter ? base1 : base2
					var partnerBase = base1.headquarter ? base2 : base1
					primaryBase.alliancePartnerBaseInfo = partnerBase
					allianceBasesForMap.push(primaryBase)
				}
			})

			var airportMarkers = updateAirportBaseMarkers(allianceBasesForMap, alliancePaths)

			$.each(airportMarkers, function(key, marker) {
				marker.addListener('mouseover', function(event) {
					closeAlliancePopups()
					var baseInfo = marker.baseInfo
					var partnerInfo = baseInfo.alliancePartnerBaseInfo

					var $popup = $("#allianceBasePopup").clone()

					$popup.find(".city").html(getCountryFlagImg(baseInfo.countryCode) + " " + baseInfo.city)
					$popup.find(".airportName").text(baseInfo.airportName)
					$popup.find(".iata").html(baseInfo.airportCode)
					$popup.find(".airlineName").html(getAirlineLogoImg(baseInfo.airlineId) + " " + baseInfo.airlineName + (baseInfo.headquarter ? " (HQ)" : ""))
					$popup.find(".baseScale").html(baseInfo.scale)

					if (partnerInfo) {
						var $table = $popup.find(".table")
						$table.append("<hr style='margin: 8px 0; border: 0; border-top: 1px solid #ccc;'>")

						var $airlineRowTemplate = $popup.find(".airlineName").closest(".table-row")
						var $partnerAirlineRow = $airlineRowTemplate.clone()
						$partnerAirlineRow.find(".airlineName").html(getAirlineLogoImg(partnerInfo.airlineId) + " " + partnerInfo.airlineName + (partnerInfo.headquarter ? " (HQ)" : ""))
						$table.append($partnerAirlineRow)

						var $scaleRowTemplate = $popup.find(".baseScale").closest(".table-row")
						var $partnerScaleRow = $scaleRowTemplate.clone()
						$partnerScaleRow.find(".baseScale").html(partnerInfo.scale)
						$table.append($partnerScaleRow)
					}

					$popup.show()

					var infoWindow = new google.maps.InfoWindow({ maxWidth : 1200 });
					infoWindow.setContent($popup[0])
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

function drawAllianceLink(groupedLink) {
	var from = new google.maps.LatLng({lat: groupedLink.fromLatitude, lng: groupedLink.fromLongitude})
	var to = new google.maps.LatLng({lat: groupedLink.toLatitude, lng: groupedLink.toLongitude})

	//use link with highest capacity for route color
	var dominantLink = groupedLink.links.reduce(function(prev, current) {
		return (prev.capacity.total > current.capacity.total) ? prev : current;
	});

	var strokeColor = airlineColors[dominantLink.airlineId]
	if (!strokeColor) {
		strokeColor = "#DC83FC"
	}

	var maxOpacity = 0.7
	var minOpacity = 0.1
	var standardCapacity = 10000
	var strokeOpacity
	if (groupedLink.totalCapacity < standardCapacity) {
		strokeOpacity = minOpacity + groupedLink.totalCapacity / standardCapacity * (maxOpacity - minOpacity)
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
		//for compatibility with other functions, assign the dominant link object
		link : dominantLink
	});

	var fromAirport = getAirportText(dominantLink.fromAirportCity, dominantLink.fromAirportCode)
	var toAirport = getAirportText(dominantLink.toAirportCity, dominantLink.toAirportCode)

	var shadowPath = new google.maps.Polyline({
		geodesic: true,
		strokeColor: strokeColor,
		strokeOpacity: 0.0001,
		strokeWeight: 25,
		path: [from, to],
		zIndex : 100,
		fromAirport : fromAirport,
		fromCountry : groupedLink.fromCountryCode,
		toAirport : toAirport,
		toCountry : groupedLink.toCountryCode,
		links: groupedLink.links, //full array of individual links
		totalCapacity: groupedLink.totalCapacity
	});

	linkPath.shadowPath = shadowPath

	shadowPath.addListener('mouseover', function(event) {
		if (!map.allianceBasePopup) {
			var $popupContent = $("#linkPopup").clone();

			$popupContent.find("#linkPopupFrom").html(getCountryFlagImg(this.fromCountry) + " " + this.fromAirport)
			$popupContent.find("#linkPopupTo").html(getCountryFlagImg(this.toCountry) + " " + this.toAirport)
			if (this.links.length > 1) {
				var capacityBreakdown = this.links.map(function (l) {
					return commaSeparateNumber(l.capacity.total);
				}).join(' / ');
				$popupContent.find("#linkPopupCapacity").html(commaSeparateNumber(this.totalCapacity) + " (" + capacityBreakdown + ")")
			} else {
				$popupContent.find("#linkPopupCapacity").html(commaSeparateNumber(this.totalCapacity))
			}

			var $airlineRowTemplate = $popupContent.find("#linkPopupAirline").closest(".table-row");
			if (this.links.length > 1) {
				$airlineRowTemplate.find(".label h5").text("Airlines:");
			} else {
				$airlineRowTemplate.find(".label h5").text("Airline:");
			}

			var firstLink = this.links[0];
			$airlineRowTemplate.find("#linkPopupAirline").html(getAirlineLogoImg(firstLink.airlineId) + " " + firstLink.airlineName);

			//add new rows for subsequent airlines
			for (var i = 1; i < this.links.length; i++) {
				var link = this.links[i];
				var $newRow = $airlineRowTemplate.clone();
				$newRow.find(".label").html("");
				$newRow.find(".value").removeAttr('id').html(getAirlineLogoImg(link.airlineId) + " " + link.airlineName);
				$airlineRowTemplate.parent().append($newRow);
			}

			var infowindow = new google.maps.InfoWindow({ maxWidth : 1200});
			$popupContent.show()
			infowindow.setContent($popupContent[0])
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

function showAllianceMissionModal(candidates, selectedMission, isAdmin) {
    $('#allianceMissionModal .missionStatsGraph').hide()
    $('#allianceMissionModal .allianceMissionRewards').empty()

    //update phase icons
    var phase
    var tillNextPhase
    if (selectedMission) {
        if (selectedMission.status == "SELECTED") {
            phase = 1
        } else if (selectedMission.status == "IN_PROGRESS") {
            phase = 2
        } else {
            phase = 3
        }
        tillNextPhase = selectedMission.tillNextPhase
    } else {
        phase = 1
        tillNextPhase = candidates[0].tillNextPhase

    }
    $('#allianceMissionModal .phase img').attr('src', 'assets/images/icons/12px/status-grey.png')
    $('#allianceMissionModal .phase .tillNextPhase').text('')
    $('#allianceMissionModal .phase[data-phase="' + phase + '"] img').attr('src', 'assets/images/icons/12px/status-green.png')
    if (tillNextPhase > 0) {
        $('#allianceMissionModal .phase[data-phase="' + phase + '"] .tillNextPhase').text("(" + tillNextPhase + " week(s) to advance to next phase)")
    }

    $('#allianceMissionModal .allianceMissionCandidates').empty()
    $.each(candidates, function(index, candidate) {
        var $candidateDiv = $('<div style="margin: 5px; padding: 10px; border-radius: 0.5em;">' + candidate.description + '</div>').appendTo($('#allianceMissionModal .allianceMissionCandidates'))
        var $checkButton = $('<div class="round-button tick" style="margin-right: 10px;"></div>')
        if (phase == 1) {
            if (isAdmin) {
                $checkButton.bind("click", function(){
                    selectAllianceMission(candidate, function(result) {
                        $('#allianceMissionModal .allianceMissionCandidates .selected').removeClass("selected")
                        updateAllianceMission(result.current, result.previous, true)
                        showAllianceMissionModal(result.current.missionCandidates, result.current.selectedMission, true)
                    })
                })
            } else {
                $checkButton.addClass('disabled')
                $checkButton.prop('title', 'Only admins can select mission for alliance')
            }
        } else {
            $checkButton.addClass('disabled')
        }
        //TODO enable/disable the button

        $candidateDiv.prepend($checkButton)
        if (selectedMission && candidate.id == selectedMission.id) {
            $candidateDiv.find('.round-button').removeClass('disabled')
            $candidateDiv.addClass('selected')
        }

        var $difficultyBar = $('<div>Difficulty:</div>')
        var $starBar = generateSimpleImageBar("assets/images/icons/star.png", candidate.difficulty)
        $starBar.css('display', 'inline-block')
        $starBar.css('vertical-align', 'text-bottom')
        $starBar.css('margin', '5px 0px 0px 5px')
        $difficultyBar.append($starBar)
        $candidateDiv.append($difficultyBar)

        if ("lastWeekValue" in candidate) {
            var $referenceBar = $("<div>Reference value from last week:</div>")
            var $lastWeekValueSpan = $("<span style='margin-left: 5px;'></span>")
            $lastWeekValueSpan.text(commaSeparateNumber(candidate["lastWeekValue"]))
            $referenceBar.append($lastWeekValueSpan)
            $candidateDiv.append($referenceBar)
        }
    })

    if (selectedMission) {
        $('#allianceMissionModal .mission .description').text(selectedMission.description)
        $('#allianceMissionModal .mission .progress').text(selectedMission.progress + "%")
        $('#allianceMissionModal .mission .status').text(selectedMission.statusText)

        $.each($('#allianceMissionModal .mission .stats'), function(index, statsRow) {
            var $statsRow = $(statsRow)
            var key = $statsRow.data("key")
            if (key) {
                if (key in selectedMission.stats) {
                    $statsRow.find('.value').text(commaSeparateNumber(selectedMission.stats[key]))
                    $statsRow.show()
                } else {
                    $statsRow.hide()
                }
            }
        })
        if (phase >= 2) { //only show stats graph if in progress
            var url = "airlines/" + activeAirline.id + "/mission-stats/" + selectedMission.id
            	$.ajax({
            		type: 'GET',
            		url: url,
            		contentType: 'application/json; charset=utf-8',
            	    dataType: 'json',
            	    success: function(result) {
            	        plotMissionStatsGraph(result.stats, result.threshold, $('#allianceMissionModal .missionStatsGraph'))
            	    	$('#allianceMissionModal .missionStatsGraph').show()
            	    },
                    error: function(jqXHR, textStatus, errorThrown) {
                        console.log(JSON.stringify(jqXHR));
                        console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            	    }
            	});
        }

        showAllianceMissionRewards(selectedMission.id, selectedMission.potentialRewards, selectedMission.progress >= 100, phase)

    } else {
        $('#allianceMissionModal .mission .value').text('-')
        $('#allianceMissionModal .mission .stats').hide()
        //$('#allianceMissionModal .allianceMissionCandidates').hide()
    }

    if (phase < 3) {
        $('#allianceMissionModal .phase[data-phase="3"] .description').text("Potential Rewards")
    } else {
        var description = selectedMission.progress >= 100 ? "Successful" : "Failed"
        $('#allianceMissionModal .phase[data-phase="3"] .description').text(description)
    }

    $("#allianceMissionModal").fadeIn(200)
}

function showAllianceMissionRewards(missionId, rewards, isSuccessful, phase) {
    $.each(rewards, function(index, reward) {
        var $rewardDiv = $('<div class="section">' + reward.description + '</div>')
        $rewardDiv.data("id", reward.id)
        $lockStatusImg = $("<img>")
        if (isSuccessful) {
            if (phase == 2 || reward.isAvailable) {
                $lockStatusImg.attr('src', 'assets/images/icons/unlock-tick.png')
                $lockStatusImg.attr('title', 'Unlocked')
            } else {
                if (reward.isClaimed) {
                    $lockStatusImg.attr('src', 'assets/images/icons/tick.png')
                    $lockStatusImg.attr('title', 'Claimed')
                } else {
                    $lockStatusImg.attr('src', 'assets/images/icons/cross.png')
                    $lockStatusImg.attr('title', 'Not selected')
                }
            }
        } else {
            $lockStatusImg.attr('src', 'assets/images/icons/lock.png')
            $lockStatusImg.attr('title', 'Not yet unlocked')
        }

        $rewardDiv.prepend($lockStatusImg)
        if (phase >= 3 && reward.isAvailable) {
            $rewardDiv.addClass("clickable")
            $rewardDiv.bind("click", function() {
                promptConfirm("Do you want to claim this reward?", function() {
                    var url = "airlines/" + activeAirline.id + "/select-alliance-mission-reward/" + missionId + "/" + reward.id
                    $.ajax({
                        type: 'GET',
                        url: url,
                        contentType: 'application/json; charset=utf-8',
                        dataType: 'json',
                        success: function(result) {
                            updateAirlineInfo(activeAirline.id)
                            loadCurrentAirlineMemberDetails(loadedAlliancesById)
                            closeModal($("#allianceMissionModal"))
                        },
                        error: function(jqXHR, textStatus, errorThrown) {
                            console.log(JSON.stringify(jqXHR));
                            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
                        }
                    })
                })
            })
        }
        $('#allianceMissionModal .allianceMissionRewards').append($rewardDiv)
    })
    $('#allianceMissionModal .allianceMissionRewards').show()
}

function selectAllianceMission(mission, callback) {
    var url = "airlines/" + activeAirline.id + "/select-alliance-mission/" + mission.id
	$.ajax({
		type: 'GET',
		url: url,
		contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	callback(result)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});

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

function checkResetAllianceLabelColor(targetAllianceId) {
    checkAllianceLabelColorAction(targetAllianceId, function(airlineOverride) {
        resetAllianceLabelColor(targetAllianceId, function() {
            var selectedSortHeader = $('#allianceTableSortHeader .table-header .cell.selected')
            updateAllianceTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
            updateAllianceBasicsDetails(targetAllianceId)
            $('#allianceDetails .tagColor.picker').val('')
        },airlineOverride)
    })
}

function checkAllianceLabelColorAction(targetAllianceId, colorAction) {
    if (activeAirline.isAllianceAdmin) {
        promptSelection("Do you want to apply this to all your alliance members or just your airline?", ["Alliance", "Airline"], function(changeType) {
            var airlineOverride = (changeType === "Airline")
            colorAction(airlineOverride)
        })
    } else {
        colorAction(true)
    }
}

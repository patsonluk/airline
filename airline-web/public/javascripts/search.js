var loadedOlympicsEvents = []
var loadedAlerts = []


function showSearchCanvas() {
    var titlesContainer = $("#searchCanvas div.titlesContainer")
    positionTitles(titlesContainer)
    setActiveDiv($("#searchCanvas"))
	$("#searchCanvas").css("display", "flex")
	highlightTab($('.searchCanvasTab'))
	$("#routeSearchResult").empty()
	$("#historySearchResult .table-row").empty()
	$('#searchCanvas .searchContainer input').val('')
	$('#searchCanvas .searchContainer input').removeData("selectedId")

    refreshSearchDiv(titlesContainer.children('div.selected'))
    var titleSelections =  titlesContainer.children('div.titleSelection')
    titleSelections.off("click.refreshSearchDiv");
    titleSelections.on("click.refreshSearchDiv", function(){
      refreshSearchDiv($(this))
    });
    updateNavigationArrows(titlesContainer)

    initializeHistorySearch()
}

function initializeHistorySearch() {
    var locationSearchInput = $('#searchCanvas div.searchCriterion input')
    locationSearchInput.on('confirmSelection', function(e) {
        var disablingInputs
        if ($(this).data("searchGroup")) {
            var searchGroup = $(this).data("searchGroup")
            disablingInputs = $(this).closest('div.searchCriterion').siblings('div.searchCriterion').find('input[data-search-group="' + searchGroup + '"]') //disable other to-s
        }

        disablingInputs.val('')
        disablingInputs.removeData("selectedId") //disable other inputs in div.searchCriterion
        //alert('My Custom Event - Change Data Called! for ' + $(this).data("selectedId"));
    })
    //register sort function for result table
     var sortHeaderCells = $('#searchCanvas div.historySearch div.sortHeader div.cell.clickable')
     sortHeaderCells.on("click.toggleSort", function(){
       toggleTableSortOrder($(this), function(sortProperty, sortOrder) {
        updateLinkHistoryTable(sortProperty, sortOrder)
       })
     });


}

function refreshSearchDiv(selectedDiv) {
    var searchTitleType = selectedDiv.data('searchType')
    if (searchTitleType === 'route') {
        $('#searchCanvas div.routeSearch').show();
        $('#searchCanvas div.routeSearch').siblings('.searchContainer').hide();
    } else if (searchTitleType === 'history') {
        $('#searchCanvas div.historySearch').show();
        $('#searchCanvas div.historySearch').siblings('.searchContainer').hide();
    } else if (searchTitleType === 'research') {
        $('#searchCanvas div.research').show();
        $('#researchSearchResult').hide();
        $('#searchCanvas div.research').siblings('.searchContainer').hide();
    }

}


function searchFlight(fromAirportId, toAirportId) {
    if (fromAirportId && toAirportId) {
        var url = "search-route/" + fromAirportId + "/" + toAirportId

        $.ajax({
            type: 'GET',
            url: url,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(searchResult) {
                $("#routeSearchResult").empty()

                $.each(searchResult, function(index, entry) {
                    var itineraryDiv = $("<div class='section itinerary' onclick='toggleSearchLinkDetails($(this))'></div>")
                    var total = 0

                    var routeDiv = $("<div style='float:left; width : 85%'></div>")
                    itineraryDiv.append(routeDiv)

                     //Generate Summary
                    var startLink
                    for (i = 0 ; i < entry.route.length; i ++) {
                        if (entry.route[i].transportType == 'FLIGHT') {
                            startLink = entry.route[i]
                            break;
                        }
                    }

                    var endLink
                    for (i = entry.route.length - 1 ; i >= 0 ; i --) {
                        if (entry.route[i].transportType == 'FLIGHT') {
                            endLink = entry.route[i]
                            break;
                        }
                    }

                    var startDay = Math.floor(startLink.departure / (24 * 60))
                    var summaryDiv = $("<div class='summary'  style='display: flex; align-items: center;'></div>")
                    summaryDiv.append("<div style='width: 50%; float:left;'> " + getAirlineTimeSlotText(startLink.departure, startDay) + " - " + getAirlineTimeSlotText(endLink.arrival, startDay) + "</div>")
                    summaryDiv.append("<div style='width: 50%; float:left;'> " + getDurationText(endLink.arrival - startLink.departure) +  "</div>")
                    summaryDiv.append("<div style='clear:both; '></div>")

                    routeDiv.append(summaryDiv)

                    var previousLink
                    var flightCount = 0
                    $.each(entry.route, function(index, link) {
                        if (link.transportType == 'FLIGHT') {
                            flightCount ++
                            //check shuttle
                            var preShuttle
                            var postShuttle
                            if (index > 0) {
                                if (entry.route[index - 1].transportType == 'SHUTTLE') {
                                    preShuttle = entry.route[index - 1]
                                }
                            }
                            if (index < entry.route.length - 1) {
                                if (entry.route[index + 1].transportType == 'SHUTTLE') {
                                    postShuttle = entry.route[index + 1]
                                }
                            }

                            var linkDiv = $("<div style='margin-bottom: 10px;'></div>")
                            var linkSummaryDiv = $("<div style='margin : 10px 0;'></div>")
                            linkSummaryDiv.append("<div style='width: 50%; float:left; display: flex; align-items: center;'> " + getAirlineLogoImg(link.airlineId) + "<span class='summary'>" + link.airlineName + "</span></div>")
                            var linkDurationText = getDurationText(link.arrival - link.departure)
                            var remarks = []
                            if (preShuttle) {
                                remarks.push("Depart from " + link.fromAirportIata)
                            }
                            if (postShuttle) {
                                remarks.push("Arrive at " + link.toAirportIata)
                            }
                            if (previousLink) {
                                remarks.push("+" + getDurationText(link.departure - previousLink.arrival) + " layover at " + link.fromAirportIata)
                            }

                            if (remarks.length > 0) {
                                var remarksText = ""
                                for (i = 0 ; i < remarks.length; i++) {
                                    if (i > 0) {
                                        linkDurationText += ", "
                                    }
                                    remarksText += remarks[i]
                                }
                                linkDurationText += "(" + remarksText + ")"
                            }

                            linkSummaryDiv.append("<div style='width: 50%; float:left;'> " + linkDurationText + "</div>")
    //                        airlineSpan.append("<div style='width: 50%; float:left;'> " + link.flightCode + "&nbsp;" + getAirlineTimeSlotText(link.departure, startDay) + " - " + getAirlineTimeSlotText(link.arrival, startDay) + "</div>")
                            linkSummaryDiv.append("<div style='clear:both; '></div>")
                            linkDiv.append(linkSummaryDiv)

                            var linkDetailDiv = $("<div style='display: flex; align-items: center; margin: 0 10px;' class='linkDetails'></div>")
                            var linkDetailLeftDiv = $("<div style='width: 50%;'></div>").appendTo(linkDetailDiv)
                            linkDetailLeftDiv.append("<div style='display: inline-block; width: 75px;' class='summary'> " + link.flightCode + "</div>")
                            linkDetailLeftDiv.append("<span>" + getAirlineTimeSlotText(link.departure, startDay) + " - " + getAirlineTimeSlotText(link.arrival, startDay) + "</span>")
                            linkDetailLeftDiv.append("<div>$" + link.price + " (" +  link.linkClass + ")</div>")
                            $featureIconsDiv = getLinkFeatureIconsDiv(link.features, preShuttle || postShuttle)
                            linkDetailLeftDiv.append($featureIconsDiv)
                            linkDetailLeftDiv.append(getLinkReviewDiv(link.computedQuality))


                            var linkDetailRightDiv = $("<div style='width: 50%;'></div>").appendTo(linkDetailDiv)
                            linkDetailRightDiv.append("<div style='display: flex; align-items: center;'>" + getAirportText(link.fromAirportCity, link.fromAirportIata) + "<img src='assets/images/icons/arrow.png' style='margin: 0 5px;'>" + getAirportText(link.toAirportCity, link.toAirportIata) + "</div>")
                            linkDetailRightDiv.append("<div>Aircraft : " + (link.airplaneModelName ? link.airplaneModelName : "-") + "</div>")
                            if (link.operatorAirlineId) { //code share
                                linkDetailRightDiv.append("<div>Operated by " + getAirlineLogoImg(link.operatorAirlineId) + link.operatorAirlineName + "</div>")
                            }
                            if (preShuttle) {
                                linkDetailRightDiv.append("<div>Free shuttle from " + preShuttle.fromAirportText + " to " + preShuttle.toAirportText + " by " + getAirlineLogoImg(preShuttle.airlineId) + preShuttle.airlineName + "</div>")
                            }
                            if (postShuttle) {
                                linkDetailRightDiv.append("<div>Free shuttle from " + postShuttle.fromAirportText + " to " + postShuttle.toAirportText + " by " + getAirlineLogoImg(postShuttle.airlineId) + postShuttle.airlineName + "</div>")
                            }


                            linkDetailDiv.append("<div style='clear:both; '></div>")

                            linkDetailDiv.hide()
                            linkDiv.append(linkDetailDiv)

    //                        var directionDiv = $("<div style='display: flex; align-items: center;' >" + link.flightCode + "&nbsp;" + getAirportText(link.fromAirportCity, link.fromAirportIata) + "<img src='assets/images/icons/arrow.png' style='margin: 0 5px;'>" + getDurationText(link.duration) + " " + link.linkClass + "</div>")
    //                        linkDiv.append(directionDiv)

                            routeDiv.append(linkDiv)
                            total += link.price
                            previousLink = link
                        }
                    })
                    var stopDescription
                    if (flightCount == 1) {
                        stopDescription = "Direct Flight"
                    } else if (flightCount == 2) {
                        stopDescription = "1 Stop"
                    } else {
                        stopDescription = (flightCount - 1) + " Stops"
                    }
                    var priceDiv = $("<div style='float: right; width: 15%;'><div class='price'>$ " + total + "</div></div>")
                    var priceTextDiv = priceDiv.find('div.price')

                    $.each(entry.remarks, function(index, remark) {
                        if (remark == 'BEST_SELLER') {
                            priceTextDiv.css("color", "darkgreen")
                            priceTextDiv.after("<div style='display:inline-block;' class='remark'>BEST SELLER</div>")
                        } else if (remark == 'BEST_DEAL') {
                            priceTextDiv.css("color", "darkgreen")
                            priceTextDiv.after("<div style='display:inline-block;' class='remark'>BEST DEAL</div>")
                        }
                    })


                    priceDiv.append($("<div style='margin-top: 5px;'>" + stopDescription + "</div>"))


                    itineraryDiv.append(priceDiv)
                    itineraryDiv.append("<div style='clear:both;'></div>")
                    $("#routeSearchResult").append(itineraryDiv)
                })



                if (searchResult.length == 0) {
                    $("#routeSearchResult").append("<div class='ticketTitle'>Sorry, no flights available.</div>")
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
}


function searchLinkHistory() {
    var url = "search-link-history"

    var fromAirportId = $('#searchCanvas div.historySearch input.fromAirport').data('selectedId')
    var toAirportId = $('#searchCanvas div.historySearch input.toAirport').data('selectedId')
    var fromCountryCode = $('#searchCanvas div.historySearch input.fromCountry').data('selectedId')
    var toCountryCode = $('#searchCanvas div.historySearch input.toCountry').data('selectedId')
    var fromZone = $('#searchCanvas div.historySearch input.fromZone').data('selectedId')
    var toZone = $('#searchCanvas div.historySearch input.toZone').data('selectedId')
    var airlineId = $('#searchCanvas div.historySearch input.airline').data('selectedId')
    var allianceId = $('#searchCanvas div.historySearch input.alliance').data('selectedId')

    var capacity = $('#searchCanvas input.capacity').val() ? parseInt($('#searchCanvas input.capacity').val()) : null
    var capacityDelta = $('#searchCanvas input.capacityDelta').val() ? parseInt($('#searchCanvas input.capacityDelta').val()) : null

    var searchData = {}

    if (fromAirportId) {
        searchData["fromAirportId"] = parseInt(fromAirportId)
    }
    if (toAirportId) {
        searchData["toAirportId"] = parseInt(toAirportId)
    }
    if (fromCountryCode) {
        searchData["fromCountryCode"] = fromCountryCode
    }
    if (toCountryCode) {
        searchData["toCountryCode"] = toCountryCode
    }
    if (fromZone) {
        searchData["fromZone"] = fromZone
    }
    if (toZone) {
        searchData["toZone"] = toZone
    }
    if (airlineId) {
        searchData["airlineId"] = airlineId
    }
    if (allianceId) {
        searchData["allianceId"] = allianceId
    }

    if ($('#searchCanvas input.capacity').val()) {
        searchData["capacity"] = parseInt($('#searchCanvas input.capacity').val())
    }

    if ($('#searchCanvas input.capacityDelta').val()) {
        searchData["capacityDelta"] = parseInt($('#searchCanvas input.capacityDelta').val())
    }


    $.ajax({
        type: 'POST',
        url: url,
        contentType: 'application/json; charset=utf-8',
        data: JSON.stringify(searchData),
        dataType: 'json',
        success: function(searchResult) {
            $("#linkHistorySearchResult").empty()
            $("#searchCanvas .linkHistorySearchTable").data("entries", searchResult)
            updateLinkHistoryTable()
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



function updateLinkHistoryTable(sortProperty, sortOrder) {
	var linkHistoryTable = $("#searchCanvas .linkHistorySearchTable")
	linkHistoryTable.children("div.table-row").remove()

    var loadedData = linkHistoryTable.data('entries')
	//sort the list
	//loadedLinks.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	loadedData = sortPreserveOrder(loadedData, sortProperty, sortOrder == "ascending")


	$.each(loadedData, function(index, link) {
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell'>" + getCycleDeltaText(link.cycleDelta) + "</div>")
        row.append("<div class='cell'>" + getAirlineLogoImg(link.airlineId) + link.airlineName + "</div>")
		row.append("<div class='cell'>" + getCountryFlagImg(link.fromCountryCode) + getAirportText(link.fromAirportCity, link.fromAirportIata) + "</div>")
		row.append("<div class='cell'>" + getCountryFlagImg(link.toCountryCode) + getAirportText(link.toAirportCity, link.toAirportIata) + "</div>")
//		row.append("<div class='cell'>" + link.airplaneModelName + "</div>")
		$("<div class='cell' align='right'></div>").appendTo(row).append(getCapacitySpan(link.capacity, link.frequency))
		$("<div class='cell' align='right'></div>").appendTo(row).append(getCapacityDeltaSpan(link.capacityDelta))
		$("<div class='cell'></div>").appendTo(row).text(toLinkClassValueString(link.price, '$'))
		$("<div class='cell'></div>").appendTo(row).append(getPriceDeltaSpan(link.priceDelta))


		linkHistoryTable.append(row)
	});

	if (loadedData.length == 0) {
		var row = $("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div></div>")
		linkHistoryTable.append(row)
	}
}

function getCycleDeltaText(cycleDelta) {
    if (cycleDelta >= 0) {
        return "This wk"
    } else if (cycleDelta == 1) {
        return "Last wk"
    } else {
        return (cycleDelta * -1) + " wk ago"
    }
}

function getCapacitySpan(capacity, frequency) {
    var span = $("<span></span>")
    $('<span>' + capacity.total + '</span>').appendTo(span).prop('title', toLinkClassValueString(capacity))
    span.append('<span>(' + frequency + ')</span>')
    return span
}

function getCapacityDeltaSpan(capacityDelta) {
    var span = $("<span></span>")
    if (!capacityDelta.economy && !capacityDelta.business && !capacityDelta.first) {
        span.text('-')
    } else {
        span.append(getDeltaSpan(capacityDelta.total))
        span.prop('title', toLinkClassValueString(capacityDelta))
    }
    return span
}

function getDeltaSpan(delta) {
    var span = $('<span></span>')
    var displayValue
    if (delta < 0) {
        span.append('<img src="assets/images/icons/12px/arrow-270-red.png">')
        displayValue = delta * -1
    } else {
        span.append('<img src="assets/images/icons/12px/arrow-090.png">')
        displayValue = delta
    }
    span.append('<span>' + displayValue + '</span>')
    return span
}

function getPriceDeltaSpan(priceDelta) {
    var span = $("<span></span>")
    if (!priceDelta.economy && !priceDelta.business && !priceDelta.first) {
        span.text("-")
        return span
    }

    if (priceDelta.economy) {
        span.append(getDeltaSpan(priceDelta.economy))
    } else {
        span.append('<span>-</span>')
    }
    span.append("/")

    if (priceDelta.business) {
        span.append(getDeltaSpan(priceDelta.business))
    } else {
        span.append('<span>-</span>')
    }
    span.append("/")

    if (priceDelta.first) {
        span.append(getDeltaSpan(priceDelta.first))
    } else {
        span.append('<span>-</span>')
    }

    return span
}

function toggleTableSortOrder(sortHeader, updateTableFunction) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}

	sortHeader.siblings().removeClass("selected")
	sortHeader.addClass("selected")

	updateTableFunction(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
}


var linkFeatureIconsLookup = {
    "WIFI" : { "description" : "WIFI", "icon" : "assets/images/icons/wi-fi-zone.png"},
    "BEVERAGE_SERVICE" : { "description" : "Beverage and snack services", "icon" : "assets/images/icons/cup.png"},
    "HOT_MEAL_SERVICE" : { "description" : "Hot meal services", "icon" : "assets/images/icons/plate-cutlery.png"},
    "PREMIUM_DRINK_SERVICE" : { "description" : "Premium drink services", "icon" : "assets/images/icons/glass.png"},
    "IFE" : { "description" : "In-flight entertainment", "icon" : "assets/images/icons/media-player-phone-horizontal.png"},
    "GAME" : { "description" : "Video game system", "icon" : "assets/images/icons/controller.png"},
    "POSH" : { "description" : "Luxurious", "icon" : "assets/images/icons/diamond.png"},
    "POWER_OUTLET" : { "description" : "Power outlet", "icon" : "assets/images/icons/plug.png"},
    "SHUTTLE" : { "description" : "Free Shuttle", "icon" : "assets/images/icons/shuttle.png"}
}

function toggleSearchLinkDetails(containerDiv) {
    if (containerDiv.find(".linkDetails:visible").length > 0) {
        containerDiv.find(".linkDetails").hide()
    } else {
        containerDiv.find(".linkDetails").show()
    }
}

function getLinkFeatureIconsDiv(features, hasShuttle) {
    var featureIconsDiv = $("<div></div>")
    $.each(features, function(index, feature) {
        var featureInfo = linkFeatureIconsLookup[feature]
        var icon = $("<img src='" + featureInfo.icon + "' title='" + featureInfo.description + "' style='margin: 2px;'>")
         featureIconsDiv.append(icon)
    })

    if (hasShuttle) {
        var featureInfo = linkFeatureIconsLookup['SHUTTLE']
        featureIconsDiv.append($("<img src='" + featureInfo.icon + "' title='" + featureInfo.description + "' style='margin: 2px;'>"))
    }
    return featureIconsDiv
}

function getLinkReviewDiv(quality) {

    var color
    var text
    if (quality >= 80) {
        text = "Excellent flight"
        color = "darkgreen"
    } else if (quality >= 70) {
        text = "Very good flight"
        color = "darkgreen"
    } else if (quality >= 60) {
        text = "Good flight"
        color = "darkgreen"
    } else if (quality >= 50) {
        text = "Average flight"
        color = "gold"
    } else if (quality >= 40) {
        text = "Poor flight"
        color = "darkorange"
    } else if (quality >= 30) {
        text = "Terrible flight"
        color = "crimson"
    } else {
        text = "Abysmal flight"
        color = "crimson"
    }
    var text = text + " (" + (quality / 10) + "/10)"
    return $("<div style='color: "  + color + "'>" + text + "</div>")
}

function getAirlineTimeSlotText(minutes, startDay) {
    var dayOfWeek = Math.floor(minutes / (24 * 60))
    var minuteOfHour = minutes % 60
    var hourOfDay = Math.floor(minutes % (24 * 60) / 60)
    var hourText = hourOfDay < 10 ? "0" + hourOfDay : hourOfDay
    var minuteText = minuteOfHour < 10 ? "0" + minuteOfHour : minuteOfHour
    if (startDay < dayOfWeek) {
        return hourText + ":" + minuteText + "(+" + (dayOfWeek - startDay) + ")"
    } else {
        return hourText + ":" + minuteText
    }
}


function getAirportTextEntry(entry) {
    var text = "";
    if (entry.airportCity) {
        text += entry.airportCity + ", " + loadedCountriesByCode[entry.countryCode].name
    }

    if (entry.airportIata) {
        text += " (" + entry.airportIata + ")"
    }

    if (entry.airportName) {
        text += " " + entry.airportName
    }

    return text
}

function getAirportShortText(entry) {
    var text = "";
    if (entry.airportCity) {
        text += entry.airportCity
    }

    if (entry.airportIata) {
        text += " (" + entry.airportIata + ")"
    }
    return text
}

function highlightText(text, phrase) {
    var index = text.toLowerCase().indexOf(phrase.toLowerCase());
    if (index >= 0) {
        var prefix = text.substring(0, index)
        var highlight = "<b>" + text.substring(index, index + phrase.length) + "</b>"
        var suffix = text.substring(index + phrase.length)
        return prefix + highlight + suffix;
    }
    return text;
}

function resetSearchInput(button) {
    var disablingInputs = button.closest('.searchContainer').find('input')
    disablingInputs.val('')
    disablingInputs.removeData("selectedId")
}

function searchButtonKeyPress(event, button) {
    if (event.keyCode == 13) { //enter
        button.click()
    }
}


function searchKeyDown(event, input) {
    if (event.keyCode == 9) { //tab, has to do it here otherwise input field would lose focus
        confirmSelection(input)
    }
}

function searchChange(input) {
    search(event, input)
    input.removeData("selectedId")
}

function searchKeyUp(event, input) {
    var resultContainer = input.closest('div.searchInput').siblings('div.searchResult')
    if (event.keyCode == 38) {
        changeSelection(-1, resultContainer)
    } else if (event.keyCode == 40) {
        changeSelection(1, resultContainer)
    } else if (event.keyCode == 13) { //enter
        confirmSelection(input)
    }
}

function searchFocusOut(input) {
    if (!input.data("selectedId")) { //have not select anything, revert to empty
        input.val("")
    }

    var resultContainer = input.closest('div.searchInput').siblings('div.searchResult')
    resultContainer.hide()

}

function confirmSelection(input) {
    var resultContainer = input.closest('div.searchInput').siblings('div.searchResult')
    var searchType = input.closest('div.searchInput').data("searchType")
    var selected = resultContainer.find('div.selected').data(searchType)
    if (selected) {
        var displayVal
        var selectedId
        if (searchType === "airport") {
            displayVal = getAirportShortText(selected)
            selectedId = selected.airportId
        } else if (searchType === "country") {
            displayVal = getCountryTextEntry(selected)
            selectedId = selected.countryCode
        } else if (searchType === "zone") {
            displayVal = getZoneTextEntry(selected)
            selectedId = selected.zone
        } else if (searchType === "airline") {
            displayVal = getAirlineTextEntry(selected)
            selectedId = selected.airlineId
        } else if (searchType === "alliance") {
            displayVal = getAllianceTextEntry(selected)
            selectedId = selected.allianceId
        }

        input.val(displayVal)
        input.data("selectedId", selectedId).trigger('confirmSelection')
    }

    resultContainer.hide()
}

function clickSelection(selectionDiv) {
    selectionDiv.siblings("div.selected").removeClass("selected")
    selectionDiv.addClass("selected")
    var resultContainer = selectionDiv.closest(".searchResult")

    var input = resultContainer.siblings(".searchInput").find("input[type=text]")
    confirmSelection(input)
}

function changeSelection(indexChange, resultContainer) {
    var currentIndex = resultContainer.find("div.searchResultEntry.selected").index()
//    if (typeof resultContainer.data('selectedIndex') !== 'undefined') {
//        currentIndex = resultContainer.data("selectedIndex")
//    } else {
//        currentIndex = 0
//    }

    currentIndex += indexChange
    var currentEntries = resultContainer.find("div.searchResultEntry")
    if (currentIndex < 0) {
        currentIndex = 0
    } else if (currentIndex >= currentEntries.length) {
        currentIndex = currentEntries.length - 1
    }

    currentEntries.removeClass("selected")
    if (currentEntries[currentIndex]) {
        $(currentEntries[currentIndex]).addClass("selected")
    }
 //   resultContainer.data("selectedIndex", currentIndex)
}

function numberInputFocusOut(input) {
    if (!parseInt(input.val())) {
        input.val('')
    }
}

var currentSearchAjax

function search(event, input, retry) {
    var resultContainer = input.closest('div.searchInput').siblings('div.searchResult')
    var searchType = input.closest('div.searchInput').data('searchType')
    var phrase = input.val()
	var url = "search-" + searchType + "?input=" + phrase
    if (currentSearchAjax) {
        currentSearchAjax.abort()
    }

	currentSearchAjax = $.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    beforeSend: function () {
	        input.parent().find(".spinner").show(0)
	        searching = true
	    },
	    success: function(searchResult) {
	  //      input.prop('disabled', false);
	        //resultContainer.removeData("selectedIndex")
	        resultContainer.find("div.searchResultEntry, div.message").remove()
	        if (searchResult.message) {
	            resultContainer.append("<div class='message'>" + searchResult.message + "</div>")
	        }

	        if (searchResult.entries) {
                $.each(searchResult.entries, function(index, entry) {
                    var textEntry
                    if (searchType === "airport") {
                        textEntry = getAirportTextEntry(entry)
                    } else if (searchType === "country") {
                        textEntry = getCountryTextEntry(entry)
                    } else if (searchType === "zone") {
                        textEntry = getZoneTextEntry(entry)
                    } else if (searchType === "airline") {
                        textEntry = getAirlineTextEntry(entry)
                    } else if (searchType === "alliance") {
                        textEntry = getAllianceTextEntry(entry)
                    }


                    var text = highlightText(textEntry, phrase)
                    var searchResultDiv = $("<div class='searchResultEntry' onmousedown='clickSelection($(this))'>" + text + "</div>")
                    searchResultDiv.data(searchType, entry)
                    resultContainer.append(searchResultDiv)
                    if (index == 0) {
                        searchResultDiv.addClass("selected")
                    }
                })
            }
            resultContainer.show()
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    },
        complete:function() {
              //Hide the loader over here
              input.parent().find(".spinner").hide()
              currentSearchAjax = undefined
        }
	});
}


function researchFlight(fromAirportId, toAirportId) {
    if (fromAirportId && toAirportId) {
        var url = "research-link/" + fromAirportId + "/" + toAirportId

        $.ajax({
            type: 'GET',
            url: url,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(result) {
                var fromAirport = result.fromAirport
                var toAirport = result.toAirport
                var fromAirportId = fromAirport.id
                var toAirportId = toAirport.id
                loadAirportImage(fromAirportId, $('#researchSearchResult img.fromAirport') )
                loadAirportImage(toAirportId, $('#researchSearchResult img.toAirport'))
                $("#researchSearchResult .fromAirportText").text(result.fromAirportText)
                $("#researchSearchResult .fromAirport .population").text(commaSeparateNumber(result.fromAirport.population))
                $("#researchSearchResult .fromAirport .incomeLevel").text(result.fromAirport.incomeLevel)
                $("#researchSearchResult .toAirportText").text(result.toAirportText)
                $("#researchSearchResult .toAirport .population").text(commaSeparateNumber(result.toAirport.population))
                $("#researchSearchResult .toAirport .incomeLevel").text(result.toAirport.incomeLevel)

                $("#researchSearchResult .distance").text(result.distance)
                $("#researchSearchResult .flightType").text(result.flightType)
                $("#researchSearchResult .demand").text(toLinkClassValueString(result.directDemand))

                $("#researchSearchResult .table.links .table-row").remove()

                $.each(result.links, function(index, link) {
                    var $row = $("<div class='table-row'><div class='cell'>" + link.airlineName
                        + "</div><div class='cell'>" + toLinkClassValueString(link.price, "$")
                        + "</div><div class='cell'>" + toLinkClassValueString(link.capacity)
                        + "</div><div class='cell'>" + link.computedQuality
                        + "</div><div class='cell'>" + link.frequency + "</div></div>")
                    $('#researchSearchResult .table.links').append($row)
                })
                if (result.links.length == 0) {
                    var $row = $("<div class='table-row'><div class='cell'>-"
                                            + "</div><div class='cell'>-"
                                            + "</div><div class='cell'>-"
                                            + "</div><div class='cell'>-"
                                            + "</div><div class='cell'>-</div></div>")
                    $('#researchSearchResult .table.links').append($row)
                }

                assignAirlineColors(result.consumptions, "airlineId")
                plotPie(result.consumptions, null, $("#researchSearchResult .linksPie"), "airlineName", "soldSeats")

                $('#researchSearchResult').show()

                //plot consumptions
             },
             error: function(jqXHR, textStatus, errorThrown) {
                            console.log(JSON.stringify(jqXHR));
                            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
             },
             complete:function() {
                          //Hide the loader over here
                          input.parent().find(".spinner").hide()
                          currentSearchAjax = undefined
             },
             beforeSend: function() {
                 $('body .loadingSpinner').show()
             },
             complete: function(){
                 $('body .loadingSpinner').hide()
             }
        });
    }
}







function getCountryTextEntry(country) {
   return country.countryName + "(" + country.countryCode + ")"
}

function getZoneTextEntry(zone) {
    return zone.zoneName + "(" + zone.zone + ")"
}

function getAirlineTextEntry(airline) {
    return airline.airlineName + "(" + airline.airlineCode + ")"
}

function getAllianceTextEntry(alliance) {
    return alliance.allianceName
}

function positionTitles(titlesContainer) {
    titlesContainer.show();
    var titleSelections = titlesContainer.children('div.titleSelection')
    titleSelections.addClass('clickable')

    var selectedDiv = titlesContainer.children('div.titleSelection.selected')

    var selectedIndex = titleSelections.index(selectedDiv)

    var divWidths = []

    $.each(titleSelections, function(index, titleSelection) {
        divWidths[index] = $(titleSelection).width()
    })

    var margin = 20

    $.each(titleSelections, function(index, titleSelection) {
        var offset = 0
        if (selectedIndex < index) { //shift right
            offset = divWidths[selectedIndex] / 2 + margin

            for (i = selectedIndex + 1; i < index ; i ++) {
                offset += divWidths[i]
                offset += margin
            }

            offset += divWidths[index] / 2

        } else if (selectedIndex > index) { //shift left
            offset -= divWidths[selectedIndex] / 2 + margin
            for (i = selectedIndex -1; i > index ; i --) {
                offset -= divWidths[i]
                offset -= margin
            }
            offset -= divWidths[index] / 2
        }

        //$(titleSelection).css({ "position": "absolute", "left" : "50%", "transform" : "translate(-50%, 0%) translate(" + (index - selectedIndex) * 150 + "px, 0)" })
        $(titleSelection).css({ "position": "absolute", "left" : "50%", "bottom": "0", "transform" : "translate(-50%, 0%) translate(" + offset + "px, 0)" })
    })

    titleSelections.off("click.select");
    titleSelections.on("click.select", function(){
      $(this).siblings().removeClass('selected')
      $(this).addClass('selected')
      positionTitles(titlesContainer)
      updateNavigationArrows(titlesContainer, true)
    });
}

function titleNavigate(arrow, indexChange) {
    var titlesContainer = arrow.closest('.titlesContainer')
    var selectedDiv = titlesContainer.find('.titleSelection.selected')
    var selectedIndex = titlesContainer.find('.titleSelection').index(selectedDiv)
    var newIndex = selectedIndex + indexChange

    var newSelectedDiv = titlesContainer.find('.titleSelection')[newIndex]
    $(newSelectedDiv).trigger('click')
}

function updateNavigationArrows($titlesContainer, animated) {
    var $selectedDiv = $titlesContainer.find('.titleSelection.selected')
    var selectedIndex = $titlesContainer.find('.titleSelection').index($selectedDiv)
    var selectionLength = $titlesContainer.find('.titleSelection').length


    if (selectionLength <= 1) {
        $titlesContainer.find('div.left').hide()
        $titlesContainer.find('div.right').hide()
    } else {
        var duration = updateNavigationArrows ? 500 : 0

        if (selectedIndex <= 0) {
            $titlesContainer.find('div.left').fadeOut(duration)
        } else {
            $titlesContainer.find('div.left').fadeIn(duration)
        }

        if (selectedIndex >= selectionLength - 1) {
            $titlesContainer.find('div.right').fadeOut(duration)
        } else {
            $titlesContainer.find('div.right').fadeIn(duration)
        }
    }
}



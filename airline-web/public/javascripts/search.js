var loadedOlympicsEvents = []
var loadedAlerts = []


function showSearchCanvas() {
	setActiveDiv($("#searchCanvas"))
	highlightTab($('.searchCanvasTab'))
	$("#routeSearchResult").empty()
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
                    var itineraryDiv = $("<div class='section itinerary clickable' onclick='toggleSearchLinkDetails($(this))'></div>")
                    var total = 0

                    var routeDiv = $("<div style='float:left; width : 85%'></div>")
                    itineraryDiv.append(routeDiv)

                     //Generate Summary
                    var startLink = entry.route[0]
                    var endLink = entry.route[entry.route.length - 1]
                    var startDay = Math.floor(startLink.departure / (24 * 60))
                    var summaryDiv = $("<div class='summary'  style='display: flex; align-items: center;'></div>")
                    summaryDiv.append("<div style='width: 50%; float:left;'> " + getAirlineTimeSlotText(startLink.departure, startDay) + " - " + getAirlineTimeSlotText(endLink.arrival, startDay) + "</div>")
                    summaryDiv.append("<div style='width: 50%; float:left;'> " + getDurationText(endLink.arrival - startLink.departure) +  "</div>")
                    summaryDiv.append("<div style='clear:both; '></div>")

                    routeDiv.append(summaryDiv)

                    var previousLink
                    $.each(entry.route, function(index, link) {
                        var linkDiv = $("<div style='margin-bottom: 10px;'></div>")
                        var linkSummaryDiv = $("<div style='margin : 10px 0;'></div>")
                        linkSummaryDiv.append("<div style='width: 50%; float:left; display: flex; align-items: center;'> " + getAirlineLogoImg(link.airlineId) + "<span class='summary'>" + link.airlineName + "</span></div>")
                        var linkDurationText = getDurationText(link.arrival - link.departure)
                        if (index > 0) {
                            linkDurationText += " (+" + getDurationText(link.departure - previousLink.arrival) + " layover at " + link.fromAirportIata + ")"
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
                        linkDetailLeftDiv.append(getLinkFeatureIconsDiv(link.features))
                        linkDetailLeftDiv.append(getLinkReviewDiv(link.computedQuality))


                        var linkDetailRightDiv = $("<div style='width: 50%;'></div>").appendTo(linkDetailDiv)
                        linkDetailRightDiv.append("<div style='display: flex; align-items: center;'>" + getAirportText(link.fromAirportCity, link.fromAirportIata) + "<img src='assets/images/icons/arrow.png' style='margin: 0 5px;'>" + getAirportText(link.toAirportCity, link.toAirportIata) + "</div>")
                        linkDetailRightDiv.append("<div>Aircraft : " + (link.airplaneModelName ? link.airplaneModelName : "-") + "</div>")
                        if (link.operatorAirlineId) { //code share
                            linkDetailRightDiv.append("<div>Operated by " + getAirlineLogoImg(link.operatorAirlineId) + link.operatorAirlineName + "</div>")
                        }


                        linkDetailDiv.append("<div style='clear:both; '></div>")

                        linkDetailDiv.hide()
                        linkDiv.append(linkDetailDiv)

//                        var directionDiv = $("<div style='display: flex; align-items: center;' >" + link.flightCode + "&nbsp;" + getAirportText(link.fromAirportCity, link.fromAirportIata) + "<img src='assets/images/icons/arrow.png' style='margin: 0 5px;'>" + getDurationText(link.duration) + " " + link.linkClass + "</div>")
//                        linkDiv.append(directionDiv)

                        routeDiv.append(linkDiv)
                        total += link.price
                        previousLink = link
                    })
                    var stopDescription
                    if (entry.route.length == 1) {
                        stopDescription = "Direct Flight"
                    } else if (entry.route.length == 2) {
                        stopDescription = "1 Stop"
                    } else {
                        stopDescription = (entry.route.length - 1) + " Stops"
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

var linkFeatureIconsLookup = {
    "WIFI" : { "description" : "WIFI", "icon" : "assets/images/icons/wi-fi-zone.png"},
    "BEVERAGE_SERVICE" : { "description" : "Beverage and snack services", "icon" : "assets/images/icons/cup.png"},
    "HOT_MEAL_SERVICE" : { "description" : "Hot meal services", "icon" : "assets/images/icons/plate-cutlery.png"},
    "PREMIUM_DRINK_SERVICE" : { "description" : "Premium drink services", "icon" : "assets/images/icons/glass.png"},
    "IFE" : { "description" : "In-flight entertainment", "icon" : "assets/images/icons/media-player-phone-horizontal.png"},
    "GAME" : { "description" : "Video game system", "icon" : "assets/images/icons/controller.png"},
    "POSH" : { "description" : "Luxurious", "icon" : "assets/images/icons/diamond.png"},
    "POWER_OUTLET" : { "description" : "Power outlet", "icon" : "assets/images/icons/plug.png"}
}

function toggleSearchLinkDetails(containerDiv) {
    if (containerDiv.find(".linkDetails:visible").length > 0) {
        containerDiv.find(".linkDetails").hide()
    } else {
        containerDiv.find(".linkDetails").show()
    }
}

function getLinkFeatureIconsDiv(features) {
    var featureIconsDiv = $("<div></div>")
    $.each(features, function(index, feature) {
        var featureInfo = linkFeatureIconsLookup[feature]
        var icon = $("<img src='" + featureInfo.icon + "' title='" + featureInfo.description + "' style='margin: 2px;'>")
         featureIconsDiv.append(icon)
    })
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

function airportSearchButtonKeyPress(event, button) {
    if (event.keyCode == 13) { //enter
        button.click()
    }
}


function airportSearchKeyDown(event, input, resultContainer) {
    if (event.keyCode == 9) { //tab, has to do it here otherwise input field would lose focus
        confirmAirportSelection(input, resultContainer)
    }
}


function airportSearchKeyUp(event, input, resultContainer) {
    if (event.keyCode == 38) {
        changeAirportSelection(-1, resultContainer)
    } else if (event.keyCode == 40) {
        changeAirportSelection(1, resultContainer)
    } else if (event.keyCode == 13) { //enter
        confirmAirportSelection(input, resultContainer)
    } else {
        searchAirport(event, input, resultContainer)
        input.removeData("selectedAirportId")
    }
}

function airportSearchFocusOut(input, resultContainer) {
    if (!input.data("selectedAirportId")) { //have not select anything, revert to empty
        input.val("")
    }

    setTimeout(function() { resultContainer.hide() }, 100) //short delay here otherwise the div disappear below the click event is registered

}

function confirmAirportSelection(input, resultContainer) {
    var selectedAirport = resultContainer.find('div.selected').data("airport")
    if (selectedAirport) {
        input.val(getAirportShortText(selectedAirport))
        input.data("selectedAirportId", selectedAirport.airportId)
    }

    resultContainer.hide()
}

function clickAirportSelection(airportSelectionDiv) {
    airportSelectionDiv.siblings("div.selected").removeClass("selected")
    airportSelectionDiv.addClass("selected")
    $('#searchCanvas input.toAirport'), $('#toAirportResult')

    var resultContainer = airportSelectionDiv.closest(".airportSearchResult")
    var input = resultContainer.siblings(".airportSearchInput").find("input[type=text]")
    confirmAirportSelection(input, resultContainer)
}

function changeAirportSelection(indexChange, resultContainer) {
    var currentIndex = resultContainer.find("div.airportEntry.selected").index()
//    if (typeof resultContainer.data('selectedIndex') !== 'undefined') {
//        currentIndex = resultContainer.data("selectedIndex")
//    } else {
//        currentIndex = 0
//    }

    currentIndex += indexChange
    var currentEntries = resultContainer.find("div.airportEntry")
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

function searchAirport(event, input, resultContainer) {
    var phrase = input.val()
	var url = "search-airport?input=" + phrase

	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    async: false,
	    dataType: 'json',
	    success: function(searchResult) {
	        resultContainer.find("div.airportEntry, div.message").remove()
	        //resultContainer.removeData("selectedIndex")
	        if (searchResult.message) {
	            resultContainer.append("<div class='message'>" + searchResult.message + "</div>")
	        }

	        if (searchResult.airports) {
                $.each(searchResult.airports, function(index, entry) {
                    var airportText = highlightText(getAirportTextEntry(entry), phrase)
                    var airportDiv = $("<div class='airportEntry' onclick='clickAirportSelection($(this))'>" + airportText + "</div>")
                    airportDiv.data("airport", entry)
                    resultContainer.append(airportDiv)
                    if (index == 0) {
                        airportDiv.addClass("selected")
                    }
                })
            }
            resultContainer.show()
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
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

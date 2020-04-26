var loadedOlympicsEvents = []
var loadedAlerts = []


function showSearchCanvas() {
	setActiveDiv($("#searchCanvas"))
	highlightTab($('.searchCanvasTab'))
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
                $("#searchResult").empty()
    //	    	loadedOlympicsEvents = olympicsEvents
    //	    	var selectedSortHeader = $('#olympicsTableSortHeader .table-header .cell.selected')
    //	    	updateOlympicTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
                $.each(searchResult, function(index, entry) {
                    var itineraryDiv = $("<div class='section'></div>")
                    var total = 0

                    var routeDiv = $("<div style='float:left; width : 500px'></div>")
                    itineraryDiv.append(routeDiv)
                    $.each(entry.route, function(index, link) {
                        var linkDiv = $("<div style='margin-bottom: 10px;'>" + link.flightCode + "&nbsp;" + getCountryFlagImg(link.fromAirportCountryCode) + getAirportText(link.fromAirportCity, link.fromAirportIata) + " => " +  getCountryFlagImg(link.toAirportCountryCode) + getAirportText(link.toAirportCity, link.toAirportIata) + " $" + link.price + " " + link.linkClass + "</div>")
                        var airlineSpan = $("<div></div>")
                        airlineSpan.append(getAirlineLogoImg(link.airlineId) + link.airlineName)
                        linkDiv.prepend(airlineSpan)
                        routeDiv.append(linkDiv)
                        total += link.price
                    })
                    var stopDescription
                    if (entry.route.length == 1) {
                        stopDescription = "Direct Flight"
                    } else if (entry.route.length == 2) {
                        stopDescription = "1 Stop"
                    } else {
                        stopDescription = (entry.route.length - 1) + " Stops"
                    }
                    var priceDiv = $("<div style='float: left'><div class='ticketTitle'>$ " + total + "</div></div>")
                    priceDiv.append($("<div>" + stopDescription + "</div>"))

                    itineraryDiv.append(priceDiv)
                    itineraryDiv.append("<div style='clear:both;'></div>")
                    $("#searchResult").append(itineraryDiv)

                })
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
    }
}


function airportSearchKeyPress(event, input, resultContainer) {
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

function confirmAirportSelection(input, resultContainer) {
    var selectedAirport = resultContainer.find('div.selected').data("airport")
    if (selectedAirport) {
        input.val(getAirportShortText(selectedAirport))
        input.data("selectedAirportId", selectedAirport.airportId)
    }

    resultContainer.hide()
}

function changeAirportSelection(indexChange, resultContainer) {
    var currentIndex
    if (typeof resultContainer.data('selectedIndex') !== 'undefined') {
        currentIndex = resultContainer.data("selectedIndex")
    } else {
        currentIndex = -1
    }

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

    resultContainer.data("selectedIndex", currentIndex)
}

function searchAirport(event, input, resultContainer) {
    var phrase = input.val()
	var url = "search-airport?input=" + phrase

	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(searchResult) {
	        resultContainer.find("div.airportEntry, div.message").remove()
	        resultContainer.removeData("selectedIndex")
	        if (searchResult.message) {
	            resultContainer.append("<div class='message'>" + searchResult.message + "</div>")
	        }

	        if (searchResult.airports) {
                $.each(searchResult.airports, function(index, entry) {
                    var airportText = highlightText(getAirportTextEntry(entry), phrase)
                    var airportDiv = $("<div class='airportEntry'>" + airportText + "</div>")
                    airportDiv.data("airport", entry)
                    resultContainer.append(airportDiv)
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

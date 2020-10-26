var loadedOlympicsEvents = []
var loadedAlerts = []


function showHistoryCanvas() {
	setActiveDiv($("#historyCanvas"))
	$("#historyCanvas").css("display", "flex")
	highlightTab($('.historyCanvasTab'))
	$("#linkHistorySearchResult").empty()
}

function searchLinkHistory() {
    var url = "search-link-history"

    var fromAirportId = $('#historyCanvas input.fromAirport').data('selectedId')
    var toAirportId = $('#historyCanvas input.toAirport').data('selectedId')
    var fromCountryCode = $('#historyCanvas input.fromCountryCode').data('selectedId')
    var toCountryCode = $('#historyCanvas input.toCountryCode').data('selectedId')

    var searchData = {}

    if (fromAirportId) {
        searchData["fromAirportId"] = parseInt(fromAirportId)
    }
    if (toAirportId) {
        searchData["toAirportId"] = parseInt(toAirportId)
    }

    $.ajax({
        type: 'POST',
        url: url,
        contentType: 'application/json; charset=utf-8',
        data: JSON.stringify(searchData),
        dataType: 'json',
        success: function(searchResult) {
            $("#linkHistorySearchResult").empty()
            $.each(searchResult, function(index, entry) {
                 $("#linkHistorySearchResult").append("<div>" +  entry.linkId + "&nbsp;"
                  + getCountryFlagImg(entry.fromCountryCode) + getAirportText(entry.fromAirportIata, entry.fromAirportCity) + "&nbsp;"
                  + getCountryFlagImg(entry.toCountryCode) + getAirportText(entry.toAirportIata, entry.toAirportCity) + "&nbsp;"
                  +  toLinkClassValueString(entry.capacity) + "&nbsp;+-" +  toLinkClassValueString(entry.capacityDelta) + "&nbsp;" + toLinkClassValueString(entry.price, '$') + "&nbsp;+-" +  toLinkClassValueString(entry.priceDelta, '$') + "</div>")
            })
            if (searchResult.length == 0) {
                $("#linkHistorySearchResult").append("<div class='ticketTitle'>No match found</div>")
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

var loadedOlympicsEvents = []
var loadedAlerts = []


function showHistoryCanvas() {
	setActiveDiv($("#historyCanvas"))
	$("#historyCanvas").css("display", "flex")
	highlightTab($('.historyCanvasTab'))
	$("#linkHistorySearchResult").empty()
}

function searchLinkHistory(fromAirportId, toAirportId) {
    if (fromAirportId && toAirportId) {
        var url = "search-link-history"

        var searchData = {
        			"fromAirportId" : parseInt(fromAirportId),
        			"toAirportId" : parseInt(toAirportId),
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
                     $("#linkHistorySearchResult").append("<div>" +  entry.linkId + "</div>")
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
}


function resetAllianceLabelColor(allianceId, callback, airlineOverride) {
    var url = "airlines/" + activeAirline.id + "/" + (airlineOverride ? "delete-alliance-label-color-as-airline" : "delete-alliance-label-color-as-alliance") + "?targetAllianceId=" + allianceId
     $.ajax({
            type: 'GET',
            url: url,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(result) {
                updateAirlineLabelColors(callback)
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
    });

}

function setAllianceLabelColor(allianceId, color, callback, airlineOverride) {
    var url = "airlines/" + activeAirline.id + "/" + (airlineOverride ? "set-alliance-label-color-as-airline" : "set-alliance-label-color-as-alliance") + "?targetAllianceId=" + allianceId + "&color=" + color.substring(1)
    $.ajax({
            type: 'GET',
            url: url,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(result) {
                updateAirlineLabelColors(callback)
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
    });
}
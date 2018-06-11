//<div id="hideLinkHistoryButton" class="googleMapButton button" onclick="hideLinkHistoryView()" style="display: none;" align="center"><span class="alignHelper"></span><img src='@routes.Assets.versioned("images/icons/arrow-return.png")' style="vertical-align: middle; padding-right: 5px"/>Exit route history view</div>
//<div id="hideAirportLinksButton" class="googleMapButton button" onclick="hideAirportLinksView()" style="display: none;" align="center"><span class="alignHelper"></span><img src='@routes.Assets.versioned("images/icons/arrow-return.png")' style="vertical-align: middle; padding-right: 5px"/>Exit airport flight map</div>

function createMapButton(map, text, onclickAction, id) {
    // Set CSS for the control border.
    var div = jQuery('<div id="' + id + '" class="googleMapButton button" onclick="' + onclickAction + '" style="align="center"><span class="alignHelper"></span><img src="assets/images/icons/arrow-return.png" style="vertical-align: middle; padding-right: 5px"/>' + text + '</div>')
    return div
}
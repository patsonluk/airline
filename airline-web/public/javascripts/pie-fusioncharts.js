function plotAirportShares(airportShares, currentAirportId, container) {
	var data = []
	$.each(airportShares, function(key, airportShare) {
		var entry = {
			label : airportShare.airportName,
			value : airportShare.share
		}
//		if (currentAirportId == airportShare.airportId) {
//			entry["sliced"] = true
//			entry["selected"] = true
//		}
		data.push(entry)
	})
	$("#cityPie").insertFusionCharts({
		type: 'pie2d',
	    width: '100%',
	    height: '195',
	    dataFormat: 'json',
		dataSource: {
	    	"chart": {
	    		"animation": "0",
	    		"pieRadius": "70",
	    		"showBorder":"0",
                "use3DLighting": "1",
                "showPercentInTooltip": "1",
                "decimals": "2",
                "toolTipBorderRadius": "2",
                "toolTipPadding": "5",
                "showHoverEffect":"1",
                "bgAlpha":"0",
                "showLabels":"0",
                "showValues":"0"
	    	},
			"data" : data
	    }
	})
	
}
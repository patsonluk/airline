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

function plotLinkProfit(linkConsumptions, container) {
	var data = []
	var category = []
	 
	var profitByMonth = {}
	$.each(linkConsumptions, function(index, linkConsumption) {
		//group in months first
		var month = Math.floor(linkConsumption.cycle / 4)
		if (!profitByMonth[month]) {
			profitByMonth[month] = linkConsumption.profit
		} else {
			profitByMonth[month] += linkConsumption.profit
		}
	})
	
	var maxMonth = 6
	var monthCounter = 0
	$.each(profitByMonth, function(key, profit) {
		if (monthCounter < maxMonth) {
			data.push({ value : profit })
			category.push({ label : key })
		} 
		monthCounter ++
	})
	
	var chart = container.insertFusionCharts({
		type: 'mscombi2d',
	    width: '100%',
	    height: '195',
	    dataFormat: 'json',
		dataSource: {
	    	"chart": {
	    		"xAxisname": "Month",
	    		"yAxisName": "Monthly Profit",
	    		"numberPrefix": "$",
	    		"useroundedges": "1",
	    		"animation": "0",
	    		"showBorder":"0",
                "toolTipBorderRadius": "2",
                "toolTipPadding": "5",
                "bgAlpha":"0",
                "showValues":"0"
	    	},
	    	"categories" : [{ "category" : category}],
			"dataset" : [ {"data" : data}, {"renderas" : "Line", "data" : data} ]
	    	            
	    }
	})
}


function plotPie(dataSource, currentKey, container, keyName, valueName) {
	var data = []
	$.each(dataSource, function(key, dataEntry) {
		var entry = {
			label : dataEntry[keyName],
			value : dataEntry[valueName]
		}
		if (currentKey && dataEntry[keyName] == currentKey) {
			entry.issliced = "1"
		}
		
//		if (currentAirportId == airportShare.airportId) {
//			entry["sliced"] = true
//			entry["selected"] = true
//		}
		data.push(entry)
	})
	var ref = container.insertFusionCharts({
		type: 'pie2d',
	    width: '100%',
	    height: '150',
	    dataFormat: 'json',
		dataSource: {
	    	"chart": {
	    		"animation": "0",
	    		"pieRadius": "65",
	    		"showBorder":"0",
                "use3DLighting": "1",
                "showPercentInTooltip": "1",
                "decimals": "2",
                "toolTipBorderRadius": "2",
                "toolTipPadding": "5",
                "showHoverEffect":"1",
                "bgAlpha":"0",
                "canvasBgAlpha" : "0",
                "showLabels":"0",
                "showValues":"0",
                "plottooltext": "$label - Passengers : $datavalue ($percentValue)"
	    	},
			"data" : data
	    }
	})
}


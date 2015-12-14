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
	var monthOrder = []
	$.each(linkConsumptions, function(index, linkConsumption) {
		//group in months first
		var month = Math.floor(linkConsumption.cycle / 4)
		if (profitByMonth[month] === undefined) {
			profitByMonth[month] = linkConsumption.profit
			monthOrder.push(month)
		} else {
			profitByMonth[month] += linkConsumption.profit
		}
	})
	
	var maxMonth = 6
	monthOrder = monthOrder.slice(0, maxMonth)
	$.each(monthOrder.reverse(), function(key, month) {
		data.push({ value : profitByMonth[month] })
		category.push({ label : month.toString() })
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

function plotLinkRidership(linkConsumptions, container) {
	var capacityData = []
	var soldSeatsData = []
	var loadFactorData = []
	
	var category = []
	 
	var maxWeek = 24
	
	linkConsumptions = $(linkConsumptions).toArray().slice(0, maxWeek)
	$.each(linkConsumptions.reverse(), function(key, linkConsumption) {
		capacityData.push({ value : linkConsumption.capacity })
		soldSeatsData.push({ value : linkConsumption.soldSeats })
		loadFactorData.push({ value : (linkConsumption.soldSeats / linkConsumption.capacity * 100).toFixed(2) })
		var month = Math.floor(linkConsumption.cycle / 4)
		//var week = linkConsumption.cycle % 4 + 1
		category.push({ label : month.toString()})
	})
	
	var chart = container.insertFusionCharts({
		type: 'mscombi2d',
	    width: '100%',
	    height: '195',
	    dataFormat: 'json',
		dataSource: {
	    	"chart": {
	    		"xAxisname": "Month",
	    		"YAxisName": "Seats",
	    		//"sYAxisName": "Load Factor %",
	    		"sNumberSuffix" : "%",
	            "sYAxisMaxValue" : "100",
	    		"useroundedges": "1",
	    		"animation": "0",
	    		"showBorder":"0",
                "toolTipBorderRadius": "2",
                "toolTipPadding": "5",
                "plotBorderAlpha": "10",
                "bgAlpha":"0",
                "showValues":"0",
                "canvasPadding":"0",
                "labelDisplay":"wrap",
                "labelStep": "4"
	    	},
	    	"categories" : [{ "category" : category}],
			"dataset" : [ 
			              { "seriesName": "Capacity", "renderAs" : "line", "data" : capacityData}
			            , {"seriesName": "Sold Seats","renderAs" : "area", "data" : soldSeatsData}
			            //, {"seriesName": "Load Factor", "renderAs" : "line", "parentYAxis": "S", "data" : loadFactorData} 
			            ]
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


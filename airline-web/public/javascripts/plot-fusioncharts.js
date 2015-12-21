

function plotSeatConfigurationGauge(container, configuration, maxSeats) {
	var dataSource = { 
		"chart": {
	    	"theme": "fint",
	        "lowerLimit": "0",
	        "upperLimit": "100",
	        "showTickMarks": "0",
	        "showTickValues": "0",
	        "showborder": "0",
	        "showtooltip": "0",
	        "chartBottomMargin": "20",
	        "bgAlpha":"0",
	        "valueFontSize": "11",  
	        "valueFontBold": "0",
	        "animation": "0",
	        "editMode": "1",
	        "baseFontColor": "#FFFFFF"
	    },
	    "pointers": {
	        //Multiple pointers defined here
	        "pointer": [
	            {
	                "bgColor": "#FFE62B",
	                "bgAlpha": "50",
	                "showValue": "0",
	                "sides" : "4",
	                "borderColor": "#FFE62B",
	                "borderAlpha": "20",
	            },
	            {
	                "bgColor": "#0077CC",
	                "bgAlpha": "50",
	                "showValue": "0",
	                "sides" : "3",
	                "borderColor": "#0077CC",
	                "borderAlpha": "20",
	            }
	        ]
	    }
	}
	
	function updateDataSource(configuration) {
		var businessPosition = configuration.economy / maxSeats * 100
		var firstPosition = (maxSeats - configuration.first * 8) / maxSeats * 100
	
		dataSource["colorRange"] = {
            "color": [
                      {
                          "minValue": "0",
                          "maxValue": businessPosition,
                          "label": "Y : " + configuration.economy,
                          "tooltext": "Business Class",
                          "code": "#6baa01"
                      },
                      {
                          "minValue": businessPosition,
                          "maxValue": firstPosition,
                          "label": "J : " + configuration.business,
                          "tooltext": "Business Class",
                          "code": "#0077CC"
                      },
                      {
                          "minValue": firstPosition,
                          "maxValue": "100",
                          "label": "F : " + configuration.first,
                          "tooltext": "Business Class",
                          "code": "#FFE62B"
                      }
                  ]
              }
	    dataSource["pointers"]["pointer"][0].value = firstPosition
	    dataSource["pointers"]["pointer"][1].value = businessPosition
	}
	
	updateDataSource(configuration)
	
	var chart = container.insertFusionCharts(
	{	
		type: 'hlineargauge',
        width: '300',
        height: '50',
        dataFormat: 'json',
	    dataSource: dataSource,
        "events": {
            //Event is raised when a real-time gauge or chart completes updating data.
            //Where we can get the updated data and display the same.
            "realTimeUpdateComplete" : function (evt, arg){
                var firstPosition = evt.sender.getData(1)
                var businessPosition = evt.sender.getData(2)
                
                if (firstPosition < businessPosition) {
                	businessPosition = firstPosition
                }
                
                configuration["first"] = Math.floor(maxSeats * (100 - firstPosition) / 100 / 8)
                
                configuration["business"] = Math.floor((maxSeats * (100 - businessPosition) / 100 - configuration["first"] * 8) / 4)
                configuration["economy"] = Math.floor(maxSeats * businessPosition / 100)
                
                console.log(configuration)
                
                updateDataSource(configuration)
                
                container.updateFusionCharts({
                	"dataSource": dataSource
                });
            }
        }
	})
}

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


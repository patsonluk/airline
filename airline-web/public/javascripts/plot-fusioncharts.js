function plotMaintenanceLevelGauge(container, maintenanceLevelInput, onchangeFunction) {
	container.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))
	
	var dataSource = { 
			"chart": {
		    	"theme": "fint",
		        "lowerLimit": "0",
		        "upperLimit": "100",
		        "showTickMarks": "0",
		        "showTickValues": "0",
		        "showborder": "0",
		        "showtooltip": "0",
		        "chartBottomMargin": "0",
		        "bgAlpha":"0",
		        "containerBackgroundOpacity" :'0',
		        "valueFontSize": "11",  
		        "valueFontBold": "0",
		        "animation": "0",
		        "editMode": "1",
		        "decimals": "0",
		        "baseFontColor": "#FFFFFF"
		    },
		    "pointers": {
		        //Multiple pointers defined here
		        "pointer": [
		            {
		                "bgColor": "#FFE62B",
		                "bgAlpha": "50",
		                "showValue": "0",
		                "sides" : "3",
		                "borderColor": "#FFE62B",
		                "borderAlpha": "20",
		                "value" : maintenanceLevelInput.val()
		            }
		        ]
		    },
		    "colorRange" : {
		    	"color": [
                      {
                    	  "minValue": "0",
                          "maxValue": "100",
                          "label": maintenanceLevelInput.val() + "%",
                          "code": "#6baa01"
                      }]
		    }
		}
	var chart = container.insertFusionCharts(
			{	
				type: 'hlineargauge',
				width: '100%',
		        height: '40px',
		        dataFormat: 'json',
			    dataSource: dataSource,
				events: {
		            //Event is raised when a real-time gauge or chart completes updating data.
		            //Where we can get the updated data and display the same.
		            "realTimeUpdateComplete" : function (evt, arg){
		                var newLevel = evt.sender.getData(1)
		                //take the floor
		                newLevel = Math.floor(newLevel)
		                dataSource["pointers"]["pointer"][0].value = newLevel
		                dataSource["colorRange"]["color"][0].label = newLevel + "%"
		                maintenanceLevelInput.val(newLevel)
		                container.updateFusionCharts({
		                	"dataSource": dataSource
		                });
		                onchangeFunction(newLevel)
		            }
		        }
			})
	
}

//unmodifiable seat configuration bar
function plotSeatConfigurationBar(container, configuration, maxSeats, spaceMultipliers, hideValues, height) {
    container.children(':FusionCharts').each((function(i) {
          $(this)[0].dispose();
    }))
    container.empty()

    var dataSource = {
        "chart": {
            "theme": "fint",
            "lowerLimit": "0",
            "upperLimit": "100",
            "showTickMarks": "0",
            "showTickValues": "0",
            "showborder": "0",
            "chartBottomMargin": "0",
            "bgAlpha":"0",
            "valueFontSize": "11",
            "valueFontBold": "0",
            "animation": "0",
            "editMode": "0",
            "containerBackgroundOpacity" :'0',
            "pointerBgAlpha":"0",
            "pointerBorderAlpha":"0",
            "chartLeftMargin": "0",
            "chartTopMargin": "0",
            "chartRightMargin": "0",
            "chartBottomMargin": "0",
            "baseFontColor": "#FFFFFF"
        }
    }


    var businessPosition = configuration.economy / maxSeats * 100
    var firstPosition = (maxSeats - configuration.first * spaceMultipliers.first) / maxSeats * 100

    var economyRange = {
                         "minValue": "0",
                         "maxValue": businessPosition,
                         "code": "#6baa01"
                       }
    var businessRange = {
                          "minValue": businessPosition,
                          "maxValue": firstPosition,
                          "code": "#0077CC"
                         }
    var firstRange = {
                      "minValue": firstPosition,
                      "maxValue": "100",
                      "code": "#FFE62B"
                      }
    if (!hideValues) {
        economyRange.label = "Y : " + configuration.economy
        businessRange.label = "J : " + configuration.business
        firstRange.label = "F : " + configuration.first
    }

    dataSource["colorRange"] = { "color": [economyRange, businessRange, firstRange] }

    if (!height) {
        height = "20px"
    }

    var chart = container.insertFusionCharts(
    {
        type: 'hlineargauge',
        width: '100%',
        height: height,
        dataFormat: 'json',
        dataSource: dataSource,
    })

}

function plotSeatConfigurationGauge(container, configuration, maxSeats, spaceMultipliers, callback) {
	container.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))
	
	container.empty()

	var chartConfig = {
                      	    	"theme": "fint",
                      	        "lowerLimit": "0",
                      	        "upperLimit": "100",
                      	        "showTickMarks": "0",
                      	        "showTickValues": "0",
                      	        "showborder": "0",
                      	        "showtooltip": "0",
                      	        "chartBottomMargin": "0",
                      	        "bgAlpha":"0",
                      	        "valueFontSize": "11",
                      	        "valueFontBold": "0",
                      	        "animation": "0",
                      	        "editMode": "0",
                      	        "pointerBgAlpha":"0",
                                  "pointerBorderAlpha":"0",
                      	        containerBackgroundOpacity :'0',
                      	        "baseFontColor": "#FFFFFF"
                      	    }


	var dataSource = { 
		"chart": chartConfig
//	    ,
//	    "pointers": {
//	        //Multiple pointers defined here
//	        "pointer": [
//	            {
//	                "bgColor": "#FFE62B",
//	                "bgAlpha": "50",
//	                "showValue": "0",
//	                //"sides" : "4",
//	                "borderColor": "#FFE62B",
//	                "borderAlpha": "20",
//	            },
//	            {
//	                "bgColor": "#0077CC",
//	                "bgAlpha": "50",
//	                "showValue": "0",
//	                //"sides" : "3",
//	                "borderColor": "#0077CC",
//	                "borderAlpha": "20",
//	            }
//	        ]
//	    }
	}
	
	function updateDataSource(configuration) {
		var businessPosition = configuration.economy / maxSeats * 100
		var firstPosition
		 if (configuration.business == 0) {
		    firstPosition = businessPosition
		 } else {
		    firstPosition = (maxSeats - configuration.first * spaceMultipliers.first) / maxSeats * 100
		 }
		dataSource["colorRange"] = {
            "color": [
                      {
                          "minValue": "0",
                          "maxValue": businessPosition,
                          "label": "Y : " + configuration.economy,
                          "tooltext": "Economy Class",
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
                          "tooltext": "First Class",
                          "code": "#FFE62B"
                      }
                  ]
              }
//	    dataSource["pointers"]["pointer"][0].value = firstPosition
//	    dataSource["pointers"]["pointer"][1].value = businessPosition
	}
	
	updateDataSource(configuration)

	var chart = container.insertFusionCharts(
	{	
		type: 'hlineargauge',
        width: '100%',
        height: '40px',
        dataFormat: 'json',
        containerBackgroundOpacity :'0',
	    dataSource: dataSource
//	    ,
//        "events": {
//            "realTimeUpdateComplete" : function (evt, arg){
//                var firstPosition = evt.sender.getData(1)
//                var businessPosition = evt.sender.getData(2)
//
//                var tinyAdjustment = 0.001 //the tiny adjustment is to avoid precision problem that causes floor to truncate number like 0.99999
//                configuration["first"] = Math.floor(tinyAdjustment + maxSeats * (100 - firstPosition) / 100 / spaceMultipliers.first)
//
//                if (firstPosition < businessPosition) {  //dragging first past business to the left => eliminate all business
//                	configuration["business"] = 0
//                } else {
//                	configuration["business"] = Math.floor(tinyAdjustment + (maxSeats * (100 - businessPosition) / 100 - configuration["first"] * spaceMultipliers.first) / spaceMultipliers.business)
//                }
//
//                if (businessPosition == 0) { //allow elimination of all economy seats
//                	configuration["economy"] = 0
//                } else {
//                	configuration["economy"] = Math.floor(tinyAdjustment + (maxSeats - configuration["first"] * spaceMultipliers.first - configuration["business"] * spaceMultipliers.business) / spaceMultipliers.economy)
//                }
//
//
//                //console.log(configuration)
//
//                updateDataSource(configuration)
//                callback(configuration)
//
//                container.updateFusionCharts({
//                	"dataSource": dataSource
//                });
//            }
//        }
	})
}

function plotAirportShares(airportShares, currentAirportId, container) {
	container.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))
	
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
	    containerBackgroundOpacity :'0',
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

var monthlyWeeksPerMark = 4
var quarterlyWeeksPerMark = 13
var monthlyMaxMark = 6
var quarterlyMaxMark = 8 //2 years
var plotUnitEnum = {
    MONTH : { value : 1,  maxWeek : monthlyMaxMark * monthlyWeeksPerMark, weeksPerMark : monthlyWeeksPerMark, maxMark : monthlyMaxMark},
    QUARTER : { value : 2,  maxWeek : quarterlyMaxMark * quarterlyWeeksPerMark, weeksPerMark : quarterlyWeeksPerMark, maxMark : quarterlyMaxMark}
}


function plotLinkProfit(linkConsumptions, container, plotUnit) {
	container.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))
	
	var data = []
	var category = []
	 
	var profitByMark = {}
	var markOrder = []

	if (plotUnit === undefined) {
        plotUnit = plotUnitEnum.MONTH
    }

    var maxMark = plotUnit.maxMark
  	var xLabel
  	var yLabel
  	var weeksPerMark = plotUnit.weeksPerMark
   	switch (plotUnit.value) {
        case plotUnitEnum.MONTH.value:
            xLabel = 'Month'
            yLabel = 'Monthly Profit'
            break;
        case plotUnitEnum.QUARTER.value:
            xLabel = 'Quarter'
            yLabel = 'Quarterly Profit'
            break;
    }

	$.each(linkConsumptions, function(index, linkConsumption) {
		//group in months first
		var mark = Math.floor(linkConsumption.cycle / weeksPerMark)
		if (profitByMark[mark] === undefined) {
			profitByMark[mark] = linkConsumption.profit
			markOrder.push(mark)
		} else {
			profitByMark[mark] += linkConsumption.profit
		}
	})
	

	markOrder = markOrder.slice(0, maxMark)
	$.each(markOrder.reverse(), function(key, mark) {
		data.push({ value : profitByMark[mark] })
		category.push({ label : mark.toString() })
	})

	var chartConfig = {
                        "xAxisname": xLabel,
                        "yAxisName": yLabel,
                        "numberPrefix": "$",
                        "useroundedges": "1",
                        "animation": "0",
                        "showBorder":"0",
                        "showPlotBorder":"0",
                        "toolTipBorderRadius": "2",
                        "toolTipPadding": "5",
                        "bgAlpha": "0",
                        "showValues":"0"
                        }

    checkDarkTheme(chartConfig)

	var chart = container.insertFusionCharts({
		type: 'mscombi2d',
	    width: '100%',
	    height: '100%',
	    containerBackgroundOpacity :'0',
	    dataFormat: 'json',
		dataSource: {
	    	"chart": chartConfig,
	    	"categories" : [{ "category" : category}],
			"dataset" : [ {"data" : data}, {"renderas" : "Line", "data" : data} ]
	    	            
	    }
	})
}

function plotLinkConsumption(linkConsumptions, ridershipContainer, revenueContainer, priceContainer, plotUnit) {
	ridershipContainer.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))
	
	revenueContainer.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))
	
	priceContainer.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))
	
	var emptySeatsData = []
	var cancelledSeatsData = []
	var soldSeatsData = {
			economy : [],
			business : [],
	        first : []
	}
	var revenueByClass = {
			economy : [],
			business : [],
	        first : []
	}
	
	var priceByClass = {
		economy : [],
		business : [],
        first : []
	}


		
	var category = []

    if (plotUnit === undefined) {
	    plotUnit = plotUnitEnum.MONTH
	}

	var maxWeek = plotUnit.maxWeek
	var weeksPerMark = plotUnit.weeksPerMark
	var xLabel
	switch (plotUnit.value) {
      case plotUnitEnum.MONTH.value:
        xLabel = 'Month'
        break;
      case plotUnitEnum.QUARTER.value:
        xLabel = 'Quarter'
        break;
    }


	if (!jQuery.isEmptyObject(linkConsumptions)) {
		linkConsumptions = $(linkConsumptions).toArray().slice(0, maxWeek)
        var hasCapacity = {} //check if there's any capacity for this link class at all
        hasCapacity.economy = $.grep(linkConsumptions, function(entry, index) {
            return entry.capacity.economy > 0
        }).length > 0
        hasCapacity.business = $.grep(linkConsumptions, function(entry, index) {
            return entry.capacity.business > 0
        }).length > 0
        hasCapacity.first = $.grep(linkConsumptions, function(entry, index) {
            return entry.capacity.first > 0
        }).length > 0

		$.each(linkConsumptions.reverse(), function(key, linkConsumption) {
			var capacity = linkConsumption.capacity.economy + linkConsumption.capacity.business + linkConsumption.capacity.first
			var soldSeats = linkConsumption.soldSeats.economy + linkConsumption.soldSeats.business + linkConsumption.soldSeats.first
			var cancelledSeats = linkConsumption.cancelledSeats.economy + linkConsumption.cancelledSeats.business + linkConsumption.cancelledSeats.first
			emptySeatsData.push({ value : capacity - soldSeats - cancelledSeats  })
			cancelledSeatsData.push({ value : cancelledSeats  })
			
			soldSeatsData.economy.push({ value : linkConsumption.soldSeats.economy })
			soldSeatsData.business.push({ value : linkConsumption.soldSeats.business })
			soldSeatsData.first.push({ value : linkConsumption.soldSeats.first })
			
			revenueByClass.economy.push({ value : linkConsumption.price.economy * linkConsumption.soldSeats.economy })
			revenueByClass.business.push({ value : linkConsumption.price.business * linkConsumption.soldSeats.business })
			revenueByClass.first.push({ value : linkConsumption.price.first * linkConsumption.soldSeats.first })

			if (hasCapacity.economy) {
			    priceByClass.economy.push({ value : linkConsumption.price.economy })
			}
			if (hasCapacity.business) {
			    priceByClass.business.push({ value : linkConsumption.price.business })
            }
            if (hasCapacity.first) {
			    priceByClass.first.push({ value : linkConsumption.price.first })
			}
			
			var mark = Math.floor(linkConsumption.cycle / weeksPerMark)
			//var week = linkConsumption.cycle % 4 + 1
			category.push({ label : mark.toString()})
		})
	}

	var chartConfig = {
                      	    		"xAxisname": xLabel,
                      	    		"YAxisName": "Seats Consumption",
                      	    		//"sYAxisName": "Load Factor %",
                      	    		"sNumberSuffix" : "%",
                      	            "sYAxisMaxValue" : "100",
                      	            "transposeAxis":"1",
                      	    		"useroundedges": "1",
                      	    		"animation": "0",
                      	    		"showBorder":"0",
                                      "toolTipBorderRadius": "2",
                                      "toolTipPadding": "5",
                                      "plotBorderAlpha": "10",
                                      "usePlotGradientColor": "0",
                                      "paletteColors": "#007849,#0375b4,#ffce00,#D46A6A,#bbbbbb",
                                      "bgAlpha":"0",
                                      "showValues":"0",
                                      "canvasPadding":"0",
                                      "labelDisplay":"wrap",
                                      "labelStep": weeksPerMark
                      	    	}

	checkDarkTheme(chartConfig, true)
	
	var ridershipChart = ridershipContainer.insertFusionCharts( {
		type: 'stackedarea2d',
	    width: '100%',
	    height: '100%',
	    dataFormat: 'json',
	    containerBackgroundOpacity :'0',
		dataSource: {
	    	"chart": chartConfig,
	    	"categories" : [{ "category" : category}],
			"dataset" : [
			              {"seriesName": "Sold Seats (Economy)", "data" : soldSeatsData.economy}
						 ,{"seriesName": "Sold Seats (Business)","data" : soldSeatsData.business}
						 ,{"seriesName": "Sold Seats (First)", "data" : soldSeatsData.first}
						 ,{ "seriesName": "Cancelled Seats", "data" : cancelledSeatsData}
						 ,{ "seriesName": "Empty Seats", "data" : emptySeatsData}			              
			            //, {"seriesName": "Load Factor", "renderAs" : "line", "parentYAxis": "S", "data" : loadFactorData} 
			            ]
	    }
	})

	chartConfig = {
	"xAxisname": xLabel,
    	    		"YAxisName": "Revenue",
    	    		//"sYAxisName": "Load Factor %",
    	    		"sYAxisMaxValue" : "100",
    	    		"transposeAxis":"1",
    	    		"useroundedges": "1",
    	    		"numberPrefix": "$",
    	    		"animation": "0",
    	    		"showBorder":"0",
                    "toolTipBorderRadius": "2",
                    "toolTipPadding": "5",
                    "plotBorderAlpha": "10",
                    "usePlotGradientColor": "0",
                    "paletteColors": "#007849,#0375b4,#ffce00",
                    "bgAlpha":"0",
                    "showValues":"0",
                    "canvasPadding":"0",
                    "labelDisplay":"wrap",
    	            "labelStep": weeksPerMark}
	checkDarkTheme(chartConfig, true)
	
	var revenueChart = revenueContainer.insertFusionCharts( {
    	type: 'stackedarea2d',
	    width: '100%',
	    height: '100%',
	    dataFormat: 'json',
	    containerBackgroundOpacity :'0',
		dataSource: {
	    	"chart": chartConfig,
	    	"categories" : [{ "category" : category}],
			"dataset" : [
			              {"seriesName": "Revenue (Economy)", "data" : revenueByClass.economy}
						 ,{"seriesName": "Revenue (Business)","data" : revenueByClass.business}
						 ,{"seriesName": "Revenue (First)", "data" : revenueByClass.first}
			            ]
	   }	
	})

	chartConfig =  {
                  	    		"xAxisname": xLabel,
                  	    		"YAxisName": "Ticket Price",
                  	    		//"sYAxisName": "Load Factor %",
                  	    		"numberPrefix": "$",
                  	    		"sYAxisMaxValue" : "100",
                  	    		"useroundedges": "1",
                  	    		"transposeAxis":"1",
                  	    		"animation": "0",
                  	    		"showBorder":"0",
                  	    		"drawAnchors": "0",
                                  "toolTipBorderRadius": "2",
                                  "toolTipPadding": "5",
                                  "paletteColors": "#007849,#0375b4,#ffce00",
                                  "bgAlpha":"0",
                                  "showValues":"0",
                                  "canvasPadding":"0",
                                  "formatNumberScale" : "0",
                                  "labelDisplay":"wrap",
                  	            "labelStep": weeksPerMark
                  	    	}
    checkDarkTheme(chartConfig, true)
	
	var priceChart = priceContainer.insertFusionCharts( {
    	type: 'msline',
	    width: '100%',
	    height: '100%',
	    dataFormat: 'json',
	    containerBackgroundOpacity :'0',
		dataSource: {
	    	"chart": chartConfig,
	    	"categories" : [{ "category" : category}],
			"dataset" : [
			              {"seriesName": "Price (Economy)", "data" : priceByClass.economy}
						 ,{"seriesName": "Price (Business)","data" : priceByClass.business}
						 ,{"seriesName": "Price (First)", "data" : priceByClass.first}
			            ]
	   }	
	})
}


function plotPie(dataSource, currentKey, container, keyName, valueName) {
	container.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))
	
	var data = []
	$.each(dataSource, function(key, dataEntry) {
		var entry = {
			label : dataEntry[keyName],
			value : dataEntry[valueName]
		}
		
		if (dataEntry.color) {
			entry.color = dataEntry.color
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
	    height: '160px',
	    dataFormat: 'json',
	    containerBackgroundOpacity :'0',
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
                "showPlotBorder":"0",
                "showLabels":"0",
                "showValues":"0",
                "plottooltext": "$label - Passengers : $datavalue ($percentValue)"
	    	},
			"data" : data
	    }
	})
}

function plotIncomeChart(airlineIncomes, period, container) {
	container.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))
	
	var data = {}
	data["total"] = []
	data["links"] = []
	data["transactions"] = []
	data["others"] = []
	var category = []
	 
	var profitByMonth = {}
	var monthOrder = []
	
	$.each(airlineIncomes, function(key, airlineIncome) {
		data["total"].push({ value : airlineIncome.totalProfit })
		data["links"].push({ value : airlineIncome.linksProfit })
		data["transactions"].push({ value : airlineIncome.transactionsProfit })
		data["others"].push({ value : airlineIncome.othersProfit })
		category.push({ "label" : airlineIncome.cycle.toString() })
	})

	var chartConfig = {
                  	    		"xAxisname": "Week",
                  	    		"yAxisName": "Profit",
                  	    		"numberPrefix": "$",
                  	    		"useroundedges": "1",
                  	    		"animation": "1",
                  	    		"showBorder":"0",
                                  "toolTipBorderRadius": "2",
                                  "toolTipPadding": "5",
                                  "bgAlpha":"0",
                                  "showValues":"1",
                                  "showZeroPlane": "1",
                                  "zeroPlaneColor": "#222222",
                                  "zeroPlaneThickness": "2",
                  	    	}


    checkDarkTheme(chartConfig)
	
	var chart = container.insertFusionCharts({
		type: 'msline',
	    width: '100%',
	    height: '100%',
	    containerBackgroundOpacity :'0',
	    dataFormat: 'json',
		dataSource: {
	    	"chart": chartConfig,
	    	"categories" : [{ "category" : category}],
			"dataset" : [ 
				{ "seriesname": "Total Income", "data" : data["total"]},
				{ "seriesname": "Flight Income", "data" : data["links"], "visible" : "0"},
				{ "seriesname": "Transaction Income", "data" : data["transactions"], "visible" : "0"},
				{ "seriesname": "Other Income", "data" : data["others"], "visible" : "0"}]
	    	            
	    }
	})
}

function plotCashFlowChart(airlineCashFlows, period, container) {
	container.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))
	
	var data = {}
	data["cashFlow"] = []
	var category = []
	 
	var profitByMonth = {}
	var monthOrder = []
	
	$.each(airlineCashFlows, function(key, airlineCashFlow) {
		data["cashFlow"].push({ value : airlineCashFlow.totalCashFlow })
		category.push({ "label" : airlineCashFlow.cycle.toString() })
	})

	var chartConfig = {
                      	    		"xAxisname": "Week",
                      	    		"yAxisName": "Profit",
                      	    		"numberPrefix": "$",
                      	    		"useroundedges": "1",
                      	    		"animation": "1",
                      	    		"showBorder":"0",
                                      "toolTipBorderRadius": "2",
                                      "toolTipPadding": "5",
                                      "bgAlpha":"0",
                                      "showValues":"1",
                                      "showZeroPlane": "1",
                                      "zeroPlaneColor": "#222222",
                                      "zeroPlaneThickness": "2",
                      	    	}
    checkDarkTheme(chartConfig)
	
	var chart = container.insertFusionCharts({
		type: 'msline',
	    width: '100%',
	    height: '100%',
	    dataFormat: 'json',
	    containerBackgroundOpacity :'0',
		dataSource: {
	    	"chart": chartConfig,
	    	"categories" : [{ "category" : category}],
			"dataset" : [ 
				{ "seriesname": "Total CashFlow", "data" : data["cashFlow"] }
			]
	    }
	})
}

function plotOilPriceChart(oilPrices, container) {
	container.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))
	
	var data = []
	var category = []
	var total = 0 
	var count = 0
	
	$.each(oilPrices, function(key, oilPrice) {
		data.push({ value : oilPrice.price })
		category.push({ "label" : oilPrice.cycle.toString() })
		total += oilPrice.price
		count ++;
	})
	
	var average
	if (count > 0)  {
		average = total / count
	} else {
		average = 0
	}

	var chartConfig = {
                      	    		"xAxisname": "Week",
                      	    		"yAxisName": "Oil Price Per Barrel",
                      	    		"numberPrefix": "$",
                      	    		"useroundedges": "1",
                      	    		"animation": "1",
                      	    		"showBorder":"0",
                      	    		"showValues": "0",
                                      "toolTipBorderRadius": "2",
                                      "toolTipPadding": "5",
                                      "bgAlpha":"0",
                                      "drawAnchors": "0",
                                      "setAdaptiveYMin":"1",
                                      "labelStep": "4"
                      	    	}
    checkDarkTheme(chartConfig)
	
	var chart = container.insertFusionCharts({
		type: 'msline',
	    width: '100%',
	    height: '100%',
	    containerBackgroundOpacity :'0',
	    dataFormat: 'json',
		dataSource: {
	    	"chart": chartConfig,
	    	"categories" : [{ "category" : category}],
			"dataset" : [{ "seriesname": "Price", "data" : data}],
			"trendlines": [{
	            "line": [
	                {
	                    "startvalue": average,
	                    "color": "#A1D490",
	                    "displayvalue": "Average",
	                    "valueOnRight": "1",
	                    "thickness": "2"
	                }
	            ]
	        }]
	    }
	})
}


function plotLoanInterestRatesChart(rates, container) {
	container.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))

	var data = []
	var category = []
	var total = 0
	var count = 0

	$.each(rates, function(key, rate) {
	    var annualRate = rate.rate * 100 //to percentage based
		data.push({ value : annualRate.toFixed(1) })
		category.push({ "label" : rate.cycle.toString() })
		total += annualRate
		count ++;
	})

	var average
	if (count > 0)  {
		average = total / count
	} else {
		average = 0
	}

	var chartConfig = {
                      	    		"xAxisname": "Week",
                      	    		"yAxisName": "Base Annual Rate",
                      	    		"numberSuffix": "%",
                      	    		"useroundedges": "1",
                      	    		"animation": "1",
                      	    		"showBorder":"0",
                      	    		"showValues": "0",
                      	    		"drawAnchors": "0",
                                      "toolTipBorderRadius": "2",
                                      "toolTipPadding": "5",
                                      "bgAlpha":"0",
                                      "setAdaptiveYMin":"1",
                                      "labelStep": "4"
                      	    	}

    checkDarkTheme(chartConfig)
	var chart = container.insertFusionCharts({
		type: 'msline',
	    width: '100%',
	    height: '100%',
	    dataFormat: 'json',
	    containerBackgroundOpacity :'0',
		dataSource: {
	    	"chart": chartConfig,
	    	"categories" : [{ "category" : category}],
			"dataset" : [{ "seriesname": "Rate", "data" : data}],
			containerBackgroundOpacity :'0',
			"trendlines": [{
	            "line": [
	                {
	                    "startvalue": average.toFixed(1),
	                    "color": "#A1D490",
	                    "displayvalue": "Average",
	                    "valueOnRight": "1",
	                    "thickness": "2"
	                }
	            ]
	        }]
	    }
	})
}

function plotRivalHistoryChart(allRivalLinkConsumptions, priceContainer, linkClass, field, numberPrefix, currentAirlineId) {
	priceContainer.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))

	var priceByAirline = {}

	var category = []

	var maxWeek = 24
	var weekCount = 0 //the rival with the most week count, usually this is just maxWeek

    var dataSet = []
    var maxValue = -1
    var minValue = 99999
    if (!jQuery.isEmptyObject(allRivalLinkConsumptions)) { //link consumptions is array (by each rival link) of array (by cycle),
	    $.each(allRivalLinkConsumptions, function(key, linkConsumptions) {
            if (linkConsumptions.length == 0) {
                return; //no consumptions yet
            }
            var newCategory = []
            var linkConsumptions = $(linkConsumptions).toArray().slice(0, maxWeek) //link consumptions for each rival link
            if (linkConsumptions.length > weekCount) { //check which rival has the longest history
                weekCount = linkConsumptions.length
            }

            var airlineName = linkConsumptions[0].airlineName
            var linkId = linkConsumptions[0].linkId
            priceHistory = []
            var lineColor = linkConsumptions[0].airlineId == currentAirlineId ? "#d84f4f" : "#f6bf1b"
            $.each(linkConsumptions.reverse(), function(key, linkConsumption) {
                var currentValue = linkConsumption[field][linkClass]
                if (currentValue > maxValue) {
                    maxValue = currentValue
                }
                if (currentValue < minValue) {
                    minValue = currentValue
                }
                priceHistory.push({ value : currentValue , color : lineColor})
                var month = Math.floor(linkConsumption.cycle / 4)
                //var week = linkConsumption.cycle % 4 + 1
                newCategory.push({ label : month.toString()})
            })
            dataSet.push({ "seriesName": airlineName, "data" : priceHistory})
            if (newCategory.length > category.length) { //take the longest length one
                category = newCategory
            }
       })

       //now pad at the beginning for those that have been around less than weekCount
       $.each(dataSet, function(index, dataEntry) {
            if (dataEntry["data"].length < weekCount) {
                var padLength = weekCount - dataEntry["data"].length
                for (i = 0; i < padLength; i++) {
                    dataEntry["data"].unshift({ "data" : ""})
                }
            }
       })
	}


     var yAxisMax = Math.round(maxValue * 1.1)
     var yAxisMin = Math.round(minValue * 0.8)

    var chartConfig = {
                      	    		"xAxisname": "Month",
                      	    		//"sYAxisName": "Load Factor %",
                      	    		"numberPrefix": numberPrefix,
                      	    		"sYAxisMaxValue" : "100",
                      	    		"useroundedges": "1",
                      	    		"transposeAxis":"1",
                      	    		"animation": "0",
                      	    		"showBorder":"0",
                      	    		"drawAnchors": "0",
                                      "toolTipBorderRadius": "2",
                                      "toolTipPadding": "5",
                                      "bgAlpha":"0",
                                      "showLegend": "0",
                                      "showValues":"0",
                                      "canvasPadding":"0",
                                      "labelDisplay":"wrap",
                      	            "labelStep": "4",
                      	            "formatNumber" : "0",
                      	            "formatNumberScale" : "0",
                      	            "yAxisMaxValue": yAxisMax,
                                      "yAxisMinValue": yAxisMin
                      	    	}
	checkDarkTheme(chartConfig)
	var priceChart = priceContainer.insertFusionCharts( {
    	type: 'msline',
	    width: '100%',
	    height: '100%',
	    dataFormat: 'json',
	    containerBackgroundOpacity :'0',
		dataSource: {
	    	"chart": chartConfig,
	    	"categories" : [{ "category" : category}],
			"dataset" : dataSet
	   }
	})
}

function plotLoyalistHistoryChart(loyalistHistory, container) {
	container.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))

    if (jQuery.isEmptyObject(loyalistHistory)) {
        return
    }

	var category = []

	var dataSet = []
//    var maxValue = -1
//    var minValue = 99999
    var dataByAirlineId = {}
    var airlineNameByAirlineId = {}
    if (!jQuery.isEmptyObject(loyalistHistory)) { //link consumptions is array (by each rival link) of array (by cycle),

	    $.each(loyalistHistory, function(index, keyValue) {
            var cycle = keyValue[0]
            var cycleEntries = keyValue[1]
            category.push({ label : cycle.toString()})
            $.each(cycleEntries, function(index, entry) {
                var airlineId = entry.airlineId
                if (!dataByAirlineId[airlineId]) {
                    dataByAirlineId[airlineId] = []
                }
//                var lineColor = "#f6bf1b"
//                if (activeAirline && activeAirline.id == airlineId) {
//                    lineColor = "#d84f4f"
//                }
//                dataByAirlineId[airlineId].push({"value": entry.amount, "color": lineColor})
                dataByAirlineId[airlineId].push({"value": entry.amount})
                airlineNameByAirlineId[airlineId] = entry.airlineName
            })
        })

        $.each(airlineNameByAirlineId, function(airlineId, airlineName) {
            dataSet.push({ "seriesName": airlineName, "data" : dataByAirlineId[airlineId]})
        })
    }

//     var yAxisMax = Math.round(maxValue * 1.1)
//     var yAxisMin = Math.round(minValue * 0.8)
    var chartConfig = {
                      	    		"xAxisname": "Week",
                      	    		"yAxisName": "Loyalist Amount",
                      	    		"useroundedges": "1",
                      	    		"transposeAxis":"1",
                      	    		"animation": "0",
                      	    		"showBorder":"0",
                      	    		"drawAnchors": "0",
                                      "toolTipBorderRadius": "2",
                                      "toolTipPadding": "5",
                                      "bgAlpha":"0",
                      //                "showLegend": "0",
                                      "showplotBorder": "1",
                                       "plotHighlightEffect": "fadeout",
                                      "showValues":"0",
                                      "canvasPadding":"0",
                                      "labelDisplay":"wrap",
                      	            "labelStep": "4",
                      	            "formatNumber" : "0",
                      	            "formatNumberScale" : "0"
                      //	            "yAxisMaxValue": yAxisMax,
                      //                "yAxisMinValue": yAxisMin
                      	    	}
    checkDarkTheme(chartConfig)
	var loyalistHistoryChart = container.insertFusionCharts( {
    	type: 'logmsline',
	    width: '100%',
	    height: '100%',
	    dataFormat: 'json',
	    containerBackgroundOpacity :'0',
		dataSource: {
	    	"chart": chartConfig,
	    	"categories" : [{ "category" : category}],
			"dataset" : dataSet
	   }
	})
}

function checkDarkTheme(chartConfig, keepPallette) {
    if (document.documentElement.getAttribute("data-theme") === "dark") {
    //if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            chartConfig.bgAlpha = "0"
            chartConfig.legendBgAlpha = "0"
            chartConfig.canvasBgAlpha = "10"
            chartConfig.canvasBgColor = "#DDDDDD"
            chartConfig.showAlternateHGridColor ="0"
            chartConfig.useRoundEdges = "0"
            if (!keepPallette) {
                chartConfig.palettecolors = "80CED7,FFF07C,80FF72,EEC0C6, 9067C6, E58C8A"
            }
            chartConfig.baseFontColor = "#DDDDDD"
            chartConfig.usePlotGradientColor = "0"
            chartConfig.legendBgColor = "#DDDDDD"
            chartConfig.legendBgAlpha = "10"
            chartConfig.toolTipBgColor = "#444444"
            chartConfig.toolTipColor = "#DDDDDD"
            chartConfig.valueFontColor = "#DDDDDD"


    //                "legendIconAlpha": "50",
    //                "legendIconBgAlpha": "30",
    //                "legendIconBorderColor": "#123456",
    //                "legendIconBorderThickness": "3"
    }
}

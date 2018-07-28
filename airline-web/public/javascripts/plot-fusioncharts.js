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
		        containerBackgroundOpacity :'0',
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

function plotSeatConfigurationGauge(container, configuration, maxSeats, spaceMultipliers, callback) {
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
	        "showtooltip": "0",
	        "chartBottomMargin": "0",
	        "bgAlpha":"0",
	        "valueFontSize": "11",  
	        "valueFontBold": "0",
	        "animation": "0",
	        "editMode": "1",
	        containerBackgroundOpacity :'0',
	        "baseFontColor": "#FFFFFF"
	    },
	    "pointers": {
	        //Multiple pointers defined here
	        "pointer": [
	            {
	                "bgColor": "#FFE62B",
	                "bgAlpha": "50",
	                "showValue": "0",
	                //"sides" : "4",
	                "borderColor": "#FFE62B",
	                "borderAlpha": "20",
	            },
	            {
	                "bgColor": "#0077CC",
	                "bgAlpha": "50",
	                "showValue": "0",
	                //"sides" : "3",
	                "borderColor": "#0077CC",
	                "borderAlpha": "20",
	            }
	        ]
	    }
	}
	
	function updateDataSource(configuration) {
		var businessPosition = configuration.economy / maxSeats * 100
		var firstPosition = (maxSeats - configuration.first * spaceMultipliers.first) / maxSeats * 100
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
        width: '100%',
        height: '40px',
        dataFormat: 'json',
	    dataSource: dataSource,
        "events": {
            "realTimeUpdateComplete" : function (evt, arg){
                var firstPosition = evt.sender.getData(1)
                var businessPosition = evt.sender.getData(2)
                
                var tinyAdjustment = 0.001 //the tiny adjustment is to avoid precision problem that causes floor to truncate number like 0.99999
                configuration["first"] = Math.floor(tinyAdjustment + maxSeats * (100 - firstPosition) / 100 / spaceMultipliers.first)
                
                if (firstPosition < businessPosition) {  //dragging first past business to the left => eliminate all business
                	configuration["business"] = 0
                } else {
                	configuration["business"] = Math.floor(tinyAdjustment + (maxSeats * (100 - businessPosition) / 100 - configuration["first"] * spaceMultipliers.first) / spaceMultipliers.business)
                }
                
                if (businessPosition == 0) { //allow elimination of all economy seats
                	configuration["economy"] = 0
                } else {
                	configuration["economy"] = Math.floor(tinyAdjustment + (maxSeats - configuration["first"] * spaceMultipliers.first - configuration["business"] * spaceMultipliers.business) / spaceMultipliers.economy)
                }
                
                
                //console.log(configuration)
                
                updateDataSource(configuration)
                callback(configuration)
                
                container.updateFusionCharts({
                	"dataSource": dataSource
                });
            }
        }
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

function plotLinkProfit(linkConsumptions, container) {
	container.children(':FusionCharts').each((function(i) {
		  $(this)[0].dispose();
	}))
	
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
	    height: '100%',
	    containerBackgroundOpacity :'0',
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

function plotLinkConsumption(linkConsumptions, ridershipContainer, revenueContainer, priceContainer) {
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
	 
	var maxWeek = 24
	
	if (!jQuery.isEmptyObject(linkConsumptions)) {
		linkConsumptions = $(linkConsumptions).toArray().slice(0, maxWeek)
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
			
			priceByClass.economy.push({ value : linkConsumption.price.economy })
			priceByClass.business.push({ value : linkConsumption.price.business })
			priceByClass.first.push({ value : linkConsumption.price.first })
			
			var month = Math.floor(linkConsumption.cycle / 4)
			//var week = linkConsumption.cycle % 4 + 1
			category.push({ label : month.toString()})
		})
	}
	
	var ridershipChart = ridershipContainer.insertFusionCharts( {
		type: 'stackedarea2d',
	    width: '100%',
	    height: '100%',
	    dataFormat: 'json',
	    containerBackgroundOpacity :'0',
		dataSource: {
	    	"chart": {
	    		"xAxisname": "Month",
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
                "labelStep": "4"
	    	},
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
	
	var revenueChart = revenueContainer.insertFusionCharts( {
    	type: 'stackedarea2d',
	    width: '100%',
	    height: '100%',
	    dataFormat: 'json',
	    containerBackgroundOpacity :'0',
		dataSource: {
	    	"chart": {
	    		"xAxisname": "Month",
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
	            "labelStep": "4"
	    	},
	    	"categories" : [{ "category" : category}],
			"dataset" : [
			              {"seriesName": "Revenue (Economy)", "data" : revenueByClass.economy}
						 ,{"seriesName": "Revenue (Business)","data" : revenueByClass.business}
						 ,{"seriesName": "Revenue (First)", "data" : revenueByClass.first}
			            ]
	   }	
	})
	
	var priceChart = priceContainer.insertFusionCharts( {
    	type: 'msline',
	    width: '100%',
	    height: '100%',
	    dataFormat: 'json',
	    containerBackgroundOpacity :'0',
		dataSource: {
	    	"chart": {
	    		"xAxisname": "Month",
	    		"YAxisName": "Ticket Price",
	    		//"sYAxisName": "Load Factor %",
	    		"numberPrefix": "$",
	    		"sYAxisMaxValue" : "100",
	    		"useroundedges": "1",
	    		"transposeAxis":"1",
	    		"animation": "0",
	    		"showBorder":"0",
                "toolTipBorderRadius": "2",
                "toolTipPadding": "5",
                "paletteColors": "#007849,#0375b4,#ffce00",
                "bgAlpha":"0",
                "showValues":"0",
                "canvasPadding":"0",
                "labelDisplay":"wrap",
	            "labelStep": "4"
	    	},
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
	
	var chart = container.insertFusionCharts({
		type: 'msline',
	    width: '100%',
	    height: '100%',
	    dataFormat: 'json',
		dataSource: {
	    	"chart": {
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
	    	},
	    	"categories" : [{ "category" : category}],
			"dataset" : [ 
				{ "seriesname": "Total Income", "data" : data["total"]},
				{ "seriesname": "Flight Income", "data" : data["links"], "visible" : "0"},
				{ "seriesname": "Transaction Income", "data" : data["transactions"], "visible" : "0"},
				{ "seriesname": "Other Income", "data" : data["others"], "visible" : "0"}]
	    	            
	    }
	})
}


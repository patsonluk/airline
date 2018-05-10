function plotMaintenanceQualityGauge(container, currentQualityInput) {
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
		                "value" : currentQualityInput.val()
		            }
		        ]
		    },
		    "colorRange" : {
		    	"color": [
                      {
                    	  "minValue": "0",
                          "maxValue": "100",
                          "label": currentQualityInput.val() + "%",
                          "code": "#6baa01"
                      }]
		    }
		}
	var chart = container.insertFusionCharts(
			{	
				type: 'hlineargauge',
		        width: '200',
		        height: '25',
		        dataFormat: 'json',
			    dataSource: dataSource,
				events: {
		            //Event is raised when a real-time gauge or chart completes updating data.
		            //Where we can get the updated data and display the same.
		            "realTimeUpdateComplete" : function (evt, arg){
		                var newQuality = evt.sender.getData(1)
		                //take the floor
		                newQuality = Math.floor(newQuality)
		                dataSource["pointers"]["pointer"][0].value = newQuality
		                dataSource["colorRange"]["color"][0].label = newQuality + "%"
		                currentQualityInput.val(newQuality)
		                container.updateFusionCharts({
		                	"dataSource": dataSource
		                });
		            }
		        }
			})
	
}

function plotSeatConfigurationGauge(container, configuration, maxSeats) {
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
        height: '30',
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
                
                configuration["first"] = Math.round(maxSeats * (100 - firstPosition) / 100 / 8)
                
                if (firstPosition == 0) { //allow elimination of all business seats
                	configuration["business"] = 0
                } else {
                	configuration["business"] = Math.round((maxSeats * (100 - businessPosition) / 100 - configuration["first"] * 8) / 4)
                }
                
                if (businessPosition == 0) { //allow elimination of all economy seats
                	configuration["economy"] = 0
                } else {
                	configuration["economy"] = maxSeats - configuration["first"] * 8 - configuration["business"] * 4
                }
                
                
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
	var emptySeatsData = []
	var soldSeatsData = {
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
			emptySeatsData.push({ value : capacity - soldSeats  })
			
			soldSeatsData.economy.push({ value : linkConsumption.soldSeats.economy })
			soldSeatsData.business.push({ value : linkConsumption.soldSeats.business })
			soldSeatsData.first.push({ value : linkConsumption.soldSeats.first })
			
			var month = Math.floor(linkConsumption.cycle / 4)
			//var week = linkConsumption.cycle % 4 + 1
			category.push({ label : month.toString()})
		})
	}
	
	var chart = container.insertFusionCharts({
		type: 'stackedarea2d',
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
                "usePlotGradientColor": "0",
                "paletteColors": "#ffce00,#0375b4,#007849,#bbbbbb",
                "bgAlpha":"0",
                "showValues":"0",
                "canvasPadding":"0",
                "labelDisplay":"wrap",
                "labelStep": "4"
	    	},
	    	"categories" : [{ "category" : category}],
			"dataset" : [
						  {"seriesName": "Sold Seats (First)", "data" : soldSeatsData.first}
						 ,{"seriesName": "Sold Seats (Business)","data" : soldSeatsData.business}
						 ,{"seriesName": "Sold Seats (Economy)", "data" : soldSeatsData.economy}
						 ,{ "seriesName": "Empty Seats", "data" : emptySeatsData}			              

			            
			            
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


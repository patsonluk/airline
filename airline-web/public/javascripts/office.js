var loadedIncomes = {}
var incomePage = 0;
var incomePeriod;

function showOfficeCanvas() {
	setActiveDiv($("#officeCanvas"))
	highlightTab($('#officeCanvasTab'))
	
	updateAirlineDetails()
	loadIncomeSheet();
	updateChampionedCountriesDetails()
	updateServiceFundingDetails()
	updateMaintenanceLevelDetails()
}

function updateAirlineDetails() {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "?extendedInfo=true"
    $.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airline) {
	    	$('#destinations').text(airline.destinations)
	    	$('#fleetSize').text(airline.fleetSize)
	    	$('#assets').text('$' + commaSeparateNumber(airline.assets))
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function loadIncomeSheet() {
	var airlineId = activeAirline.id
	//reset values
	loadedIncomes = {}
	loadedIncomes['WEEKLY'] = []
	loadedIncomes['MONTHLY'] = []
	loadedIncomes['YEARLY'] = []
	incomePage = 0 
	incomePeriod = 'WEEKLY'
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/incomes",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airlineIncomes) {
	    	//group by period
	    	$.each(airlineIncomes, function(index, airlineIncome) {
	    		loadedIncomes[airlineIncome.period].push(airlineIncome)
	    	})
	    	
	    	var totalPages = loadedIncomes[incomePeriod].length
	    	if (totalPages > 0) {
	    		incomePage = totalPages - 1
	    		updateIncomeSheet(loadedIncomes[incomePeriod][incomePage])
	    	}
	    	
	    	updateIncomeChart()
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateIncomeChart() {
	plotIncomeChart(loadedIncomes[incomePeriod], incomePeriod, $("#officeCanvas #totalProfitChart"))
}



function incomeHistoryStep(step) {
	var totalPages = loadedIncomes[incomePeriod].length
	
	if (incomePage + step < 0) {
		incomePage = 0
	} else if (incomePage + step >= totalPages) {
		incomePage = totalPages - 1
	} else {
		incomePage = incomePage + step
	}
	
	updateIncomeSheet(loadedIncomes[incomePeriod][incomePage])
}

function changeIncomePeriod(period) {
	incomePeriod = period
	var totalPages = loadedIncomes[incomePeriod].length
	incomePage = totalPages - 1
	updateIncomeSheet(loadedIncomes[incomePeriod][incomePage])
	updateIncomeChart()
}

function updateIncomeSheet(airlineIncome) {
	if (airlineIncome) {
		var periodText
		var periodCount
		var inProgress
		if (airlineIncome.period == "WEEKLY") {
			periodText = "Week"
			periodCount= airlineIncome.cycle
		} else if (airlineIncome.period == "MONTHLY") {
			periodText = "Month"
			periodCount = Math.ceil(airlineIncome.cycle / 4)
			inProgress = (airlineIncome.cycle + 1) % 4
		} else if (airlineIncome.period == "YEARLY") {
			periodText = "Year"
			periodCount = Math.ceil(airlineIncome.cycle / 52)
			inProgress = (airlineIncome.cycle + 1) % 52
		}
		
		var cycleText = periodText + " " + periodCount + (inProgress ? " (In Progress)" : "")
		
		$("#incomeCycleText").text(cycleText)
		$("#totalProfit").text('$' + commaSeparateNumber(airlineIncome.totalProfit))
        $("#totalRevenue").text('$' + commaSeparateNumber(airlineIncome.totalRevenue))
        $("#totalExpense").text('$' + commaSeparateNumber(airlineIncome.totalExpense))
        $("#linksProfit").text('$' + commaSeparateNumber(airlineIncome.linksProfit))
        $("#linksRevenue").text('$' + commaSeparateNumber(airlineIncome.linksRevenue))
        $("#linksExpense").text('$' + commaSeparateNumber(airlineIncome.linksExpense))
        $("#linksTicketRevenue").text('$' + commaSeparateNumber(airlineIncome.linksTicketRevenue))
        $("#linksAirportFee").text('$' + commaSeparateNumber(airlineIncome.linksAirportFee))
        $("#linksFuelCost").text('$' + commaSeparateNumber(airlineIncome.linksFuelCost))
        $("#linksCrewCost").text('$' + commaSeparateNumber(airlineIncome.linksCrewCost))
        $("#linksInflightCost").text('$' + commaSeparateNumber(airlineIncome.linksInflightCost))
        $("#linksMaintenance").text('$' + commaSeparateNumber(airlineIncome.linksMaintenanceCost))
        $("#linksDepreciation").text('$' + commaSeparateNumber(airlineIncome.linksDepreciation))
        $("#linksDelayCompensation").text('$' + commaSeparateNumber(airlineIncome.linksDelayCompensation))
        $("#transactionsProfit").text('$' + commaSeparateNumber(airlineIncome.transactionsProfit))
        $("#transactionsRevenue").text('$' + commaSeparateNumber(airlineIncome.transactionsRevenue))
        $("#transactionsExpense").text('$' + commaSeparateNumber(airlineIncome.transactionsExpense))
        $("#transactionsCapitalGain").text('$' + commaSeparateNumber(airlineIncome.transactionsCapitalGain))
        $("#transactionsCreateLink").text('$' + commaSeparateNumber(airlineIncome.transactionsCreateLink))
        $("#othersProfit").text('$' + commaSeparateNumber(airlineIncome.othersProfit))
        $("#othersRevenue").text('$' + commaSeparateNumber(airlineIncome.othersRevenue))
        $("#othersExpense").text('$' + commaSeparateNumber(airlineIncome.othersExpense))
        $("#othersLoanInterest").text('$' + commaSeparateNumber(airlineIncome.othersLoanInterest))
        $("#othersBaseUpkeep").text('$' + commaSeparateNumber(airlineIncome.othersBaseUpkeep))
        $("#othersServiceInvestment").text('$' + commaSeparateNumber(airlineIncome.othersServiceInvestment))
        $("#othersMaintenanceInvestment").text('$' + commaSeparateNumber(airlineIncome.othersMaintenanceInvestment))
        $("#othersAdvertisement").text('$' + commaSeparateNumber(airlineIncome.othersAdvertisement))
        $("#othersDepreciation").text('$' + commaSeparateNumber(airlineIncome.othersDepreciation))
	}
}

function setServiceFunding(funding) {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/serviceFunding"
    var data = { "serviceFunding" : parseInt(funding) }
	$.ajax({
		type: 'PUT',
		url: url,
	    data: JSON.stringify(data),
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function() {
	    	activeAirline.serviceFunding = funding
	    	updateServiceFundingDetails()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function editServiceFunding() {
	$('#serviceFundingDisplaySpan').hide()
	$('#serviceFundingInputSpan').show()
}

function updateServiceFundingDetails() {
	$('#currentServiceQuality').text(activeAirline.serviceQuality)
	
	$('#serviceFunding').text('$' + commaSeparateNumber(activeAirline.serviceFunding))
	$('#serviceFundingInput').val(activeAirline.serviceFunding)
	
	$('#serviceFundingDisplaySpan').show()
	$('#serviceFundingInputSpan').hide()

	$('#servicePrediction').text('...')
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/servicePrediction",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(servicePrediction) {
	    	$('#servicePrediction').text(servicePrediction.prediction)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	
}

function updateChampionedCountriesDetails() {
	$('#championedCountriesList').children('div.table-row').remove()
	
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/championed-countries",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(championedCountries) {
	    	$(championedCountries).each(function(index, championDetails) {
	    		var country = championDetails.country
	    		var rankingIcon
	    		var rankingTitle
	    		if (championDetails.ranking == 1) {
	    			rankingIcon = "assets/images/icons/crown.png"
	    			rankingTitle = "1st place"
	    		} else if (championDetails.ranking == 2) {
	    			rankingIcon = "assets/images/icons/crown-silver.png"
		    		rankingTitle = "2nd place"
	    		} else if (championDetails.ranking == 3) {
	    			rankingIcon = "assets/images/icons/crown-bronze.png"
			    	rankingTitle = "3rd place"
	    		}
	    		var row = $("<div class='table-row clickable' onclick=\"loadCountryDetails('" + country.countryCode + "'); showCountryView();\"></div>")
	    		row.append("<div class='cell'><img src='" + rankingIcon + "' title='" + rankingTitle + "'/></div>")
	    		row.append("<div class='cell'>" + getCountryFlagImg(country.countryCode) + country.name + "</div>")
	    		row.append("<div class='cell'>" + championDetails.reputationBoost + "</div>") 
	    		$('#championedCountriesList').append(row)
	    	})
	    	
	    	if ($(championedCountries).length == 0) {
	    		var row = $("<div class='table-row'></div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		$('#championedCountriesList').append(row)
	    	}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	
}



function setMaintenanceLevel(maintenanceLevel) {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/maintenanceQuality"
    var data = { "maintenanceQuality" : parseInt(maintenanceLevel) }
	$.ajax({
		type: 'PUT',
		url: url,
	    data: JSON.stringify(data),
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function() {
	    	activeAirline.maintenanceQuality = maintenanceLevel
	    	updateMaintenanceLevelDetails()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function editMaintenanceLevel() {
	$('#maintenanceLevelDisplaySpan').hide()
	$('#maintenanceLevelInputSpan').show()
}

function updateMaintenanceLevelDetails() {
	$('#maintenanceLevel').text(activeAirline.maintenanceQuality + "%")
	$('#maintenanceLevelInput').val(activeAirline.maintenanceQuality)
	$("#maintenanceLevelGauge").empty()
	$('#maintenanceLevelDisplaySpan').show()
	$('#maintenanceLevelInputSpan').hide()
	plotMaintenanceLevelGauge($("#maintenanceLevelGauge"), $("#maintenanceLevelInput"), function(newLevel) {
		setMaintenanceLevel(newLevel)
	})
}

function resetAirline() {
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/reset",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function() {
	    	updateAllPanels(activeAirline.id)
	    	showWorldMap()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	
}


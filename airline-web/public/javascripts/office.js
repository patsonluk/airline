
function showOfficeCanvas() {
	setActiveDiv($("#officeCanvas"))
	highlightTab($('#officeCanvasTab'))
	
	loadIncomeSheet();
}

function loadIncomeSheet() {
	var airlineId = activeAirline.id 
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/income",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airlineIncome) {
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
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


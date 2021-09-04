var loadedContracts = {}
var loadedSuggestion

function showOilCanvas() {
	setActiveDiv($("#oilCanvas"))
	highlightTab($('.oilCanvasTab'))
	loadOilPriceChart()
	loadOilDetails() 
    loadExistingOilContracts()
}

function loadOilPriceChart() {
	var url = "oil-prices"
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(oilPrices) {
	    	plotOilPriceChart(oilPrices, $("#oilCanvas #oilPriceChart"))
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	
	
}

function loadOilDetails() {
	var url = "airlines/" + activeAirline.id + "/oil-details"
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	loadedSuggestion = result
	    	$('#oilContractVolume').val(result.suggestedBarrels)
	    	$('#oilContractDuration').val(result.suggestedDuration)
	    	$('#currentInventoryPolicy').text(result.inventoryPolicyDescription)
	    	$('#currentInventoryPrice').text('$' + commaSeparateNumber(result.inventoryPrice))
	    	$('#currentInventoryPolicyDiv').show()
	    	$('#editInventoryPolicyDiv').hide()
	    	loadOilConsumptionHistoryTable(result.history)
	    	loadOilContractConsideration(result.suggestedBarrels, result.suggestedDuration)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function loadOilConsumptionHistoryTable(entries) {
	var table = $("#oilConsumptionHistoryTable")
	table.children("div.table-row").remove()
	
	var totalConsumption = 0
	$.each(entries, function(index, entry) {
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(entry.price) + "</div>")
		row.append("<div class='cell' align='right'>" + commaSeparateNumber(entry.volume) + "</div>")
		row.append("<div class='cell' align='right'>" + entry.type + "</div>")
		table.append(row)
		
		totalConsumption += entry.volume
	});
	
	//total Row
	var row = $("<div class='table-row'></div>")
	row.append("<div class='cell' align='right'>-</div>")
	row.append("<div class='cell' align='right'>" + commaSeparateNumber(totalConsumption) + "</div>")
	row.append("<div class='cell' align='right'>Total</div>")
	table.append(row)
}

function loadOilContractConsideration(volume, duration) {
	var url = "airlines/" + activeAirline.id + "/oil-contract-consideration?volume=" + volume + "&duration=" + duration
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	$('#oilContractPrice').text('$' + commaSeparateNumber(result.price))
	    	$('#oilContractInitialCost').text('$' + commaSeparateNumber(result.cost))
	    	$('#oilContractTerminationCost').text('$' + commaSeparateNumber(result.terminationPenalty))
	    	if (result.rejection) {
	    		$('#signOilContractButton').hide()
	    		$('#signOilContractRejection').text(result.rejection)
	    		$('#signOilContractRejectionDiv').show()
	    	} else {
	    		$('#signOilContractButton').show()
	    		$('#signOilContractRejectionDiv').hide()
	    	}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function addOilContract(volume, duration) {
	var url = "airlines/" + activeAirline.id + "/sign-oil-contract?volume=" + volume + "&duration=" + duration
	$.ajax({
		type: 'GET',
		url: url,
		contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	refreshPanels(activeAirline.id)
	    	showOilCanvas()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}



function loadExistingOilContracts() {
	var url = "airlines/" + activeAirline.id + "/oil-contracts"
	loadedContracts = {}
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(contracts) {
	    	updatedLoadedContracts(contracts)
	    	updateExistingContractsTable()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function editOilInventoryPolicy() {
	var url = "airlines/" + activeAirline.id + "/oil-inventory-options"
	loadedContracts = {}
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	$('#currentInventoryPolicyDiv').hide()
	    	var table = $("#editInventoryPolicyTable")
	    	table.children("div.table-row").remove()

	    	$.each(result.options, function(index, option) {
	    		var row = $("<div class='table-row'></div>")
	    		row.append("<div class='cell'>" + option.description + "</div>")
	    		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(option.price) + "</div>")
	    		if (!result.rejection) {
	    			row.append("<div class='cell' align='right'><img src='assets/images/icons/tick.png' title='Pick this policy' class='button' onclick='setOilInventoryPolicy(" + option.id + ")'/></div>")
	    		} else {
	    			row.append("<div class='cell' align='right'><img src='assets/images/icons/prohibition.png' class='button' title='" + result.rejection + "' onclick='exitOilInventoryPolicy()'/></div>")
	    		}
	    		
	    		table.append(row)
			});

	    	if (result.rejection) {
	    	    $('#editInventoryPolicyDiv .warning').text(result.rejection)
	    	    $('#editInventoryPolicyDiv .warning').show()
            } else if (result.warning) {
                $('#editInventoryPolicyDiv .warning').text(result.warning)
                $('#editInventoryPolicyDiv .warning').show()
            } else {
                $('#editInventoryPolicyDiv .warning').hide()
            }
	    	
	    	$('#editInventoryPolicyDiv').show()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function exitOilInventoryPolicy() {
	$('#currentInventoryPolicyDiv').show()
	$('#editInventoryPolicyDiv').hide()
}

function setOilInventoryPolicy(optionId) {
	var url = "airlines/" + activeAirline.id + "/set-oil-inventory-option?optionId=" + optionId
	$.ajax({
		type: 'GET',
		url: url,
		contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	loadOilDetails()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updatedLoadedContracts(contracts) {
	$.each(contracts, function(index, contract) {
		loadedContracts[contract.id] = contract
	});
}


function updateExistingContractsTable() {
	var contractsTable = $("#existingContractsTable")
	contractsTable.children("div.table-row").remove()
	
	$.each(loadedContracts, function(index, contract) {
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(contract.price) + "</div>")
		row.append("<div class='cell' align='right'>" + commaSeparateNumber(contract.volume) + " barrels</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(contract.cost) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(contract.terminationPenalty) + "</div>")
		row.append("<div class='cell' align='right'>" + contract.remainingDuration + " week(s)</div>")
		if (!contract.rejection) {
			row.append("<div class='cell'><img src='assets/images/icons/cross.png' title='Terminate contract' class='button' onclick='terminateContract(" + contract.id + ")'/></div>")
		} else {
			row.append("<div class='cell'><img src='assets/images/icons/cross-grey.png' title='" + contract.rejection + "'/></div>")
		}
		
		contractsTable.append(row)
	});
	
	if (jQuery.isEmptyObject(loadedContracts)) {
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell'></div>")
		contractsTable.append(row)
	}
}



function terminateContract(contractId) {
	var url = "airlines/" + activeAirline.id + "/oil-contracts/" + contractId
	$.ajax({
		type: 'DELETE',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    success: function() {
	    	refreshPanels(activeAirline.id)
	    	showOilCanvas()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function setSuggestedOilContractVolume() {
	$('#oilContractVolume').val(loadedSuggestion.suggestedBarrels)
	loadOilContractConsideration($('#oilContractVolume').val(), $('#oilContractDuration').val())
}

function setMaxOilContractVolume() {
	$('#oilContractVolume').val(loadedSuggestion.extraBarrelsAllowed)
	loadOilContractConsideration($('#oilContractVolume').val(), $('#oilContractDuration').val())
}

function setSuggestedOilContractDuration() {
	$('#oilContractDuration').val(loadedSuggestion.suggestedDuration)
	loadOilContractConsideration($('#oilContractVolume').val(), $('#oilContractDuration').val())
}

function setMaxOilContractDuration() {
	$('#oilContractDuration').val(loadedSuggestion.maxDuration)
	loadOilContractConsideration($('#oilContractVolume').val(), $('#oilContractDuration').val())
}
 


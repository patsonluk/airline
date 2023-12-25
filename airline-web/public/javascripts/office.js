var loadedIncomes = {}
var loadedCashFlows = {}
var officeSheetPage = 0;
var officePeriod;

var logoUploaderObj;
var liveryUploaderObj

$( document ).ready(function() {
	loadLogoTemplates()
//	$('#colorpicker1').farbtastic($('#logoColor1'));
//	$('#colorpicker2').farbtastic($('#logoColor2'));
	
//	var $box = $('#colorPicker1');
//    $box.tinycolorpicker();
//    var picker = $('#colorPicker1').data("plugin_tinycolorpicker");
//    picker.setColor("#000000");
//    $box.bind("change", function() {
//        generateLogoPreview()
//    });
//
//    $box = $('#colorPicker2');
//    $box.tinycolorpicker();
//    picker = $('#colorPicker2').data("plugin_tinycolorpicker");
//    picker.setColor("#FFFFFF");
//
//    $box.bind("change", function() {
//		generateLogoPreview()
//    });

    $('#logoModal .picker.color1').val("#000000")
    $('#logoModal .picker.color2').val("#FFFFFF")

    $('#logoModal .picker').change(function() {
        generateLogoPreview()
    });

    
    var $airlineColorPicker = $('#officeCanvas .airlineColor .picker')
    $airlineColorPicker.change(function() {
        setAirlineColor($(this).val())
    });



    populateBaseDetailsModal()
})

function showOfficeCanvas() {
	setActiveDiv($("#officeCanvas"))
	highlightTab($('.officeCanvasTab'))

	updateAirlineDetails()
	loadSheets();
	//updateAirlineDelegateStatus($('#officeCanvas .delegateStatus'))
	updateAirlineAssets()
	updateCampaignSummary()
	updateChampionedCountriesDetails()
	updateChampionedAirportsDetails()
	updateServiceFundingDetails()
	updateAirplaneRenewalDetails()
	updateAirlineBases()
	updateMaintenanceLevelDetails()
	updateAirlineColorPicker()
	updateResetAirlineInfo()
	updateHeadquartersMap($('#officeCanvas .headquartersMap'), activeAirline.id)
	updateLiveryInfo()
	loadSlogan(function(slogan) { $('#officeCanvas .slogan').val(slogan)})
}

function updateCampaignSummary() {
    $.ajax({
        type: 'GET',
        url: "airlines/" + activeAirline.id + "/campaigns?fullLoad=false",
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            var $campaignSummary = $("#officeCanvas .campaignSummary")
            $campaignSummary.children("div.table-row").remove()

            $.each(result, function(index, campaign) {
                var row = $("<div class='table-row'></div>")
                row.append("<div class='cell'>" + getCountryFlagImg(campaign.principalAirport.countryCode) + getAirportText(campaign.principalAirport.city, campaign.principalAirport.iata) + "</div>")
                row.append("<div class='cell'>" + campaign.population + "</div>")

                $campaignSummary.append(row)
            });
            if (result.length == 0) {
                $campaignSummary.append("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div></div>")
            }
        },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
    });
}

function updateAirlineAssets() {
    $.ajax({
        type: 'GET',
        url: "airlines/" + activeAirline.id + "/airport-assets",
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            var $assetList = $("#officeCanvas .assetList")
            $assetList.children("div.table-row").remove()

            $.each(result, function(index, asset) {
                var profit = asset.revenue - asset.expense
                var margin
                if (asset.expense == 0) {
                    margin = "-"
                } else {
                    margin = (profit * 100.0 / asset.revenue).toFixed(2)
                }
                var $row = $("<div class='table-row clickable'></div>")
                $row.append("<div class='cell'>" + getCountryFlagImg(asset.airport.countryCode) + asset.airport.iata + "</div>")
                $row.append("<div class='cell'>" + asset.name + "</div>")
                $row.append("<div class='cell' style='text-align: right;'>" + asset.level + "</div>")
                $row.append("<div class='cell' style='text-align: right;'>$" + commaSeparateNumber(profit) + "</div>")
                $row.append("<div class='cell' style='text-align: right;'>" + margin  + "%</div>")

                $row.click(function() {
                    showAssetModal(asset)
                    $("#airportAssetDetailsModal").data('postUpdateFunc', function() {
                        updateAirlineAssets()
                    })
                })
                $assetList.append($row)
            });
            if (result.length == 0) {
                $assetList.append("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div></div>")
            }
        },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
    });
}



function updateAirlineBases() {
    $('#officeCanvas .bases').children('.table-row').remove()

    var airlineId = activeAirline.id
    	var url = "airlines/" + airlineId + "/office-capacity"
        $.ajax({
    		type: 'GET',
    		url: url,
    	    contentType: 'application/json; charset=utf-8',
    	    dataType: 'json',
    	    success: function(officeCapacity) {
    	    	 $(activeAirline.baseAirports).each(function(index, base) {
                    var row = $("<div class='table-row clickable' data-link='airport' onclick='showAirportDetails(" + base.airportId+ ")'></div>")
                    if (base.headquarter) {
                        row.append($("<div class='cell'><img src='assets/images/icons/building-hedge.png' style='vertical-align:middle;'><span>(" + base.scale + ")</span></div><div class='cell'>" + getCountryFlagImg(base.countryCode) + getAirportText(base.city, base.airportCode) + "</div>"))

                    } else {
                        row.append($("<div class='cell'><img src='assets/images/icons/building-low.png' style='vertical-align:middle;'><span>(" + base.scale + ")</span></div><div class='cell'>" + getCountryFlagImg(base.countryCode) + getAirportText(base.city, base.airportCode) + "</div>"))
                    }
                    var capacityInfo = officeCapacity[base.airportId]
                    row.append($("<div class='cell'>" + capacityInfo.staffCapacity + "</div>"))
                    var $capacityDiv
                    if (capacityInfo.staffCapacity < capacityInfo.currentStaffRequired) {
                        $capacityDiv = $("<div class='cell fatal'>" + capacityInfo.currentStaffRequired + "</div>").appendTo(row)
                    } else {
                        $capacityDiv = $("<div class='cell'>" + capacityInfo.currentStaffRequired + "</div>").appendTo(row)
                    }
                    if (capacityInfo.currentStaffRequired != capacityInfo.futureStaffRequired) {
                        $capacityDiv.append("<span>(future : " + capacityInfo.futureStaffRequired + ")</span>")
                    }

                    var $overtimeCompensationDiv
                    if (capacityInfo.currentOvertimeCompensation == 0) {
                        $overtimeCompensationDiv = $("<div class='cell'>-</div>").appendTo(row)
                     } else {
                        $overtimeCompensationDiv = $("<div class='cell'>$" + commaSeparateNumber(capacityInfo.currentOvertimeCompensation) + "</div>").appendTo(row)
                     }

                     if (capacityInfo.currentOvertimeCompensation != capacityInfo.futureOvertimeCompensation) {
                        $overtimeCompensationDiv.append("<span>(future : $" + capacityInfo.futureOvertimeCompensation + ")</span>")
                     }

                    if (base.headquarter) {
                        $('#officeCanvas .bases .table-header').after(row)
                    } else {
                       $('#officeCanvas .bases').append(row)
                    }
                })
                if (!activeAirline.baseAirports || activeAirline.baseAirports.length == 0) {
                    $('#officeCanvas .bases').append("<div class='table-row'><div class='cell'></div></div>")
                }
                populateNavigation($('#officeCanvas .bases'))
    	    },
            error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    	});


}

function updateAirlineColorPicker() {
    var $colorPicker = $('#officeCanvas .airlineColor .picker')
	if (airlineColors[activeAirline.id]) {
		$colorPicker.val(airlineColors[activeAirline.id]);
    } else {
    	$colorPicker.val("#FFFFFF");
	}
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
	        $('#officeCanvas .reputationDetails').empty()
	        $('#officeCanvas .reputationDetails').append("<span>" + airline.reputation + " (" + airline.gradeDescription + ")</span>")
            var infoIcon = $('<span><img src="/assets/images/icons/information.png"></span>').appendTo($('#officeCanvas .reputationDetails'))
            var reputationHtml = $("<div></div>")
            if (airline.reputationBreakdowns.breakdowns.length == 0) { //not yet updated by sim on first creation
                reputationHtml.append("<div>Target Reputation: -</div>")
            } else {
                reputationHtml.append("<div>Target Reputation: " + airline.reputationBreakdowns.total.toFixed(2) + "</div>")
            }
            var breakdownList = $("<ul></ul>")
            $.each(airline.reputationBreakdowns.breakdowns, function(index, breakdown) {
                breakdownList.append("<li>" + breakdown.description + " : " + breakdown.value.toFixed(2) + "</li>")
            })
            reputationHtml.append(breakdownList)
            reputationHtml.append("<div class='remarks'>Current reputation adjusts slowly towards the target reputation</div>")
            reputationHtml.append("<div>Next Grade: " + airlineGradeLookup[airline.gradeValue] + "</div>")
            addTooltipHtml(infoIcon, reputationHtml, {'width' : '350px'})

            $('#officeCanvas .airlineName').text(airline.name)
            cancelAirlineRename()
            if (isPremium()) {
                if (airline.renameCooldown) {
                    disableButton($('#officeCanvas .airlineNameDisplaySpan .editButton'), "Cannot rename yet. Cooldown: " + toReadableDuration(airline.renameCooldown))
                } else {
                    enableButton($('#officeCanvas .airlineNameDisplaySpan .editButton'))
                }
            } else {
                disableButton($('#officeCanvas .airlineNameDisplaySpan .editButton'), "Airline rename is only available to Patreon members")
            }

	    	$('#airlineCode').text(airline.airlineCode)
	    	$('#airlineCodeInput').val(airline.airlineCode)
	    	$('#destinations').text(airline.destinations)
	    	$('#fleetSize').text(airline.fleetSize)
	    	$('#fleetAge').text(getYearMonthText(airline.fleetAge))
	    	$('#assets').text('$' + commaSeparateNumber(airline.assets))
	    	$('#officeCanvas .linkCount').text(airline.linkCount)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function loadSheets() {
	var airlineId = activeAirline.id
	//reset values
	loadedIncomes = {}
	loadedIncomes['WEEKLY'] = []
	loadedIncomes['MONTHLY'] = []
	loadedIncomes['YEARLY'] = []

	loadedCashFlows = {}
    loadedCashFlows['WEEKLY'] = []
    loadedCashFlows['MONTHLY'] = []
    loadedCashFlows['YEARLY'] = []

	officeSheetPage = 0
	officePeriod = 'WEEKLY'
	$('#officeCanvas select.period').val(officePeriod)

		
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/finances",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(data) {
	    	var airlineIncomes = data.incomes
	    	//group by period
	    	$.each(airlineIncomes, function(index, airlineIncome) {
	    		loadedIncomes[airlineIncome.period].push(airlineIncome)
	    	})
	    	
	    	var totalPages = loadedIncomes[officePeriod].length
	    	if (totalPages > 0) {
	    		officeSheetPage = totalPages - 1
	    		updateIncomeSheet(loadedIncomes[officePeriod][officeSheetPage])
	    	}
	    	
	    	updateIncomeChart()
	    	
	    	var airlineCashFlows = data.cashFlows
	    	//group by period
	    	$.each(airlineCashFlows, function(index, airlineCashFlow) {
	    		loadedCashFlows[airlineCashFlow.period].push(airlineCashFlow)
	    	})
	    	
	    	totalPages = loadedCashFlows[officePeriod].length
	    	if (totalPages > 0) {
	    		officeSheetPage = totalPages - 1
	    		updateCashFlowSheet(loadedCashFlows[officePeriod][officeSheetPage])
	    	}
	    	
	    	updateCashFlowChart()
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateIncomeChart() {
	plotIncomeChart(loadedIncomes[officePeriod], officePeriod, $("#officeCanvas #totalProfitChart"))
}

function updateCashFlowChart() {
	plotCashFlowChart(loadedCashFlows[officePeriod], officePeriod, $("#officeCanvas #totalCashFlowChart"))
}



function officeHistoryStep(step) {
    var type = $('#officeCanvas .sheetOptions').find('.cell.selected').data('type')
    var totalPages = loadedIncomes[officePeriod].length //income and cash flow should have same # of pages - just pick income arbitrarily
    if (officeSheetPage + step < 0) {
        officeSheetPage = 0
    } else if (officeSheetPage + step >= totalPages) {
        officeSheetPage = totalPages - 1
    } else {
        officeSheetPage = officeSheetPage + step
    }

    if (type === 'income') {
        updateIncomeSheet(loadedIncomes[officePeriod][officeSheetPage])
    } else if (type === 'cashFlow') {
    	updateCashFlowSheet(loadedCashFlows[officePeriod][officeSheetPage])
    }
}

function changeOfficePeriod(period, type) {
    var type = $('#officeCanvas .sheetOptions').find('.cell.selected').data('type')
    officePeriod = period
    if (type === 'income') {
        var totalPages = loadedIncomes[officePeriod].length
        officeSheetPage = totalPages - 1
        updateIncomeSheet(loadedIncomes[officePeriod][officeSheetPage])
        updateIncomeChart()
    } else if (type === 'cashFlow') {
        var totalPages = loadedCashFlows[officePeriod].length
    	officeSheetPage = totalPages - 1
    	updateCashFlowSheet(loadedCashFlows[officePeriod][officeSheetPage])
    	updateCashFlowChart()
    }
}

function updateIncomeSheet(airlineIncome) {
	if (airlineIncome) {
		var periodCount
		var inProgress
		if (airlineIncome.period == "WEEKLY") {
			periodCount= airlineIncome.cycle
		} else if (airlineIncome.period == "MONTHLY") {
			periodCount = Math.ceil(airlineIncome.cycle / 4)
			inProgress = (airlineIncome.cycle + 1) % 4
		} else if (airlineIncome.period == "YEARLY") {
			periodCount = Math.ceil(airlineIncome.cycle / 52)
			inProgress = (airlineIncome.cycle + 1) % 52
		}
		
		var cycleText = periodCount + (inProgress ? " (In Progress)" : "")
		
		$("#officeCycleText").text(cycleText)
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
        $("#linksLoungeCost").text('$' + commaSeparateNumber(airlineIncome.linksLoungeCost))
        $("#linksDepreciation").text('$' + commaSeparateNumber(airlineIncome.linksDepreciation))
        $("#linksDelayCompensation").text('$' + commaSeparateNumber(airlineIncome.linksDelayCompensation))
        $("#transactionsProfit").text('$' + commaSeparateNumber(airlineIncome.transactionsProfit))
        $("#transactionsRevenue").text('$' + commaSeparateNumber(airlineIncome.transactionsRevenue))
        $("#transactionsExpense").text('$' + commaSeparateNumber(airlineIncome.transactionsExpense))
        $("#transactionsCapitalGain").text('$' + commaSeparateNumber(airlineIncome.transactionsCapitalGain))
        //$("#transactionsCreateLink").text('$' + commaSeparateNumber(airlineIncome.transactionsCreateLink))
        $("#othersProfit").text('$' + commaSeparateNumber(airlineIncome.othersProfit))
        $("#othersRevenue").text('$' + commaSeparateNumber(airlineIncome.othersRevenue))
        $("#othersExpense").text('$' + commaSeparateNumber(airlineIncome.othersExpense))
        $("#othersLoanInterest").text('$' + commaSeparateNumber(airlineIncome.othersLoanInterest))
        $("#othersBaseUpkeep").text('$' + commaSeparateNumber(airlineIncome.othersBaseUpkeep))
        $("#othersOvertimeCompensation").text('$' + commaSeparateNumber(airlineIncome.othersOvertimeCompensation))
        $("#othersLoungeUpkeep").text('$' + commaSeparateNumber(airlineIncome.othersLoungeUpkeep))
        $("#othersLoungeCost").text('$' + commaSeparateNumber(airlineIncome.othersLoungeCost))
        $("#othersLoungeIncome").text('$' + commaSeparateNumber(airlineIncome.othersLoungeIncome))
        $("#othersAssetExpense").text('$' + commaSeparateNumber(airlineIncome.othersAssetExpense))
        $("#othersAssetRevenue").text('$' + commaSeparateNumber(airlineIncome.othersAssetRevenue))
        $("#othersServiceInvestment").text('$' + commaSeparateNumber(airlineIncome.othersServiceInvestment))
        $("#othersAdvertisement").text('$' + commaSeparateNumber(airlineIncome.othersAdvertisement))
        $("#othersFuelProfit").text('$' + commaSeparateNumber(airlineIncome.othersFuelProfit))
        $("#othersDepreciation").text('$' + commaSeparateNumber(airlineIncome.othersDepreciation))
	}
}


function changeCashFlowPeriod(period) {
	officePeriod = period
	var totalPages = loadedCashFlows[officePeriod].length
	officeSheetPage = totalPages - 1
	updateCashFlowSheet(loadedCashFlows[officePeriod][officeSheetPage])
	updateCashFlowChart()
}


function updateCashFlowSheet(airlineCashFlow) {
	if (airlineCashFlow) {
		var periodCount
		var inProgress
		if (airlineCashFlow.period == "WEEKLY") {
			periodCount= airlineCashFlow.cycle
		} else if (airlineCashFlow.period == "MONTHLY") {
			periodCount = Math.ceil(airlineCashFlow.cycle / 4)
			inProgress = (airlineCashFlow.cycle + 1) % 4
		} else if (airlineCashFlow.period == "YEARLY") {
			periodCount = Math.ceil(airlineCashFlow.cycle / 52)
			inProgress = (airlineCashFlow.cycle + 1) % 52
		}
		
		var cycleText = periodCount + (inProgress ? " (In Progress)" : "")
		
		$("#officeCycleText").text(cycleText)
		$("#cashFlowSheet .totalCashFlow").text('$' + commaSeparateNumber(airlineCashFlow.totalCashFlow))
        $("#cashFlowSheet .operation").text('$' + commaSeparateNumber(airlineCashFlow.operation))
        $("#cashFlowSheet .loanInterest").text('$' + commaSeparateNumber(airlineCashFlow.loanInterest))
        $("#cashFlowSheet .loanPrincipal").text('$' + commaSeparateNumber(airlineCashFlow.loanPrincipal))
        $("#cashFlowSheet .baseConstruction").text('$' + commaSeparateNumber(airlineCashFlow.baseConstruction))
        $("#cashFlowSheet .buyAirplane").text('$' + commaSeparateNumber(airlineCashFlow.buyAirplane))
        $("#cashFlowSheet .sellAirplane").text('$' + commaSeparateNumber(airlineCashFlow.sellAirplane))
        $("#cashFlowSheet .createLink").text('$' + commaSeparateNumber(airlineCashFlow.createLink))
        $("#cashFlowSheet .facilityConstruction").text('$' + commaSeparateNumber(airlineCashFlow.facilityConstruction))
        $("#cashFlowSheet .oilContract").text('$' + commaSeparateNumber(airlineCashFlow.oilContract))
        $("#cashFlowSheet .assetTransactions").text('$' + commaSeparateNumber(airlineCashFlow.assetTransactions))
	}
}

function setTargetServiceQuality(targetServiceQuality) {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/target-service-quality"
	if (!checkTargetServiceQualityInput(targetServiceQuality)) { //if invalid, then return
	    return;
	}

    var data = { "targetServiceQuality" : parseInt(targetServiceQuality) }
	$.ajax({
		type: 'PUT',
		url: url,
	    data: JSON.stringify(data),
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	activeAirline.targetServiceQuality = result.targetServiceQuality
	    	updateServiceFundingDetails()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function checkTargetServiceQualityInput(input) {
    var value = parseInt(input)
    if (value === undefined) {
        $("#serviceFundingInputSpan .warning").show()
        return false;
    } else {
        if (input < 0 || input > 100) {
            $("#serviceFundingInputSpan .warning").show()
            return false;
        } else { //ok
            $("#serviceFundingInputSpan .warning").hide()
            return true;
        }
    }
}

function setAirplaneRenewal(threshold) {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/airplane-renewal"
	var data
	if (threshold) {
		data = { "threshold" : parseInt(threshold) }
	} else {
		data = { "threshold" : -1 } //disable
	} 
		
	
		
	$.ajax({
		type: 'PUT',
		url: url,
	    data: JSON.stringify(data),
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	updateAirplaneRenewalDetails()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function editAirlineCode() {
	$('#airlineCodeDisplaySpan').hide()
	$('#airlineCodeInputSpan').show()
}

function validateAirlineCode(airlineCode) {
	if (/[^a-zA-Z]/.test(airlineCode) || airlineCode.length != 2) {
		$('#airlineCodeInputSpan .warning').show()
	} else {
		$('#airlineCodeInputSpan .warning').hide()
	}
}

function setAirlineCode(airlineCode) {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/airline-code"
    var data = { "airlineCode" : airlineCode }
	$.ajax({
		type: 'PUT',
		url: url,
	    data: JSON.stringify(data),
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airline) {
	    	activeAirline = airline
	    	$('#airlineCode').text(airline.airlineCode)
	    	$('#airlineCodeInputSpan').hide()
	    	$('#airlineCodeDisplaySpan').show()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    },
        beforeSend: function() {
            $('body .loadingSpinner').show()
        },
        complete: function(){
            $('body .loadingSpinner').hide()
        }
	});
}


function editAirlineName() {
	$('#officeCanvas .airlineNameDisplaySpan').hide()
	$('#officeCanvas .airlineNameInput').val(activeAirline.name)
	validateAirlineName(activeAirline.name)
	$('#officeCanvas .airlineNameInputSpan').show()
}

function validateAirlineName(airlineName) {
    $.ajax({
        type: 'GET',
        url: "signup/airline-name-check?airlineName=" + airlineName,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            if (result.ok) {
                enableButton('#officeCanvas .airlineNameInputSpan .confirm')
                $('#officeCanvas .airlineNameInputSpan .warning').hide()
            } else {
                disableButton('#officeCanvas .airlineNameInputSpan .confirm')
                $('#officeCanvas .airlineNameInputSpan .warning').text(result.rejection)
                $('#officeCanvas .airlineNameInputSpan .warning').show()
            }
        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    })
}

function confirmAirlineRename(airlineName) {
    promptConfirm("Change airline name to " + airlineName + " ? Can only rename every 30 days.", function() {
        var airlineId = activeAirline.id
        	var url = "airlines/" + airlineId + "/airline-name"
            var data = { "airlineName" : airlineName }
        	$.ajax({
        		type: 'PUT',
        		url: url,
        	    data: JSON.stringify(data),
        	    contentType: 'application/json; charset=utf-8',
        	    dataType: 'json',
        	    success: function(airline) {
        	    	loadUser(false)
        	    	showOfficeCanvas()
        	    },
                error: function(jqXHR, textStatus, errorThrown) {
        	            console.log(JSON.stringify(jqXHR));
        	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        	    },
                beforeSend: function() {
                    $('body .loadingSpinner').show()
                },
                complete: function(){
                    $('body .loadingSpinner').hide()
                }
        	});
    })
}

function cancelAirlineRename() {
    $('#officeCanvas .airlineNameDisplaySpan').show()
	$('#officeCanvas .airlineNameInputSpan').hide()
}





function loadLogoTemplates() {
	$('#logoTemplates').empty()
	$.ajax({
		type: 'GET',
		url: "logos/templates",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(templates) {
	    	//group by period
	    	$.each(templates, function(index, templateIndex) {
	    		$('#logoTemplates').append('<div style="padding: 3px; margin: 3px; float: left;" class="clickable" onclick="selectLogoTemplate(' + templateIndex + ')"><img src="logos/templates/' + templateIndex + '"></div>')
	    	})
	    	
	    	
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function editAirlineLogo() {
	var modal = $('#logoModal')
	$('#logoTemplateIndex').val(0)
	generateLogoPreview()
	modal.fadeIn(200)
}

function selectLogoTemplate(templateIndex) {
	$('#logoTemplateIndex').val(templateIndex)
	generateLogoPreview()
}

function generateLogoPreview() {
	var logoTemplate = $('#logoTemplateIndex').val()

	var color1 = $('#logoModal .picker.color1').val()
    var color2 = $('#logoModal .picker.color2').val()

	var url = "logos/preview?templateIndex=" + logoTemplate + "&color1=" + encodeURIComponent(color1) + "&color2=" + encodeURIComponent(color2)
	$('#logoPreview').empty();
	$('#logoPreview').append('<img src="' + url + '">')
}

function setAirlineLogo() {
	var logoTemplate = $('#logoTemplateIndex').val() 
	var color1 = $('#logoModal .picker.color1').val()
    var color2 = $('#logoModal .picker.color2').val()
	
	var url = "airlines/" + activeAirline.id + "/set-logo?templateIndex=" + logoTemplate + "&color1=" + encodeURIComponent(color1) + "&color2=" + encodeURIComponent(color2)
    $.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(dummy) {
	    	updateAirlineLogo()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function setAirlineColor(color) {
	var url = "airlines/" + activeAirline.id + "/set-color?color=" + encodeURIComponent(color)
    $.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(dummy) {
	    	updateAirlineColors()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function showUploadLogo() {
	if (activeAirline.reputation >= 40) {
		updateLogoUpload()
		$('#uploadLogoModal .uploadForbidden').hide()
		$('#uploadLogoModal .uploadPanel').show()
	} else {
		$('#uploadLogoModal .uploadForbidden .warning').text('You may only upload airline banner at Reputation 40 or above')
		$('#uploadLogoModal .uploadForbidden').show()
		$('#uploadLogoModal .uploadPanel').hide()
	}
	
	$('#uploadLogoModal').fadeIn(200)
}


function updateLogoUpload() {
	$('#uploadLogoModal .uploadPanel .warning').hide()
	if (logoUploaderObj) {
		logoUploaderObj.reset()
	}
	
	logoUploaderObj = $("#uploadLogoModal .uploadPanel .fileuploader").uploadFile({
		url:"airlines/" + activeAirline.id + "/logo",
		multiple:false,
		dragDrop:false,
		acceptFiles:"image/png",
		fileName:"logoFile",
		maxFileSize:100*1024,
		onSuccess:function(files,data,xhr,pd)
		{
			if (data.success) {
				$('#uploadLogoModal .uploadPanel .warning').hide()
				closeModal($('#uploadLogoModal'))
				updateAirlineLogo()
			} else if (data.error) {
				$('#uploadLogoModal .uploadPanel .warning').text(data.error)
				$('#uploadLogoModal .uploadPanel .warning').show()
			}
			
		}
	});
}

function updateLiveryInfo() {
    $('#officeCanvas img.livery').attr('src', 'airlines/' + activeAirline.id + "/livery?dummy=" + Math.random())
}

function showUploadLivery() {
	if (activeAirline.reputation >= 40) {
		updateLiveryUpload()
		$('#uploadLiveryModal .uploadForbidden').hide()
		$('#uploadLiveryModal .uploadPanel').show()
	} else {
		$('#uploadLiveryModal .uploadForbidden .warning').text('You may only upload airline livery at Reputation 40 or above')
		$('#uploadLiveryModal .uploadForbidden').show()
		$('#uploadLiveryModal .uploadPanel').hide()
	}

	$('#uploadLiveryModal').fadeIn(200)
}


function updateLiveryUpload() {
	$('#uploadLiveryModal .uploadPanel .warning').hide()
	if (liveryUploaderObj) {
		liveryUploaderObj.reset()
	}

	liveryUploaderObj = $("#uploadLiveryModal .uploadPanel .fileuploader").uploadFile({
		url:"airlines/" + activeAirline.id + "/livery",
		multiple:false,
		dragDrop:false,
		acceptFiles:"image/png",
		fileName:"liveryFile",
		maxFileSize:1 * 1024 * 1024,
		onSuccess:function(files,data,xhr,pd)
		{
			if (data.success) {
				$('#uploadLiveryModal .uploadPanel .warning').hide()
				closeModal($('#uploadLiveryModal'))
				updateLiveryInfo()
			} else if (data.error) {
				$('#uploadLiveryModal .uploadPanel .warning').text(data.error)
				$('#uploadLiveryModal .uploadPanel .warning').show()
			}

		}
	});
}

function deleteLivery() {
    var url = "airlines/" + activeAirline.id + "/livery"
    $.ajax({
		type: 'DELETE',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(dummy) {
	    	updateLiveryInfo()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function saveSlogan() {
    var url = "airlines/" + activeAirline.id + "/slogan"
    $.ajax({
		type: 'PUT',
		url: url,
		contentType: 'application/json; charset=utf-8',
        dataType: 'json',
	    data: JSON.stringify({ 'slogan' : $('#officeCanvas .slogan').val() }) ,


	    success: function(result) {
	    	$('#officeCanvas .slogan').val(result.slogan)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function loadSlogan(callback) {
    var url = "airlines/" + activeAirline.id + "/slogan"
    $.ajax({
		type: 'GET',
		url: url,
	    dataType: 'json',
	    success: function(result) {
	    	callback(result.slogan)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}




function editTargetServiceQuality() {
	$('#serviceFundingDisplaySpan').hide()
	$('#serviceFundingInputSpan').show()
}


function updateServiceFundingDetails() {
	$('#currentServiceQuality').text(activeAirline.serviceQuality)
	
	$('#targetServiceQuality').text(activeAirline.targetServiceQuality)
	$('#targetServiceQualityInput').val(activeAirline.targetServiceQuality)
	
	$('#serviceFundingDisplaySpan').show()
	$('#serviceFundingInputSpan').hide()

	$('#fundingProjection').text('...')
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/service-funding-projection",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	$('#fundingProjection').text(commaSeparateNumber(result.fundingProjection))
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	
}

function editAirplaneRenewal() {
	$('#airplaneRenewalDisplaySpan').hide()
	$('#airplaneRenewalInputSpan').show()
}


function updateAirplaneRenewalDetails() {
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/airplane-renewal",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airplaneRenewal) {
	    	if (airplaneRenewal.threshold) {
	    		$('#airplaneRenewal').text('Below ' + airplaneRenewal.threshold + "%")
	    		$('#airplaneRenewalInput').val(airplaneRenewal.threshold)
	    	} else {
	    		$('#airplaneRenewal').text('-')
	    		$('#airplaneRenewalInput').val(40)
	    	}
	    	$('#airplaneRenewalDisplaySpan').show()
	    	$('#airplaneRenewalInputSpan').hide()
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
    	    		var row = $("<div class='table-row clickable' data-link='country' onclick=\"showCountryView('" + country.countryCode + "');\"></div>")
    	    		row.append("<div class='cell'>" + getRankingImg(championDetails.ranking) + "</div>")
    	    		row.append("<div class='cell'>" + getCountryFlagImg(country.countryCode) + country.name + "</div>")
    	    		$('#championedCountriesList').append(row)
    	    	})

    	    	populateNavigation($('#championedCountriesList'))

    	    	if ($(championedCountries).length == 0) {
    	    		var row = $("<div class='table-row'></div>")
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

function updateChampionedAirportsDetails() {
	$('#championedAirportsList').children('div.table-row').remove()
	
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/championed-airports",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(championedInfo) {
	        $(championedInfo.airports).each(function(index, championDetails) {
	    		var row = $("<div class='table-row clickable' data-link='airport' onclick=\"showAirportDetails('" + championDetails.airportId + "');\"></div>")
	    		row.append("<div class='cell'>" + getRankingImg(championDetails.ranking) + "</div>")
	    		row.append("<div class='cell'>" + getCountryFlagImg(championDetails.countryCode) + championDetails.airportText + "</div>")
	    		row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(championDetails.loyalistCount) + "</div>")
	    		if (index < championedInfo.maxEntries) {
                    row.append("<div class='cell' style='text-align: right;'>" + championDetails.reputationBoost + "</div>")
                } else {
                    var $cell = $("<div class='cell' style='text-align: right; color:#888888;'><img src='assets/images/icons/exclamation-circle.png' style='vertical-align: middle;'/>" + championDetails.reputationBoost + "</div>")
                    row.attr("title", "Entry gives no boost beyond top " + championedInfo.maxEntries)
                    row.append($cell)
                }

	    		$('#championedAirportsList').append(row)

	    	})

	    	populateNavigation($('#championedAirportsList'))
	    	
	    	if ($(championedAirports).length == 0) {
	    		var row = $("<div class='table-row'></div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		$('#championedAirportsList').append(row)
	    	}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	
}


function selectSheet(tab, sheet) {
    tab.siblings().removeClass("selected")
	tab.addClass("selected")
    var type = tab.data('type')
    if (type === 'income') {
        updateIncomeSheet(loadedIncomes[officePeriod][officeSheetPage])
        updateIncomeChart()
    } else if (type === 'cashFlow') {
        updateCashFlowSheet(loadedCashFlows[officePeriod][officeSheetPage])
    	updateCashFlowChart()
    }

	sheet.siblings(".sheet").hide()
	sheet.show()
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


function updateResetAirlineInfo() {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/reset-consideration"
    $.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	if (result.rebuildRejection) {
	    		disableButton($("#officeCanvas .button.resetAirline.rebuild"), result.rebuildRejection)
	    	} else {
	    	    enableButton($("#officeCanvas .button.resetAirline.rebuild"))
	    	}

            if (result.bankruptRejection) {
                disableButton($("#officeCanvas .button.resetAirline.bankrupt"), result.bankruptRejection)
            } else {
                enableButton($("#officeCanvas .button.resetAirline.bankrupt"))
            }


	    	if (result.overall >= 0) {
	    		$("#officeCanvas #resetBalance").text(commaSeparateNumber(result.overall))
	    	} else {
	    		$("#officeCanvas #resetBalance").text('-' + commaSeparateNumber(result.overall * -1)) //to avoid the () negative number which could be confusing
	    	}
	    	
	    	$('.resetTooltip .airplanes').text(commaSeparateNumber(result.airplanes))
	    	$('.resetTooltip .bases').text(commaSeparateNumber(result.bases))
	    	$('.resetTooltip .assets').text(commaSeparateNumber(result.assets))
	    	$('.resetTooltip .loans').text(commaSeparateNumber(result.loans))
	    	$('.resetTooltip .oilContracts').text(commaSeparateNumber(result.oilContracts))
	    	$('.resetTooltip .cash').text(commaSeparateNumber(result.existingBalance))
	    	$('.resetTooltip .overall').text(commaSeparateNumber(result.overall))
	    	
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function resetAirline(keepAssets) {
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/reset?keepAssets=" + keepAssets,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function() {
	    	updateAllPanels(activeAirline.id)
	    	selectedLink = undefined
	    	showWorldMap()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	
}

function populateBaseDetailsModal() {
	$.ajax({
		type: 'GET',
		url: "airports/base/scale-details",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',

	    success: function(result) {
	        var $table = $('#baseDetailsModal .scaleDetails')
            var $remarks = $('#baseDetailsModal .groupRemarks')
            $remarks.empty()
            var index = 1
	        $.each(result.groupInfo, function(group, description) {
//                var $header = $table.find('.table-header .cell[data-group="' + group + '"]')
//                addTooltip($header, description, {'width' : '200px'})
                $remarks.append("<h5><span class='dot'>Freq Cap " + (index ++) +  " - " + description + "</span></h5>")
            })

	        $table.children("div.table-row").remove()
	    	$.each(result.scaleProgression, function(index, entry) {
	    	    var maxFrequency = entry.maxFrequency

                var row = $("<div class='table-row'></div>")
                row.append("<div class='cell'>" + entry.scale + "</div>")

                row.append("<div class='cell'>" + entry.headquartersStaffCapacity + "/" + entry.baseStaffCapacity + "</div>")
                row.append("<div class='cell'>" + maxFrequency.GROUP_1 + "</div>")
                row.append("<div class='cell'>" + maxFrequency.GROUP_2 + "</div>")
                row.append("<div class='cell'>" + maxFrequency.GROUP_3 + "</div>")
                row.attr('data-scale', entry.scale)

                $table.append(row)
	    	})
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function showBaseDetailsModal() {
    var scale = $('#baseDetailsModal').data('scale')
    $('#baseDetailsModal .table-row').removeClass('selected')

    if (scale) {
        var $selectRow = $('#baseDetailsModal').find('.table-row[data-scale="' + scale + '"]')
        $selectRow.addClass('selected')
    }

    $('#baseDetailsModal').fadeIn(500)

}
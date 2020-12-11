var loadedIncomes = {}
var incomePage = 0;
var incomePeriod;

var loadedCashFlows = {}
var cashFlowPage = 0;
var cashFlowPeriod;

var fileuploaderObj;
var airlineColorPicker;

$( document ).ready(function() {
	loadLogoTemplates()
//	$('#colorpicker1').farbtastic($('#logoColor1'));
//	$('#colorpicker2').farbtastic($('#logoColor2'));
	
	var $box = $('#colorPicker1');
    $box.tinycolorpicker();
    var picker = $('#colorPicker1').data("plugin_tinycolorpicker");
    picker.setColor("#000000");
    $box.bind("change", function() {
    			generateLogoPreview()
    	    });
    
    $box = $('#colorPicker2');
    $box.tinycolorpicker();
    picker = $('#colorPicker2').data("plugin_tinycolorpicker");
    picker.setColor("#FFFFFF");
    
    $box.bind("change", function() {
		generateLogoPreview()
    });
    
    
    $box = $('#colorPicker3');
    $box.tinycolorpicker();
    airlineColorPicker = $('#colorPicker3').data("plugin_tinycolorpicker");
     
    $box.bind("change", function() {
		setAirlineColor()
    });
})

function showOfficeCanvas() {
	setActiveDiv($("#officeCanvas"))
	highlightTab($('.officeCanvasTab'))
	
	updateAirlineDetails()
	loadSheets();
	updateAirlineDelegateStatus($('#officeCanvas .delegateStatus'))
	updateChampionedCountriesDetails()
	updateServiceFundingDetails()
	updateAirplaneRenewalDetails()
	updateAirlineBases()
	updateMaintenanceLevelDetails()
	updateAirlineColorPicker()
	updateResetAirlineInfo()
}
function refreshAirlineDelegateStatus($delegateStatusDiv, delegateInfo) {
    $delegateStatusDiv.empty()
    var availableDelegates = delegateInfo.availableCount

    //delegate info
    for (i = 0 ; i < availableDelegates; i ++) {
        var delegateIcon = $('<img src="assets/images/icons/user-silhouette-available.png" title="Available Delegate"/>')
        $delegateStatusDiv.append(delegateIcon)
    }

    $.each(delegateInfo.busyDelegates, function(index, busyDelegate) {
        var delegateIcon
        if (busyDelegate.completed) {
            delegateIcon = $('<img src="assets/images/icons/user-silhouette-unavailable.png" title="' + busyDelegate.coolDown + ' week(s) cool down remaining. Previous task : ' + busyDelegate.taskDescription + '"/>')
        } else {
            delegateIcon = $('<img src="assets/images/icons/user-silhouette-busy.png" title="Busy with task - ' + busyDelegate.taskDescription + '"/>')
        }
        $delegateStatusDiv.append(delegateIcon)
    })
}


function updateAirlineDelegateStatus($delegateStatusDiv, successFunction) {
    $delegateStatusDiv.empty()
    var airlineId = activeAirline.id

	$.ajax({
		type: 'GET',
		url: "delegates/airline/" + activeAirline.id,
		contentType: 'application/json; charset=utf-8',
		dataType: 'json',
	    success: function(delegateInfo) {
	        refreshAirlineDelegateStatus($delegateStatusDiv, delegateInfo)

            if (successFunction) {
                successFunction(delegateInfo)
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
                    var row = $("<div class='table-row'></div>")
                    if (base.headquarter) {
                        row.append($("<div class='cell'><img src='assets/images/icons/Ring-hedge.png' style='vertical-align:middle;'><span>(" + base.scale + ")</span></div><div class='cell'>" + getCountryFlagImg(base.countryCode) + getAirportText(base.city, base.airportCode) + "</div>"))

                    } else {
                        row.append($("<div class='cell'><img src='assets/images/icons/building-low.png' style='vertical-align:middle;'><span>(" + base.scale + ")</span></div><div class='cell'>" + getCountryFlagImg(base.countryCode) + getAirportText(base.city, base.airportCode) + "</div>"))
                    }
                    var capacityInfo = officeCapacity[base.airportId]
                    row.append($("<div class='cell'>" + capacityInfo.delegateCapacity + "</div>"))
                    row.append($("<div class='cell'>" + capacityInfo.staffCapacity + "</div>"))
                    if (capacityInfo.staffCapacity < capacityInfo.staffRequired) {
                        row.append($("<div class='cell fatal'>" + capacityInfo.staffRequired + "</div>"))
                    } else {
                        row.append($("<div class='cell'>" + capacityInfo.staffRequired + "</div>"))
                    }

                    if (capacityInfo.overtimeCompensation == 0) {
                        row.append($("<div class='cell'>-</div>"))
                     } else {
                        row.append($("<div class='cell'>$" + commaSeparateNumber(capacityInfo.overtimeCompensation) + "</div>"))
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
    	    },
            error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    	});


}

function updateAirlineColorPicker() {
	if (airlineColors[activeAirline.id]) {
		airlineColorPicker.setColor(airlineColors[activeAirline.id]);
    } else {
    	airlineColorPicker.setColor("#FFFFFF");
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
	incomePage = 0 
	incomePeriod = 'WEEKLY'
		
	loadedCashFlows = {}
	loadedCashFlows['WEEKLY'] = []
	loadedCashFlows['MONTHLY'] = []
	loadedCashFlows['YEARLY'] = []
	cashFlowPage = 0 
	cashFlowPeriod = 'WEEKLY'
		
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
	    	
	    	var totalPages = loadedIncomes[incomePeriod].length
	    	if (totalPages > 0) {
	    		incomePage = totalPages - 1
	    		updateIncomeSheet(loadedIncomes[incomePeriod][incomePage])
	    	}
	    	
	    	updateIncomeChart()
	    	
	    	var airlineCashFlows = data.cashFlows
	    	//group by period
	    	$.each(airlineCashFlows, function(index, airlineCashFlow) {
	    		loadedCashFlows[airlineCashFlow.period].push(airlineCashFlow)
	    	})
	    	
	    	totalPages = loadedCashFlows[cashFlowPeriod].length
	    	if (totalPages > 0) {
	    		cashFlowPage = totalPages - 1
	    		updateCashFlowSheet(loadedCashFlows[cashFlowPeriod][cashFlowPage])
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
	plotIncomeChart(loadedIncomes[incomePeriod], incomePeriod, $("#officeCanvas #totalProfitChart"))
}

function updateCashFlowChart() {
	plotCashFlowChart(loadedCashFlows[cashFlowPeriod], cashFlowPeriod, $("#officeCanvas #totalCashFlowChart"))
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
        $("#othersServiceInvestment").text('$' + commaSeparateNumber(airlineIncome.othersServiceInvestment))
        $("#othersMaintenanceInvestment").text('$' + commaSeparateNumber(airlineIncome.othersMaintenanceInvestment))
        $("#othersAdvertisement").text('$' + commaSeparateNumber(airlineIncome.othersAdvertisement))
        $("#othersFuelProfit").text('$' + commaSeparateNumber(airlineIncome.othersFuelProfit))
        $("#othersDepreciation").text('$' + commaSeparateNumber(airlineIncome.othersDepreciation))
	}
}

function cashFlowHistoryStep(step) {
	var totalPages = loadedCashFlows[cashFlowPeriod].length
	
	if (cashFlowPage + step < 0) {
		cashFlowPage = 0
	} else if (cashFlowPage + step >= totalPages) {
		cashFlowPage = totalPages - 1
	} else {
		cashFlowPage = cashFlowPage + step
	}
	
	updateCashFlowSheet(loadedCashFlows[cashFlowPeriod][cashFlowPage])
}

function changeCashFlowPeriod(period) {
	cashFlowPeriod = period
	var totalPages = loadedCashFlows[cashFlowPeriod].length
	cashFlowPage = totalPages - 1
	updateCashFlowSheet(loadedCashFlows[cashFlowPeriod][cashFlowPage])
	updateCashFlowChart()
}


function updateCashFlowSheet(airlineCashFlow) {
	if (airlineCashFlow) {
		var periodText
		var periodCount
		var inProgress
		if (airlineCashFlow.period == "WEEKLY") {
			periodText = "Week"
			periodCount= airlineCashFlow.cycle
		} else if (airlineCashFlow.period == "MONTHLY") {
			periodText = "Month"
			periodCount = Math.ceil(airlineCashFlow.cycle / 4)
			inProgress = (airlineCashFlow.cycle + 1) % 4
		} else if (airlineCashFlow.period == "YEARLY") {
			periodText = "Year"
			periodCount = Math.ceil(airlineCashFlow.cycle / 52)
			inProgress = (airlineCashFlow.cycle + 1) % 52
		}
		
		var cycleText = periodText + " " + periodCount + (inProgress ? " (In Progress)" : "")
		
		$("#cashFlowCycleText").text(cycleText)
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
	    }
	});
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
	var color1 = $('#colorPicker1 .colorInput').val()
	var color2 = $('#colorPicker2 .colorInput').val()
	
	var url = "logos/preview?templateIndex=" + logoTemplate + "&color1=" + encodeURIComponent(color1) + "&color2=" + encodeURIComponent(color2)
	$('#logoPreview').empty();
	$('#logoPreview').append('<img src="' + url + '">')
}

function setAirlineLogo() {
	var logoTemplate = $('#logoTemplateIndex').val() 
	var color1 = $('#colorPicker1 .colorInput').val()
	var color2 = $('#colorPicker2 .colorInput').val()
	
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

function setAirlineColor() {
	var color = $('#colorPicker3 .colorInput').val()
	
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
		$('#uploadLogoPanelForbidden').hide()
		$('#uploadLogoPanel').show()
	} else {
		$('#uploadLogoPanelForbidden .warning').text('You may only upload airline banner at Reputation 40 or above')
		$('#uploadLogoPanelForbidden').show()
		$('#uploadLogoPanel').hide()
	}
	
	$('#uploadLogoModal').fadeIn(200)
}

function updateLogoUpload() {
	$('#uploadLogoPanel .warning').hide()
	if (fileuploaderObj) {
		fileuploaderObj.reset()
	}
	
	fileuploaderObj = $("#fileuploader").uploadFile({
		url:"airlines/" + activeAirline.id + "/logo",
		multiple:false,
		dragDrop:false,
		acceptFiles:"image/png",
		fileName:"logoFile",
		maxFileSize:100*1024,
		onSuccess:function(files,data,xhr,pd)
		{
			if (data.success) {
				$('#uploadLogoPanel .warning').hide()
				closeModal($('#uploadLogoModal'))
				updateAirlineLogo()
			} else if (data.error) {
				$('#uploadLogoPanel .warning').text(data.error)	
				$('#uploadLogoPanel .warning').show()
			}
			
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
	    		var row = $("<div class='table-row clickable' onclick=\"showCountryView('" + country.countryCode + "');\"></div>")
	    		row.append("<div class='cell'>" + getRankingImg(championDetails.ranking) + "</div>")
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


function selectSheet(tab, sheet) {
	tab.siblings(".selection").removeClass("selected")
	tab.addClass("selected")
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
	    	if (result.rejection) {
	    		$("#officeCanvas .button.resetAirline").hide()
	    		$("#officeCanvas .resetAirlineRejection").text(result.rejection)
	    		$("#officeCanvas .resetAirlineRejectionDiv").show()
	    	} else {
	    		$("#officeCanvas .button.resetAirline").show()
	    		$("#officeCanvas .resetAirlineRejectionDiv").hide()
	    	} 
	    	if (result.overall >= 0) {
	    		$("#officeCanvas #resetBalance").text(commaSeparateNumber(result.overall))
	    	} else {
	    		$("#officeCanvas #resetBalance").text('-' + commaSeparateNumber(result.overall * -1)) //to avoid the () negative number which could be confusing
	    	}
	    	
	    	$('.resetTooltip .airplanes').text(commaSeparateNumber(result.airplanes))
	    	$('.resetTooltip .bases').text(commaSeparateNumber(result.bases))
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


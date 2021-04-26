var loadedLoans = {}

function showBankCanvas() {
	setActiveDiv($("#bankCanvas"))
	highlightTab($('.bankCanvasTab'))
	loadInterestRateChart()
	loadNewLoanDetails() 
    loadOutstandingLoans()
}

function loadInterestRateChart() {
	var url = "loan-interest-rates"
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(oilPrices) {
	    	plotLoanInterestRatesChart(oilPrices, $("#bankCanvas #loanInterestRateChart"))
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});


}

function loadNewLoanDetails() {
	$('#newLoanMaxAmount').text('-')
	var url = "airlines/" + activeAirline.id + "/max-loan"
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	    	$("#newLoanOptionsTable").children("div.table-row").remove()
	    	
	    	if (result.rejection) {
		    	$('#newLoanMaxAmount').text('$0')
		    	$("#newLoanAmountRow").hide()
		    	$('#newLoanAmount').val(0)
		    	$("#newLoanRejection").text(result.rejection)
		    	$("#newLoanRejectionSpan").show()
		    	$("#newLoanOptions").hide()
	    	} else {
	    		$('#newLoanMaxAmount').text('$' + commaSeparateNumber(result.maxAmount))
	    		$("#newLoanAmountRow").show()
		    	$('#newLoanAmount').val(result.maxAmount)
		    	$("#newLoanRejection").text("")
		    	$("#newLoanRejectionSpan").hide()
		    	loadLoanOptions(result.maxAmount)
		    	$("#newLoanOptions").show()
	    	}
	    	
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function loadLoanOptions(amount) {
	var url = "airlines/" + activeAirline.id + "/loan-options?loanAmount=" + amount
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(loanOptions) {
	    	updateNewLoanOptionsTable(loanOptions)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateNewLoanOptionsTable(loanOptions) {
	var optionsTable = $("#newLoanOptionsTable")
	optionsTable.children(".table-row").remove()
	$.each(loanOptions, function(index, loanOption) {
		var weeklyPayment = Math.ceil((loanOption.borrowedAmount + loanOption.interest) / loanOption.loanTerm)
		var interestRate = loanOption.interestRate * 100
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loanOption.borrowedAmount) + "</div>")
		row.append("<div class='cell' align='right'>" + interestRate.toFixed(1)  + "%</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loanOption.interest) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loanOption.remainingAmount) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(weeklyPayment) + " for " + loanOption.loanTerm + " weeks</div>")

		var loanFunction = function() {
		    var action = function() {
		        takeoutLoan(loanOption.borrowedAmount, loanOption.loanTerm)
            }

		    promptConfirm("Confirm taking out this loan with term " + loanOption.loanTerm + " weeks?", action)

		}
		var cell = $("<div class='cell'><img src='assets/images/icons/money--plus.png' title='Borrow with this Term' class='button'></div>").appendTo(row)
		cell.find('.button').click(loanFunction)
		optionsTable.append(row)
	});
}

function takeoutLoan(amount, term) {
	var url = "airlines/" + activeAirline.id + "/loans"
	$.ajax({
		type: 'POST',
		url: url,
		data: { 'requestedAmount' : parseInt(amount), 'requestedTerm': parseInt(term)} ,
	    dataType: 'json',
	    success: function(loan) {
	    	refreshPanels(activeAirline.id)
	    	showBankCanvas()
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


function loadOutstandingLoans() {
	$("#existingLoanDetails").fadeOut(200)
	loadOutstandingLoansTable()
}

function loadOutstandingLoansTable() {
	var url = "airlines/" + activeAirline.id + "/loans"
	loadedLoans = {}
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(loans) {
	    	updatedLoadedLoans(loans)
	    	updateOutstandingLoansTable()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updatedLoadedLoans(loans) {
	$.each(loans, function(index, loan) {
		loadedLoans[loan.id] = loan
	});
}


function updateOutstandingLoansTable() {
	var loansTable = $("#outstandingLoansTable")
	loansTable.children("div.table-row").remove()
	
	$.each(loadedLoans, function(index, loan) {
	    var interestRate = loan.interestRate * 100
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loan.borrowedAmount) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loan.interest) + "</div>")
		row.append("<div class='cell' align='right'>" + interestRate.toFixed(1) + "%</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loan.remainingAmount) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loan.weeklyPayment) + " remaining " + loan.remainingTerm + " week(s)</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loan.earlyRepaymentFee) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loan.earlyRepayment) + "</div>")
		if (loan.earlyRepayment <= activeAirline.balance) {
			row.append("<div class='cell'><img src='assets/images/icons/money--minus.png' title='Pay off early' class='button' onclick='repayLoan(" + loan.id + ")'/></div>")
		} else {
			row.append("<div class='cell'></div>")
		}
		
		loansTable.append(row)
	});
	
	if (jQuery.isEmptyObject(loadedLoans)) {
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell'></div>")
		loansTable.append(row)
	}
}



function repayLoan(loanId) {
	var url = "airlines/" + activeAirline.id + "/loans/" + loanId
	$.ajax({
		type: 'DELETE',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    success: function() {
	    	refreshPanels(activeAirline.id)
	    	showBankCanvas()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


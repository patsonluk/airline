var loadedLoans = {}

function showBankCanvas() {
	setActiveDiv($("#bankCanvas"))
	highlightTab($('#bankCanvasTab'))
	loadNewLoanDetails() 
    loadOutstandingLoans()
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
	    	var optionsTable = $("#newLoanOptionsTable")
	    	optionsTable.children("div.table-row").remove()
	    	
	    	if (result.maxAmount == 0) {
		    	$('#newLoanMaxAmount').text('$' + commaSeparateNumber(result.maxAmount))
		    	$("#newLoanAmount").prop("disabled", true);
		    	$('#newLoanAmount').val(result.maxAmount)
	    	} else {
	    		$('#newLoanMaxAmount').text('$' + commaSeparateNumber(result.maxAmount))
	    		$("#newLoanAmount").prop("disabled", false);
		    	$('#newLoanAmount').val(result.maxAmount)
	    		loadLoanOptions(result.maxAmount)	
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
	$.each(loanOptions, function(index, loanOption) {
		var weeklyPayment = Math.ceil((loanOption.borrowedAmount + loanOption.interest) / loanOption.loanTerm)
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loanOption.borrowedAmount) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loanOption.interest) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loanOption.remainingAmount) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(weeklyPayment) + " for " + loanOption.loanTerm + " weeks</div>")
		row.append("<div class='cell'><img src='assets/images/icons/money--plus.png' title='Borrow with this Term' class='button' onclick='takeoutLoan(" + loanOption.borrowedAmount + "," + loanOption.loanTerm + ")'/></div>")
		
		
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
		var weeklyPayment = Math.ceil((loan.borrowedAmount + loan.interest) / loan.loanTerm)
		var remainingTerm = Math.ceil(loan.remainingAmount / weeklyPayment)
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loan.borrowedAmount) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loan.interest) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(loan.remainingAmount) + "</div>")
		row.append("<div class='cell' align='right'>" + '$' + commaSeparateNumber(weeklyPayment) + " remaining " + remainingTerm + " week(s)</div>")
		if (loan.remainingAmount <= activeAirline.balance) {
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


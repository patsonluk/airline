var loadedLogs = []
var loadedAlerts = []


function showLogCanvas() {
	setActiveDiv($("#logCanvas"))
	highlightTab($('.logCanvasTab'))
	loadAllLogs()
	loadAllAlerts()
}

function loadAllLogs() {
	var url = "airlines/" + activeAirline.id + "/logs"
	
	loadedLogs = []

	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(logs) {
	    	loadedLogs = logs
	    	var selectedSortHeader = $('#logTableSortHeader .table-header .cell.selected')
	    	updateLogTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function loadAllAlerts() {
	var url = "airlines/" + activeAirline.id + "/alerts"

	loadedAlerts = []

	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(alerts) {
	    	loadedAlerts = alerts
	    	var selectedSortHeader = $('#alertTableSortHeader .table-header .cell.selected')
	    	updateAlertTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateLogTable(sortProperty, sortOrder) {
	var logTable = $("#logCanvas #logTable")
	
	logTable.children("div.table-row").remove()
	
	//sort the list
	loadedLogs.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	$.each(loadedLogs, function(index, log) {
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell'>" + log.cycleAgo + " week(s) ago</div>")
		row.append("<div class='cell'>" + log.severityText + "</div>")
		row.append("<div class='cell'>" + log.categoryText + "</div>")
		row.append("<div class='cell'>" + getAirlineLogoImg(log.airlineId) + log.airlineName + "</div>")
		row.append("<div class='cell'>" + log.message + "</div>")

		logTable.append(row)
	});

	if (loadedLogs.length == 0) {
	    logTable.append("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div></div>")
	}
}

function updateAlertTable(sortProperty, sortOrder) {
	var alertTable = $("#logCanvas #alertTable")

	alertTable.children("div.table-row").remove()

	//sort the list
	loadedAlerts.sort(sortByProperty(sortProperty, sortOrder == "ascending"))

	$.each(loadedAlerts, function(index, alert) {
		var row = $("<div class='table-row clickable'  onclick=' showLinksDetails(); refreshLinkDetails(" + alert.targetId + ");'></div>")
		row.append("<div class='cell'>" + alert.cycle + "</div>")
		row.append("<div class='cell'>" + alert.categoryText + "</div>")
		row.append("<div class='cell'>" + alert.duration + " week(s)</div>")
		row.append("<div class='cell'>" + getAirlineLogoImg(alert.airlineId) + alert.airlineName + "</div>")
		row.append("<div class='cell'>" + alert.message + "</div>")

		alertTable.append(row)
	});

    if (loadedAlerts.length == 0) {
    	    alertTable.append("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div></div>")
    }
}

function toggleTableSortOrder(sortHeader, updateTableFunction) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}
	
	sortHeader.siblings().removeClass("selected")
	sortHeader.addClass("selected")
	
	updateTableFunction(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
}

function toggleLogTableSortOrder(sortHeader) {
    toggleTableSortOrder(sortHeader, updateLogTable)
}
function toggleAlertTableSortOrder(sortHeader) {
    toggleTableSortOrder(sortHeader, updateAlertTable)
}


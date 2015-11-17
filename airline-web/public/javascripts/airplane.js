var loadedModels = {}

$( document ).ready(function() {
	loadAirplaneModels()
})

function loadAirplaneModels() {
	$.ajax({
		type: 'GET',
		url: "http://localhost:9001/airplane-models",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(models) {
	    	$.each(models, function( key, model ) {
	    		$("#airplaneModelOption").append($("<option></option>").attr("value", model.id).text(model.name));
	    		loadedModels[model.id] = model
	  		});
	    	updateModelInfo($('#modelInfo'))
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function addAirplane(modelId, quantity, airlineId) {
	var url = "http://localhost:9001/airplanes?model=" + modelId + "&quantity=" + quantity + "&airlineId=" + airlineId 
	$.ajax({
		type: 'PUT',
		data: JSON.stringify({}),
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(response) {
	    	updateAirplaneList(airlineId, $('#airplaneList'))
	    	updateBalance(airlineId)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function sellAirplane(airlineId, airplaneId) {
	$.ajax({
		type: 'DELETE',
		data: JSON.stringify({}),
		url: "http://localhost:9001/airlines/" + airlineId + "/airplanes/" + airplaneId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(response) {
	    	updateAllPanels(airlineId)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function updateModelInfo() {
	var modelId = $("#airplaneModelOption").val()
	$('#modelInfo').empty()
	model = loadedModels[modelId]
	$('#modelInfo').append($("<div>" +
			"Model : " + model.name 
			+ "<br/>Capacity  : " + model.capacity 
			+ "<br/>Fuel Burn : " + model.fuelBurn
			+ "<br/>Speed : " + model.speed
			+ "<br/>Range : " + model.range
			+ "<br/>Price : " + model.price +
			"</div>")); 
	  		
}

function updateAirplaneList(airlineId, airplaneList) {
	airplaneList.empty()
	$.ajax({
		type: 'GET',
		url: "http://localhost:9001/airlines/"+ airlineId + "/airplanes?getAssignedLink=true",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airplanes) {
	    	$.each(airplanes, function( key, airplane ) {
	    		airplaneList.append($("<a href='javascript:void(0)' onclick='loadAirplaneDetails(" + airlineId + "," + airplane.id + ")'></a>").text(airplane.name + ' (id ' + airplane.id + ')'))
				airplaneList.append($("<br/>"))
	  		});
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function loadAirplaneDetails(airlineId, airplaneId) {
	$("#linkDetails").hide()
	$('#airplaneDetails').show()
	
	$("#actionAirplaneId").val(airplaneId)
	//load link
	$.ajax({
		type: 'GET',
		url: "http://localhost:9001/airlines/" + airlineId + "/airplanes/" + airplaneId + "?getAssignedLink=true",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airplane) {
	    	$("#airplaneDetailsId").text(airplane.id)
    		$("#airplaneDetailsName").text(airplane.name)
	    	$("#airplaneDetailsCapacity").text(airplane.capacity)
	    	$("#airplaneDetailsFuelBurn").text(airplane.fuelBurn)
	    	$("#airplaneDetailsSpeed").text(airplane.speed + " km / hr")
	    	$("#airplaneDetailsRange").text(airplane.range + " km")
	    	$("#airplaneDetailsCondition").text(airplane.condition + "%")
	    	$("#airplaneDetailsAge").text(airplane.age + "week(s)")
	    	$("#airplaneDetailsValue").text("$" + airplane.value)
	    	if (airplane.link) {
	    		$("#airplaneDetailsLink").text(airplane.link.fromAirportName + "(" + airplane.link.fromAirportCity + ") => " + airplane.link.toAirportName + "(" + airplane.link.toAirportCity + ")")
	    	} else {
	    		$("#airplaneDetailsLink").text("-")
	    	}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}
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


function updateModelInfo(modelInfoDiv, modelId) {
	modelInfoDiv.empty()
	model = loadedModels[modelId]
	modelInfoDiv.append($("<div>" +
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
		url: "http://localhost:9001/airplanes?airlineId=" + airlineId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airplanes) {
	    	$.each(airplanes, function( key, airplane ) {
	    		airplaneList.append($("<h5></h5>").text(airplane.name + ' (id ' + airplane.id + ')'))
	  		});
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}
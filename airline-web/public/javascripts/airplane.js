var loadedModels = {}

$( document ).ready(function() {
	loadAirplaneModels()
})

function loadAirplaneModels() {
	$.ajax({
		type: 'GET',
		url: "airplane-models",
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


function addAirplane(modelId, quantity) {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/airplanes?model=" + modelId + "&quantity=" + quantity + "&airlineId=" + airlineId 
	$.ajax({
		type: 'PUT',
		data: JSON.stringify({}),
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(response) {
	    	refreshPanels(airlineId)
	    	updateAirplaneList()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function sellAirplane(airplaneId) {
	$.ajax({
		type: 'DELETE',
		url: "airlines/" + activeAirline.id + "/airplanes/" + airplaneId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(response) {
	    	refreshPanels(activeAirline.id)
	    	updateAirplaneList()
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

function updateAirplaneList() {
	var airplaneList = $("#airplaneList")
	var selectedModelId //check previously selected model id
	if ($("#airplaneList a.selected").length !== 0) {
		selectedModelId = $("#airplaneList a.selected").data("modelId")
	}
	
	airplaneList.empty()
	var airlineId = activeAirline.id
	$.ajax({
		type: 'GET',
		url: "airlines/"+ airlineId + "/airplanes?simpleResult=true",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(models) { //a list of model with airplanes
	    	$.each(models, function(key, model) {
	    		var label = model.name + " (assigned: " + model.assignedAirplanes.length + " free: " + model.freeAirplanes.length + ")"
	    		var aLink = $("<a href='javascript:void(0)' data-model-id='" + model.id + "' onclick='selectAirplaneModel(this.modelInfo)'></a>").text(label)
	    		airplaneList.append(aLink)
	    		aLink.get(0).modelInfo = model //tag the info to the element
	    		airplaneList.append($("<br/>"))
	    		
	    		if (selectedModelId == model.id) {
	    			selectAirplaneModel(model)
	    		}
	  		});
	    	
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}
function selectAirplaneModel(model) {
	$("#airplaneList a.selected").removeClass("selected")
	//highlight the selected model
	$("#airplaneList a[data-model-id='" + model.id +"']").addClass("selected")
	
	//expand the airplane list under this model
	expandAirplaneList(model)
}

function expandAirplaneList(modelInfo) {
	var airplaneList = $("#expandedAirplaneList")
	airplaneList.empty()
	$.each(modelInfo.assignedAirplanes, function( key, airplaneId ) {
		airplaneList.append($("<a href='javascript:void(0)' onclick='loadAirplaneDetails(" + airplaneId + ")'></a>").text(modelInfo.name + ' (id ' + airplaneId + ')'))
		airplaneList.append($("<br/>"))
	});
	
	$.each(modelInfo.freeAirplanes, function( key, airplaneId ) {
		airplaneList.append($("<a href='javascript:void(0)' onclick='loadAirplaneDetails(" + airplaneId + ")'></a>").text(modelInfo.name + ' (id ' + airplaneId + ')'))
		airplaneList.append($("<br/>"))
	});
}

function loadAirplaneDetails(airplaneId) {
	var airlineId = activeAirline.id 
	$("#actionAirplaneId").val(airplaneId)
	//load link
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/airplanes/" + airplaneId + "?getAssignedLink=true",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airplane) {
	    	$("#airplaneDetailsId").text(airplane.id)
    		$("#airplaneDetailsName").text(airplane.name)
	    	$("#airplaneDetailsCapacity").text(airplane.capacity)
	    	$("#airplaneDetailsFuelBurn").text(airplane.fuelBurn)
	    	$("#airplaneDetailsSpeed").text(airplane.speed + " km / hr")
	    	$("#airplaneDetailsRange").text(airplane.range + " km")
	    	$("#airplaneDetailsCondition").text(airplane.condition.toFixed(2) + "%")
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
var loadedModels = {}
var sortedModels = []
var selectedModelId
var selectedModel

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
	    		loadedModels[model.id] = model
	  		});
	    	//updateModelInfo($('#modelInfo'))
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function addAirplane(modelId, quantity, fromPlanLink) {
	fromPlanLink = fromPlanLink || false
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
	    	if (fromPlanLink) {
	    		planLink($("#planLinkFromAirportId").val(), $("#planLinkToAirportId").val())
	    		$("#planLinkModelSelect").data('explicitId', modelId) //force the plan link to use this value after buying a plane
	    	} else {
	    		showAirplaneCanvas()
	    	}
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
	    	showAirplaneCanvas()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function replaceAirplane(airplaneId) {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/airplanes/" + airplaneId 
	$.ajax({
		type: 'PUT',
		data: JSON.stringify({}),
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(response) {
	    	refreshPanels(airlineId)
	    	showAirplaneCanvas()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function updateModelInfo(modelId) {
	model = loadedModels[modelId]
	$('#airplaneModelDetails #selectedModel').val(modelId)
	$('#airplaneModelDetails #modelName').text(model.name)
	$('#airplaneModelDetails #capacity').text(model.capacity)
	$('#airplaneModelDetails #fuelBurn').text(model.fuelBurn)
	$('#airplaneModelDetails #range').text(model.range + "km")
	$('#airplaneModelDetails #speed').text(model.speed + "km/h")
	$('#airplaneModelDetails #price').text("$" + commaSeparateNumber(model.price))
	
	if (model.constructionTime == 0) {
		$('#airplaneModelDetails .delivery').text("immediate")
		$('#airplaneModelDetails .delivery').removeClass('warning')
		$('#airplaneModelDetails .add').text('Purchase')
	} else {
		$('#airplaneModelDetails .delivery').text(model.constructionTime + " weeks")
		$('#airplaneModelDetails .delivery').addClass('warning')
		$('#airplaneModelDetails .add').text('Place Order')
	}
	
	if (activeAirline.balance < model.price) {
		$('#airplaneModelDetails .rejection').show()
		$('#airplaneModelDetails .add').hide()
	} else {
		$('#airplaneModelDetails .rejection').hide()
		$('#airplaneModelDetails .add').show()
	}
	
}

function selectAirplaneModel(model) {
	selectedModel = model
	$("#airplaneCanvas #airplaneModelList li.selected").removeClass("selected")
	//highlight the selected model
	$("#airplaneCanvas #airplaneModelList li[data-model-id='" + model.id +"']").addClass("selected")
	
	//expand the airplane list under this model
	showAirplaneInventory(model)
	//show basic airplane model details
	//model = loadedModels[modelId]
	$('#airplaneCanvas #selectedModel').val(model.id)
	$('#airplaneCanvas #modelName').text(model.name)
	$('#airplaneCanvas #capacity').text(model.capacity)
	$('#airplaneCanvas #fuelBurn').text(model.fuelBurn)
	$('#airplaneCanvas #range').text(model.range + " km")
	$('#airplaneCanvas #speed').text(model.speed + " km/h")
	$('#airplaneCanvas #price').text("$" + commaSeparateNumber(model.price))
	if (model.constructionTime == 0) {
		$('#airplaneCanvas .delivery').text("immediate")
		$('#airplaneCanvas .delivery').removeClass('warning')
		$('#airplaneCanvas .add').text('Purchase')
	} else {
		$('#airplaneCanvas .delivery').text(model.constructionTime + " weeks")
		$('#airplaneCanvas .delivery').addClass('warning')
		$('#airplaneCanvas .add').text('Place Order')
	}
	if (activeAirline.balance < model.price) {
		$('#airplaneCanvas .rejection').show()
		$('#airplaneCanvas .add').hide()
	} else {
		$('#airplaneCanvas .rejection').hide()
		$('#airplaneCanvas .add').show()
	}
	
	$('#airplaneCanvas #airplaneModelDetail').fadeIn(200)
	//hide owned model details
	$('#airplaneCanvas #ownedAirplaneDetail').fadeOut(200)
}

function showAirplaneInventory(modelInfo) {
	var airplaneInventoryList = $("#airplaneCanvas #airplaneInventoryList")
	airplaneInventoryList.empty()
	if (modelInfo.assignedAirplanes || modelInfo.availableAirplanes || modelInfo.constructingAirplanes) {
		$.each(modelInfo.assignedAirplanes, function( key, airplane ) {
			var airplaneId = airplane.id
			var li = $("<li class='row clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this))'>" + modelInfo.name + " (id " + airplaneId + ")</li>").appendTo(airplaneInventoryList)
			insertAirplaneConditionWarning(li, airplane.condition, modelInfo.badConditionThreshold)
		});
		
		$.each(modelInfo.availableAirplanes, function( key, airplane ) {
			var airplaneId = airplane.id
			var li = $("<li style='color: #2988c3;' class='row clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this))'>" + modelInfo.name + " (id " + airplaneId + ")</li>").appendTo(airplaneInventoryList)
			insertAirplaneConditionWarning(li, airplane.condition, modelInfo.badConditionThreshold)
		});
		
		$.each(modelInfo.constructingAirplanes, function( key, airplane ) {
			var airplaneId = airplane.id
			var li = $("<li style='color: #ee9a29;' class='row clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this))'>" + modelInfo.name + " (id " + airplaneId + ")</li>").appendTo(airplaneInventoryList)
			insertAirplaneConditionWarning(li, airplane.condition, modelInfo.badConditionThreshold)
		});
	}
}

function insertAirplaneConditionWarning(entry, condition, badConditionThreshold) {
	if (condition < badConditionThreshold) {
		entry.append("<img src='assets/images/icons/12px/exclamation.png'/>")
	}
}

function loadOwnedAirplaneDetails(airplaneId, selectedItem) {
	$("#airplaneInventoryList li.selected").removeClass("selected")
	//highlight the selected model
	selectedItem.addClass("selected")
	
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
    		$("#airplaneDetailsCondition").text(airplane.condition.toFixed(2) + "%")
    		$("#airplaneDetailsCondition").removeClass("warning fatal")
    		if (airplane.condition < selectedModel.criticalConditionThreshold) {
    			$("#airplaneDetailsCondition").addClass("fatal")
    		} else if (airplane.condition < selectedModel.badConditionThreshold) {
    			$("#airplaneDetailsCondition").addClass("warning")
    		}
    		
    		if (airplane.age >= 0) {
    			$("#airplaneDetailsAge").text(airplane.age + "week(s)")
    			$("#airplaneDetailsAgeRow").show()
    			$("#airplaneDetailsDeliveryRow").hide()
    		} else {
    			$("#airplaneDetailsDelivery").text(airplane.age * -1 + "week(s)")
    			$("#airplaneDetailsAgeRow").hide()
    			$("#airplaneDetailsDeliveryRow").show()
    		}
	    	$("#airplaneDetailsSellValue").text("$" + commaSeparateNumber(airplane.sellValue))
	    	var replaceCost = airplane.price - airplane.sellValue
	    	$("#airplaneDetailsReplaceCost").text("$" + commaSeparateNumber(replaceCost))
	    	$("#airplaneDetailsLink").empty()
	    	if (airplane.link) {
	    		$("#airplaneDetailsLink").append("<a href='javascript:void(0)' onclick='showWorldMap(); selectLinkFromMap(" + airplane.link.id + ", true)'>" + airplane.link.fromAirportName + "(" + airplane.link.fromAirportCity + ") => " + airplane.link.toAirportName + "(" + airplane.link.toAirportCity + ")</a>" )
	    		$("#sellAirplaneButton").hide()
	    	} else {
	    		$("#airplaneDetailsLink").text("-")
	    		if (airplane.age >= 0) {
	    			$("#sellAirplaneButton").show()
	    		} else {
	    			$("#sellAirplaneButton").hide()
	    		}
	    	}
	    	
	    	$('#ownedAirplaneDetail .rejection').hide()
	    	if (airplane.age >= 0) {
	    		if (activeAirline.balance >= replaceCost) { 
	    			$("#replaceAirplaneButton").show()
	    		} else {
	    			$("#replaceAirplaneButton").hide()
	    			$('#ownedAirplaneDetail .rejection').show()
	    		}
	    	} else {
	    		$("#replaceAirplaneButton").hide()
	    	}
	    	
	    	$("#airplaneCanvas #ownedAirplaneDetail").fadeIn(200)
	    	
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function showAirplaneCanvas() {
	loadAirplaneModels()
	setActiveDiv($("#airplaneCanvas"))
	highlightTab($('#airplaneCanvasTab'))
	
	if ($("#airplaneCanvas #airplaneModelList li.selected").length !== 0) {
		selectedModelId = $("#airplaneCanvas #airplaneModelList li.selected").data("modelId")
	}
	
	var airlineId = activeAirline.id
	var ownedModelIds = []
	sortedModels = []
	$.ajax({
		type: 'GET',
		url: "airlines/"+ airlineId + "/airplanes?simpleResult=true",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(ownedModels) { //a list of model with airplanes
	    	$.each(ownedModels, function(key, model) {
	    		ownedModelIds.push(model.id)
	    		sortedModels.push(model)
	  		});
	    	
	    	//now add all the models that the airline does not own
	    	$.each(loadedModels, function(modelId, model) {
	    		if (!ownedModelIds.includes(model.id)) {
	    			model.assignedAirplanes = []
	    			model.availableAirplanes = []
	    			model.constructingAirplanes = []
	    			sortedModels.push(model)
	    		}
	    	})
	    	
	    	updateAirplaneModelList($('#airplaneSortSelect').val(), $('#airplaneSortOrderSelect').val())
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateAirplaneModelList(sortingProperty, ascending) {
	var airplaneModelList = $("#airplaneCanvas #airplaneModelList")
	airplaneModelList.empty()
	
	//sort the list
	sortedModels.sort(sortByProperty(sortingProperty, ascending == "ascending"))
	
	$.each(sortedModels, function(index, model) {
		var label = model.name + " (" + model.assignedAirplanes.length + " / " + model.availableAirplanes.length + " / " + model.constructingAirplanes.length + ")"
		var item = $("<li class='row clickable' data-model-id='" + model.id + "' onclick='selectAirplaneModel(this.modelInfo)'>" + label + "</li>").appendTo(airplaneModelList)
		item.get(0).modelInfo = model //tag the info to the element
		
		if (selectedModelId == model.id) {
			selectAirplaneModel(model)
		}
	});
}
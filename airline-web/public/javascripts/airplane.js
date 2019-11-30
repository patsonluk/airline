var loadedModelsById = {}
var loadedModelsOwnerInfo = {}
var loadedUsedAirplanes = []
var selectedModelId
var selectedModel

function loadAirplaneModels() {
	loadedModelsById = {}
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/airplane-models",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    async: false,
	    success: function(models) {
	    	$.each(models, function( key, model ) {
	    		loadedModelsById[model.id] = model
	  		});
	    	//updateModelInfo($('#modelInfo'))
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}



function loadAirplaneModelOwnerInfo() {
	if ($("#airplaneCanvas #airplaneModelTable div.selected").length !== 0) {
		selectedModelId = $("#airplaneCanvas #airplaneModelTable div.selected").data("modelId")
	}
	
	var airlineId = activeAirline.id
	var ownedModelIds = []
	loadedModelsOwnerInfo = []
	$.ajax({
		type: 'GET',
		url: "airlines/"+ airlineId + "/airplanes?simpleResult=true",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    async: false,
	    success: function(ownedModels) { //a list of model with airplanes
	    	$.each(ownedModels, function(key, model) {
	    		ownedModelIds.push(model.id)
	    		loadedModelsOwnerInfo.push(model)
	    		model.assignedAirplanes.sort(sortByProperty('condition'))
	    		model.idleAirplanes.sort(sortByProperty('condition'))
	    		
	    		model.totalOwned = model.assignedAirplanes.length + model.idleAirplanes.length 
	    		model.rejection = loadedModelsById[model.id].rejection
	  		});
	    	
	    	//now add all the models that the airline does not own
	    	$.each(loadedModelsById, function(modelId, model) {
	    		if (!ownedModelIds.includes(model.id)) {
	    			model.assignedAirplanes = []
	    			model.idleAirplanes = []
	    			model.totalOwned = 0
	    			loadedModelsOwnerInfo.push(model)
	    		}
	    	})
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function updateAirplaneModelTable(sortProperty, sortOrder) {
	//sort the list
	loadedModelsOwnerInfo.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	var airplaneModelTable = $("#airplaneModelTable")
	airplaneModelTable.children("div.table-row").remove()
	
	
	$.each(loadedModelsOwnerInfo, function(index, modelOwnerInfo) {
		var row = $("<div class='table-row clickable' data-model-id='" + modelOwnerInfo.id + "' onclick='selectAirplaneModel($(this).data(\"modelOwnerInfo\"))'></div>")
		row.append("<div class='cell'>" + modelOwnerInfo.name + "</div>")
		row.append("<div class='cell' align='right'>" + commaSeparateNumber(modelOwnerInfo.price) + "</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.capacity + "</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.range + " km</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.fuelBurn + "</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.lifespan / 52 + " yrs</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.speed + " km/h</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.assignedAirplanes.length + "/" + modelOwnerInfo.idleAirplanes.length + "</div>")
		row.data("modelOwnerInfo", modelOwnerInfo)
		
		if (selectedModelId == modelOwnerInfo.id) {
			row.addClass("selected")
			selectAirplaneModel(modelOwnerInfo)
		}
		airplaneModelTable.append(row)
	});
	
}

function updateUsedAirplaneTable(sortProperty, sortOrder) {
	var usedAirplaneTable = $("#airplaneCanvas #usedAirplaneTable")
	usedAirplaneTable.children("div.table-row").remove()
	
	//sort the list
	loadedUsedAirplanes.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	$.each(loadedUsedAirplanes, function(index, usedAirplane) {
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell'>" + usedAirplane.id + "</div>")
		row.append("<div class='cell' align='right'>$" + commaSeparateNumber(usedAirplane.dealerValue) + "</div>")
		row.append("<div class='cell' align='right'>" + usedAirplane.condition.toFixed(2) + "%</div>")
		if (!usedAirplane.rejection) {
			row.append("<div class='cell' align='right'><img class='clickable' src='assets/images/icons/airplane-plus.png' title='Purchase this airplane' onclick='buyUsedAirplane(" + usedAirplane.id + ")'></div>")
		} else {
			row.append("<div class='cell' align='right'><img src='assets/images/icons/prohibition.png' title='" + usedAirplane.rejection + "'/></div>")
		}
		usedAirplaneTable.append(row)
	});
	
	if (loadedUsedAirplanes.length == 0 ) {
		var row = $("<div class='table-row'></div>")
		row.append("<div class='cell'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell' align='right'>-</div>")
		row.append("<div class='cell' align='right'></div>")
		usedAirplaneTable.append(row)
	}
	
}

function toggleAirplaneModelTableSortOrder(sortHeader) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}
	
	sortHeader.siblings().removeClass("selected")
	sortHeader.addClass("selected")
	
	updateAirplaneModelTable(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
}

function toggleUsedAirplaneTableSortOrder(sortHeader) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}
	
	sortHeader.siblings().removeClass("selected")
	sortHeader.addClass("selected")
	
	updateUsedAirplaneTable(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
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

function buyUsedAirplane(airplaneId) {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/used-airplanes/airplanes/" + airplaneId 
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
	loadAirplaneModels()
	model = loadedModelsById[modelId]
	$('#airplaneModelDetails #selectedModel').val(modelId)
	$('#airplaneModelDetails #modelName').text(model.name)
	$('#airplaneModelDetails #capacity').text(model.capacity)
	$('#airplaneModelDetails #airplaneType').text(model.airplaneType)
	$('#airplaneModelDetails #minAirportSize').text(model.minAirportSize)
	$('#airplaneModelDetails #fuelBurn').text(model.fuelBurn)
	$('#airplaneModelDetails #range').text(model.range + "km")
	$('#airplaneModelDetails #speed').text(model.speed + "km/h")
	$('#airplaneModelDetails #lifespan').text(model.lifespan / 52 + " years")
	$('#airplaneModelDetails #airplaneCountry').html(getCountryFlagImg(model.countryCode))
	$('#airplaneModelDetails #price').text("$" + commaSeparateNumber(model.price))
	
	if (model.constructionTime == 0) {
		$('#airplaneModelDetails .delivery').text("immediate")
		$('#airplaneModelDetails .delivery').removeClass('warning')
//		$('#airplaneModelDetails .add').text('Purchase')
	} else {
		$('#airplaneModelDetails .delivery').text(model.constructionTime + " weeks")
		$('#airplaneModelDetails .delivery').addClass('warning')
//		$('#airplaneModelDetails .add').text('Place Order')
	}
	
	if (model.rejection) {
		$('#airplaneModelDetails .rejectionSpan').text(model.rejection)
		$('#airplaneModelDetails .rejection').show()
//		$('#airplaneModelDetails .add').hide()
	} else {
		$('#airplaneModelDetails .rejectionSpan').text('')
		$('#airplaneModelDetails .rejection').hide()
//		$('#airplaneModelDetails .add').show()
	}
}

function selectAirplaneModel(model) {
	selectedModel = model
	$("#airplaneCanvas #airplaneModelTable div.selected").removeClass("selected")
	//highlight the selected model
	$("#airplaneCanvas #airplaneModelTable div[data-model-id='" + model.id +"']").addClass("selected")
	
	loadUsedAirplanes(model)
	//expand the airplane list under this model
	showAirplaneInventory(model)
	//show basic airplane model details
	//model = loadedModels[modelId]
	if (model.imageUrl) {
		var imageLocation = 'assets/images/airplanes/' + model.name.replace(/\s+/g, '-').toLowerCase() + '.png'
		$('#modelIllustration img').attr('src', imageLocation)
		$('#modelIllustration a').attr('href', model.imageUrl)
		$('#modelIllustration').show()
	} else {
		$('#modelIllustration').hide()
	}
	
	$('#airplaneCanvas #selectedModel').val(model.id)
	$('#airplaneCanvas #modelName').text(model.name)
	$('#airplaneCanvas #capacity').text(model.capacity)
	$('#airplaneCanvas #airplaneType').text(model.airplaneType)
	$('#airplaneCanvas #minAirportSize').text(model.minAirportSize)
	$('#airplaneCanvas #fuelBurn').text(model.fuelBurn)
	$('#airplaneCanvas #range').text(model.range + " km")
	$('#airplaneCanvas #speed').text(model.speed + " km/h")
	$('#airplaneCanvas #lifespan').text(model.lifespan / 52 + " years")
	$('#airplaneCanvas #airplaneCountry').html(getCountryFlagImg(model.countryCode))
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
	if (model.rejection) {
		$('#airplaneCanvas .rejectionSpan').text(model.rejection)
		$('#airplaneCanvas .rejection').show()
		$('#airplaneCanvas .add').hide()
	} else {
		$('#airplaneCanvas .rejectionSpan').text('')
		$('#airplaneCanvas .rejection').hide()
		$('#airplaneCanvas .add').show()
	}
	
	$('#airplaneCanvas #airplaneModelDetail').fadeIn(200)
	//hide owned model details
	$('#airplaneCanvas #ownedAirplaneDetail').fadeOut(200)
}

function loadUsedAirplanes(modelInfo) {
	$.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/used-airplanes/models/" + modelInfo.id,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(usedAirplanes) { 
	    	loadedUsedAirplanes = usedAirplanes
	    	var selectedSortHeader = $('#usedAirplaneSortHeader .cell.selected')
	    	updateUsedAirplaneTable(selectedSortHeader.data("sort-property"), selectedSortHeader.data("sort-order"))
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function showAirplaneInventory(modelInfo) {
	var airplaneInventoryList = $("#airplaneCanvas #airplaneInventoryList")
	airplaneInventoryList.empty()
	
	
	$.each(modelInfo.airplanes, function( key, airplane ) {
		var airplaneId = airplane.id
		
		var status
		var isBadCondition = airplane.condition < modelInfo.badConditionThreshold
		if (airplane.assigned) {
			if (!airplane.isReady) {
				status = "assignedBuilding"
			} else if (isBadCondition){
				status = "assignedReadyBad"
			} else {
				status = "assignedReady"
			}
		} else {
			if (!airplane.isReady) {
				status = "idleBuilding"
			} else if (isBadCondition){
				status = "idleReadyBad"
			} else {
				status = "idleReady"
			}
		}
		
		var li = $("<div style='float: left;' class='clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this))'></div>").appendTo(airplaneInventoryList)
		li.append(getAirplaneIcon(status))
	});
	
	
	
	$.each(modelInfo.assignedAirplanes, function( key, airplane ) {
		var airplaneId = airplane.id
		if (!airplane.isReady) {
			status = "assignedBuilding"
		} else if (airplane.condition < modelInfo.badConditionThreshold){
			status = "assignedReadyBad"
		} else {
			status = "assignedReady"
		}
		var li = $("<div style='float: left;' class='clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this))'></div>").appendTo(airplaneInventoryList)
		li.append(getAirplaneIcon(status, airplane))
	});
	
	$.each(modelInfo.idleAirplanes, function( key, airplane ) {
		var airplaneId = airplane.id
		
		if (!airplane.isReady) {
			status = "idleBuilding"
		} else if (airplane.condition < modelInfo.badConditionThreshold){
			status = "idleReadyBad"
		} else {
			status = "idleReady"
		}
		var li = $("<div style='float: left;' class='clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this))'></div>").appendTo(airplaneInventoryList)
		li.append(getAirplaneIcon(status, airplane))
	});
	
	
	if (modelInfo.totalOwned == 0) {
		airplaneInventoryList.append("<div class='label'>Do not own any " + modelInfo.name + "</div>")
	}
}

function loadOwnedAirplaneDetails(airplaneId, selectedItem) {
	$("#airplaneInventoryList .selected").removeClass("selected")
	//highlight the selected model
	selectedItem.addClass("selected")
	
	var airlineId = activeAirline.id 
	$("#actionAirplaneId").val(airplaneId)
	var currentCycle
	$.ajax({
        type: 'GET',
        url: "current-cycle",
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        async: false,
        success: function(result) {
            currentCycle = result.cycle
        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });


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
    		var age = currentCycle - airplane.constructedCycle

    		if (age >= 0) {
    			$("#airplaneDetailsAge").text(getYearMonthText(age))
    			$("#airplaneDetailsAgeRow").show()
    			$("#airplaneDetailsDeliveryRow").hide()
    		} else {
    			$("#airplaneDetailsDelivery").text(age * -1 + "week(s)")
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
	    		if (age >= 0) {
	    			$("#sellAirplaneButton").show()
	    		} else {
	    			$("#sellAirplaneButton").hide()
	    		}
	    	}
	    	
	    	$('#ownedAirplaneDetail .rejection').hide()
	    	if (age >= 0) {
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
	setActiveDiv($("#airplaneCanvas"))
	highlightTab($('#airplaneCanvasTab'))
	
	var selectedSortHeader = $('#airplaneModelSortHeader .cell.selected')
	loadAirplaneModels()
    loadAirplaneModelOwnerInfo()
    updateAirplaneModelTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
}


var loadedModelsById = {}
var loadedModelsOwnerInfo
var loadedModelsOwnerInfoById = {}
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

//load model info on airplanes that matches the model ID
function loadAirplaneModelOwnerInfoByModelId(modelId) {
    var airlineId = activeAirline.id
	$.ajax({
		type: 'GET',
		url: "airlines/"+ airlineId + "/airplanes/model/" + modelId + "?simpleResult=true",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    async: false,
	    success: function(result) {
            existingInfo = loadedModelsOwnerInfoById[modelId] //find the existing info
            //update the existing info with the newly loaded result
            if (result.length == 1) { //then has owned airplanes with this model
                var newInfo = result[0]
	            existingInfo.assignedAirplanes = newInfo.assignedAirplanes
	            existingInfo.assignedAirplanes.sort(sortByProperty('condition'))
                existingInfo.availableAirplanes = newInfo.availableAirplanes
                existingInfo.availableAirplanes.sort(sortByProperty('condition'))
                existingInfo.constructingAirplanes = newInfo.constructingAirplanes

                existingInfo.totalOwned = existingInfo.assignedAirplanes.length + existingInfo.availableAirplanes.length + existingInfo.constructingAirplanes.length
	        } else { //no longer owned this model
	            existingInfo.assignedAirplanes = []
                existingInfo.availableAirplanes = []
                existingInfo.constructingAirplanes = []
                existingInfo.totalOwned = 0
	        }
        },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


//load model info on all airplanes
function loadAirplaneModelOwnerInfo() {
	var airlineId = activeAirline.id
	var ownedModelIds = []
	loadedModelsOwnerInfo = []
	loadedModelsOwnerInfoById = {}
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
	    		loadedModelsOwnerInfoById[model.id] = model
	    		model.assignedAirplanes.sort(sortByProperty('condition'))
	    		model.availableAirplanes.sort(sortByProperty('condition'))
	    		
	    		model.totalOwned = model.assignedAirplanes.length + model.availableAirplanes.length + model.constructingAirplanes.length
	    		model.rejection = loadedModelsById[model.id].rejection
	  		});
	    	
	    	//now add all the models that the airline does not own
	    	$.each(loadedModelsById, function(modelId, model) {
	    		if (!ownedModelIds.includes(model.id)) {
	    			model.assignedAirplanes = []
	    			model.availableAirplanes = []
	    			model.constructingAirplanes = []
	    			model.totalOwned = 0
	    			loadedModelsOwnerInfo.push(model)
	    			loadedModelsOwnerInfoById[model.id] = model
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
    if (!sortProperty && !sortOrder) {
        var selectedSortHeader = $('#airplaneModelSortHeader .cell.selected')
        sortProperty = selectedSortHeader.data('sort-property')
        sortOrder = selectedSortHeader.data('sort-order')
    }
	//sort the list
	loadedModelsOwnerInfo.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	var airplaneModelTable = $("#airplaneModelTable")
	airplaneModelTable.children("div.table-row").remove()
	
	
	$.each(loadedModelsOwnerInfo, function(index, modelOwnerInfo) {
		var row = $("<div class='table-row clickable' data-model-id='" + modelOwnerInfo.id + "' onclick='selectAirplaneModel(loadedModelsOwnerInfoById[" + modelOwnerInfo.id + "])'></div>")
		row.append("<div class='cell'>" + modelOwnerInfo.name + "</div>")
		row.append("<div class='cell' align='right'>" + commaSeparateNumber(modelOwnerInfo.price) + "</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.capacity + "</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.range + " km</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.fuelBurn + "</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.lifespan / 52 + " yrs</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.speed + " km/h</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.assignedAirplanes.length + "/" + modelOwnerInfo.availableAirplanes.length + "/" + modelOwnerInfo.constructingAirplanes.length + "</div>")

		
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
		row.data("airplane", usedAirplane)
		row.append("<div class='cell'>" + usedAirplane.id + "</div>")
		var priceColor
		if (usedAirplane.dealerRatio >= 1.1) { //expensive
		    priceColor = "#D46A6A"
		} else if (usedAirplane.dealerRatio <= 0.9) { //cheap
		    priceColor = "#68A357"
		}
		var priceDiv = $("<div class='cell' align='right'>$" + commaSeparateNumber(usedAirplane.dealerValue) + "</div>")
		if (priceColor) {
		    priceDiv.css("color", priceColor)
	    }
		row.append(priceDiv)
		row.append("<div class='cell' align='right'>" + usedAirplane.condition.toFixed(2) + "%</div>")
		if (!usedAirplane.rejection) {
			row.append("<div class='cell' align='right'><img class='clickable' src='assets/images/icons/airplane-plus.png' title='Purchase this airplane' onclick='promptBuyUsedAirplane($(this).closest(\".table-row\").data(\"airplane\"))'></div>")
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

function promptBuyUsedAirplane(airplane) {
    var buyAirplaneFunction = function(homeAirportId, selectedConfigurationId) {
        buyUsedAirplane(airplane.id, homeAirportId, selectedConfigurationId)
    }
    promptBuyAirplane(airplane.modelId, airplane.condition.toFixed(2), airplane.dealerValue, 0, null, buyAirplaneFunction)
}

function promptBuyNewAirplane(modelId, fromPlanLink, explicitHomeAirportId) {
    var buyAirplaneFunction = function(homeAirportId, selectedConfigurationId) {
        buyAirplane(modelId, homeAirportId, selectedConfigurationId, fromPlanLink)
    }

    promptBuyAirplane(modelId, 100, loadedModelsById[modelId].price, loadedModelsById[modelId].constructionTime, explicitHomeAirportId, buyAirplaneFunction)
}

function promptBuyAirplane(modelId, condition, price, deliveryTime, explicitHomeAirportId, buyAirplaneFunction) {
    var model = loadedModelsById[modelId]
    if (model.imageUrl) {
        var imageLocation = 'assets/images/airplanes/' + model.name.replace(/\s+/g, '-').toLowerCase() + '.png'
        $('#buyAirplaneModal .modelIllustration img').attr('src', imageLocation)
        $('#buyAirplaneModal .modelIllustration a').attr('href', model.imageUrl)
        $('#buyAirplaneModal .modelIllustration').show()

    } else {
        $('#buyAirplaneModal .modelIllustration').hide()
    }

    $('#buyAirplaneModal .modelName').text(model.name)
    if (deliveryTime == 0) {
		$('#buyAirplaneModal .delivery').text("immediate")
		$('#buyAirplaneModal .delivery').removeClass('warning')
		$('#buyAirplaneModal .add').text('Purchase')
	} else {
		$('#buyAirplaneModal .delivery').text(deliveryTime + " weeks")
        $('#buyAirplaneModal .delivery').addClass('warning')
        $('#buyAirplaneModal .add').text('Place Order')
	}

	$('#buyAirplaneModal .price').text("$" + commaSeparateNumber(price))
	$('#buyAirplaneModal .condition').text(condition + "%")

    var homeOptionsSelect = $("#buyAirplaneModal .homeOptions").empty()
    $.each(activeAirline.baseAirports, function(index, baseAirport) {
        var option = $("<option value='" + baseAirport.airportId + "'>" + getAirportText(baseAirport.city, baseAirport.airportCode) + "</option>")
        if (baseAirport.headquarter) {
            homeOptionsSelect.prepend(option)
        } else {
            homeOptionsSelect.append(option)
        }
        if (explicitHomeAirportId) { //if an explicit home is provided
            if (explicitHomeAirportId == baseAirport.airportId) {
                option.attr("selected", "selected")
            }
        } else { //otherwise look for HQ
            if (baseAirport.airportId == activeAirline.headquarterAirport.airportId) {
                option.attr("selected", "selected")
            }
        }
    })
    var airlineId = activeAirline.id

    $.ajax({
        type: 'GET',
        url: "airlines/" + airlineId + "/configurations?modelId=" + modelId,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            if (result.configurations.length == 0) {
                $('#buyAirplaneModal .table-row.seatConfiguration').hide()
            } else {
                $("#buyAirplaneModal .configuration-options").empty()
                $("#buyAirplaneModal .configuration-options").data("selectedIndex", 0)
                $("#buyAirplaneModal .configuration-options").data("optionCount", result.configurations.length)

                var defaultIndex = 0
                $.each(result.configurations, function(index, option) {
                    if (option.isDefault) {
                        defaultIndex = index
                    }
                })

                for (i = 0 ; i < result.configurations.length; i ++) {
                    //start from the default
                    var index = (i + defaultIndex) % result.configurations.length
                    var option = result.configurations[index]
                    var barDiv = $("<div style='width : 100%' class='configuration-option'></div>")
                    $("#buyAirplaneModal .configuration-options").append(barDiv)
                    barDiv.data("configurationId", option.id)
                    if (i != 0) { //if not the matching one, hide by default
                        barDiv.hide()
                    }
                    plotSeatConfigurationBar(barDiv, option, model.capacity, result.spaceMultipliers)
                }
                if (result.configurations.length == 1) { //then hide the arrows buttons
                    $("#buyAirplaneModal .seatConfiguration .button").hide()
                } else {
                    $("#buyAirplaneModal .seatConfiguration .button").show()
                }
                $('#buyAirplaneModal .table-row.seatConfiguration').show()
            }
            $('#buyAirplaneModal .add').unbind("click").bind("click", function() {
                var selectedIndex = $("#buyAirplaneModal .configuration-options").data("selectedIndex")
                var selectedConfigurationId
                if (selectedIndex === undefined) {
                    selectedConfigurationId = -1
                } else {
                    selectedConfigurationId = $($("#buyAirplaneModal .configuration-options").children()[selectedIndex]).data("configurationId")
                }

                buyAirplaneFunction($("#buyAirplaneModal .homeOptions").find(":selected").val(), selectedConfigurationId)
            })
            $('#buyAirplaneModal').fadeIn(200)
        },
         error: function(jqXHR, textStatus, errorThrown) {
                        console.log(JSON.stringify(jqXHR));
                        console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}


function buyAirplane(modelId, homeAirportId, configurationId, fromPlanLink) {
	fromPlanLink = fromPlanLink || false
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/airplanes?model=" + modelId + "&quantity=1&airlineId=" + airlineId + "&homeAirportId=" + homeAirportId + "&configurationId=" + configurationId
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
	    	closeModal($('#buyAirplaneModal'))
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
	    	$("#ownedAirplaneDetailModal").data("hasChange", true)
	    	closeModal($('#ownedAirplaneDetailModal'))
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
	        $("#ownedAirplaneDetailModal").data("hasChange", true)
	        closeModal($('#ownedAirplaneDetailModal'))
	        refreshPanels(airlineId)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function buyUsedAirplane(airplaneId, homeAirportId, configurationId) {
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/used-airplanes/airplanes/" + airplaneId + "?homeAirportId=" + homeAirportId + "&configurationId=" + configurationId
	$.ajax({
		type: 'PUT',
		data: JSON.stringify({}),
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(response) {
	    	refreshPanels(airlineId)
	    	showAirplaneCanvas()
	    	closeModal($('#buyAirplaneModal'))
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
	$('#airplaneModelDetails .selectedModel').val(modelId)
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
		$('#airplaneModelDetails .add').text('Purchase')
	} else {
		$('#airplaneModelDetails .delivery').text(model.constructionTime + " weeks")
		$('#airplaneModelDetails .delivery').addClass('warning')
		$('#airplaneModelDetails .add').text('Place Order')
	}
	
	if (model.rejection) {
		disableButton($('#airplaneModelDetails .add'), model.rejection)
	} else {
		enableButton($('#airplaneModelDetails .add'))
	}
}

function selectAirplaneModel(model) {
	selectedModel = model
	selectedModelId = model.id
	$("#airplaneCanvas #airplaneModelTable div.selected").removeClass("selected")
	//highlight the selected model
	$("#airplaneCanvas #airplaneModelTable div[data-model-id='" + model.id +"']").addClass("selected")
	
	loadUsedAirplanes(model)


	//show basic airplane model details
	//model = loadedModels[modelId]
	if (model.imageUrl) {
		var imageLocation = 'assets/images/airplanes/' + model.name.replace(/\s+/g, '-').toLowerCase() + '.png'
		$('#airplaneCanvas .modelIllustration img').attr('src', imageLocation)
		$('#airplaneCanvas .modelIllustration a').attr('href', model.imageUrl)
		$('#airplaneCanvas .modelIllustration').show()
	} else {
		$('#airplaneCanvas .modelIllustration').hide()
	}
	
	$('#airplaneCanvas .selectedModel').val(model.id)
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
		disableButton($('#airplaneCanvas .add'), model.rejection)
	} else {
		enableButton($('#airplaneCanvas .add'))
	}

	$('#airplaneCanvas #airplaneModelDetail').fadeIn(200)

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

function toggleUtilizationRate(container, checkbox) {
    if (checkbox.is(':checked')) {
        container.find('.utilization').show()
    } else {
        container.find('.utilization').hide()
    }
}


function showAirplaneInventory(modelId) {
    if (!loadedModelsOwnerInfo) {
        loadAirplaneModelOwnerInfo()
    }

    $("#airplaneInventoryModal .inventoryContainer").empty()
    var model = loadedModelsById[modelId]

    $("#airplaneInventoryModal .modelName").text(model.name)

    $.each(activeAirline.baseAirports, function(index, base) {
        var inventoryDiv = $("<div style='width : 95%; height : 85px;' class='section config'></div>")
        if (base.headquarter) {
            inventoryDiv.append("<div style='display : inline-block;'><img src='assets/images/icons/building-hedge.png' style='vertical-align:middle;'></div>&nbsp;<div style='display : inline-block;'><h4>" + getCountryFlagImg(base.countryCode) + getAirportText(base.city, base.airportName) + "</h4></div>")
        } else {
            inventoryDiv.append("<div style='display : inline-block;'><img src='assets/images/icons/building-low.png' style='vertical-align:middle;'></div>&nbsp;<div style='display : inline-block;'><h4>" + getCountryFlagImg(base.countryCode) + getAirportText(base.city, base.airportName) + "</h4></div>")
        }

        var assignedAirplanesCount = getAssignedAirplanesCount("homeAirportId", base.airportId, model.id)
        addAirplaneInventoryDiv(inventoryDiv, model.id, "homeAirportId", base.airportId)

        if (base.headquarter) {
            $("#airplaneInventoryModal .inventoryContainer").prepend(inventoryDiv)
        } else {
            $("#airplaneInventoryModal .inventoryContainer").append(inventoryDiv)
        }
    })
    toggleUtilizationRate($("#airplaneInventoryModal .inventoryContainer"), $("#airplaneInventoryModal .toggleUtilizationRateBox"))
    $('#airplaneInventoryModal').css( "zIndex", 0) //as there could be other modals on top of this
    $('#airplaneInventoryModal').fadeIn(200)
}

function refreshInventoryAfterAirplaneUpdate() {
    loadAirplaneModelOwnerInfoByModelId(selectedModelId) //refresh the loaded airplanes on the selected model
    updateAirplaneModelTable()
    showAirplaneInventory(selectedModelId)
}

function addAirplaneInventoryDiv(containerDiv, modelId, compareKey, compareValue) {
    var airplanesDiv = $("<div style= 'width : 100%; height : 50px; overflow: auto;'></div>")
    var info = loadedModelsOwnerInfoById[modelId]

    var empty = true

    $.each(info.assignedAirplanes, function( key, airplane ) {
        if (airplane[compareKey] == compareValue) {
            var airplaneId = airplane.id
            var li = $("<div style='float: left;' class='clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this), refreshInventoryAfterAirplaneUpdate)'></div>").appendTo(airplanesDiv)
            li.append(getAirplaneIcon(airplane, info.badConditionThreshold, true))
            empty = false
         }
    });

    $.each(info.availableAirplanes, function( key, airplane ) {
        if (airplane[compareKey] == compareValue) {
            var airplaneId = airplane.id
            var li = $("<div style='float: left;' class='clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this), refreshInventoryAfterAirplaneUpdate)'></div>").appendTo(airplanesDiv)
            li.append(getAirplaneIcon(airplane, info.badConditionThreshold, false))
            empty = false
        }
    });

    $.each(info.constructingAirplanes, function( key, airplane ) {
        if (airplane[compareKey] == compareValue) {
            var airplaneId = airplane.id
            var li = $("<div style='float: left;' class='clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this), refreshInventoryAfterAirplaneUpdate)'></div>").appendTo(airplanesDiv)
            li.append("<img src='assets/images/icons/airplane-empty-construct.png'/>")
            empty = false
        }
    });

    if (empty) {
        airplanesDiv.append("<h4>-</h4>")
    }

    containerDiv.append(airplanesDiv)
}

function getAirplaneIcon(airplane, badConditionThreshold, isAssigned) {
    var condition = airplane.condition
    var airplaneId = airplane.id
    var div = $("<div style='position: relative;'></div>")
    var img = $("<img>")
    var src
    if (!airplane.isReady) {
        if (isAssigned) {
            src = 'assets/images/icons/airplane-construct.png'
        } else {
            src = 'assets/images/icons/airplane-empty-construct.png'
        }
    } else if (condition < badConditionThreshold) {
		if (isAssigned) {
			src = 'assets/images/icons/airplane-exclamation.png'
		} else {
			src = 'assets/images/icons/airplane-empty-exclamation.png'
		}
	} else {
		if (isAssigned) {
			src = 'assets/images/icons/airplane.png'
		} else {
			src = 'assets/images/icons/airplane-empty.png'
		}
	}

	var utilization = Math.round((airplane.maxFlightMinutes - airplane.availableFlightMinutes) / airplane.maxFlightMinutes * 100)
    var color
    if (utilization < 25) {
        color = "#FF9973"
    } else if (utilization < 50) {
        color = "#FFC273"
    } else if (utilization < 75) {
        color = "#59C795"
    } else {
        color = "#8CB9D9"
    }

	img.attr("src", src)
	div.attr("title", "#"+ airplaneId + " condition: " + condition.toFixed(2) + "% util: " + utilization + "%")
	div.append(img)


	var utilizationDiv = $("<div class='utilization' style='position: absolute; right: 0; bottom: 0; background-color: " + color + "; font-size: 8px; display: none;'></div>")
	utilizationDiv.text(utilization)
	div.append(utilizationDiv)
    return div;
}

function loadOwnedAirplaneDetails(airplaneId, selectedItem, closeCallback, disableChangeHome) {
	//highlight the selected model
//	if (selectedItem) {
//	    selectedItem.addClass("selected")
//    }
	
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
		url: "airlines/" + airlineId + "/airplanes/" + airplaneId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airplane) {
	        var model = loadedModelsById[airplane.modelId]
            if (model.imageUrl) {
                var imageLocation = 'assets/images/airplanes/' + model.name.replace(/\s+/g, '-').toLowerCase() + '.png'
                $('#ownedAirplaneDetail .modelIllustration img').attr('src', imageLocation)
                $('#ownedAirplaneDetail .modelIllustration a').attr('href', model.imageUrl)
                $('#ownedAirplaneDetail .modelIllustration').show()
            } else {
                $('#ownedAirplaneDetail .modelIllustration').hide()
            }

	    	$("#airplaneDetailsId").text(airplane.id)
    		$("#airplaneDetailsCondition").text(airplane.condition.toFixed(2) + "%")
    		$("#airplaneDetailsCondition").removeClass("warning fatal")
    		if (airplane.condition < airplane.criticalConditionThreshold) {
    			$("#airplaneDetailsCondition").addClass("fatal")
    		} else if (airplane.condition < airplane.badConditionThreshold) {
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
	    	if (airplane.links.length > 0) {
	    	    $.each(airplane.links, function(index, linkEntry) {
	    	        var link = linkEntry.link
	    	        var linkDescription = "<div style='display: flex; align-items: center;'>" + getAirportText(link.fromAirportCity, link.fromAirportCode) + "<img src='assets/images/icons/arrow.png'>" + getAirportText(link.toAirportCity, link.toAirportCode) + " " + linkEntry.frequency + " flight(s) per week</div>"
	    	        $("#airplaneDetailsLink").append("<div><a href='javascript:void(0)' onclick='closeAllModals(); showWorldMap(); selectLinkFromMap(" + link.id + ", true)'>" + linkDescription + "</a></div>" )
	    	    })
	    		disableButton("#sellAirplaneButton", "Cannot sell airplanes with route assigned")
	    	} else {
	    		$("#airplaneDetailsLink").text("-")
	    		if (age >= 0) {
	    		    enableButton("#sellAirplaneButton")
	    		} else {
	    			disableButton("#sellAirplaneButton", "Cannot sell airplanes that are still under construction")
	    		}

	    	}
	    	$("#ownedAirplaneDetail .availableFlightMinutes").text(airplane.availableFlightMinutes)
	    	populateAirplaneHome(airplane, disableChangeHome)

            var weeksRemainingBeforeReplacement = airplane.constructionTime - (currentCycle - airplane.purchasedCycle)
	    	if (weeksRemainingBeforeReplacement <= 0) {
	    	    if (activeAirline.balance < replaceCost) {
            	    disableButton("#replaceAirplaneButton", "Not enough cash to replace this airplane")
            	} else {
	    	        enableButton("#replaceAirplaneButton")
                }
	    	} else {
                disableButton("#replaceAirplaneButton", "Can only replace this airplane " + weeksRemainingBeforeReplacement + " week(s) from now")
	    	}

	    	$("#ownedAirplaneDetail").data("airplane", airplane)

            $.ajax({
                type: 'GET',
                url: "airlines/" + airlineId + "/configurations?modelId=" + airplane.modelId,
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                success: function(result) {
                    var configuration
                    var matchingIndex
                    $.each(result.configurations, function(index, option) {
                        if (option.id == airplane.configurationId) {
                            configuration = option
                            matchingIndex = index
                            plotSeatConfigurationBar($('#ownedAirplaneDetailModal .configurationBar'), configuration, airplane.capacity, result.spaceMultipliers)
                        }
                    })
                    if (result.configurations.length <= 1) { //then cannot change
                        $("#ownedAirplaneDetail .configuration-view .edit").hide()
                        $("#ownedAirplaneDetail .configuration-view .editDisabled").show()
                    } else {
                        $("#ownedAirplaneDetail .configuration-view .show").hide()

                        //populateConfigurationOptionsFunction = function() { //delay this as the div is not visible and fusionchart would not render it
                            $("#ownedAirplaneDetail .configuration-options").empty()
                            $("#ownedAirplaneDetail .configuration-options").data("selectedIndex", 0)
                            $("#ownedAirplaneDetail .configuration-options").data("optionCount", result.configurations.length)
                            for (i = 0 ; i < result.configurations.length; i ++) {
                                //start from the matching one
                                var index = (i + matchingIndex) % result.configurations.length
                                var option = result.configurations[index]
                                var barDiv = $("<div style='width : 100%' class='configuration-option'></div>")
                                $("#ownedAirplaneDetail .configuration-options").append(barDiv)
                                barDiv.data("configurationId", option.id)
                                if (i != 0) { //if not the matching one, hide by default
                                    barDiv.hide()
                                }
                                plotSeatConfigurationBar(barDiv, option, airplane.capacity, result.spaceMultipliers)
                            }
                        //}
                        $("#ownedAirplaneDetail .configuration-view .edit").show()
                        $("#ownedAirplaneDetail .configuration-view .editDisabled").hide()
                    }
                    $("#ownedAirplaneDetail .configuration-view").show()
                    $("#ownedAirplaneDetail .configuration-edit").hide()

                    if (closeCallback) {
                        $("#ownedAirplaneDetailModal").data("closeCallback", function() {
                            if ($("#ownedAirplaneDetailModal").data("hasChange")) { //only trigger close callback if there are changes
                                closeCallback()
                                $("#ownedAirplaneDetailModal").removeData("hasChange")
                            }
                        })
                    } else {
                        $("#ownedAirplaneDetailModal").removeData("closeCallback")
                    }
                    $("#ownedAirplaneDetailModal").fadeIn(200)
                },
                 error: function(jqXHR, textStatus, errorThrown) {
                	            console.log(JSON.stringify(jqXHR));
                	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
                	    }
            });



	    	
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function populateAirplaneHome(airplane, disableChangeHome) {
    var homeAirportId = airplane.homeAirportId
    var hasLinks = airplane.links.length > 0
    var currentAirport
    var homeOptionsSelect = $("#ownedAirplaneDetail .homeEdit .homeOptions").empty()
    $.each(activeAirline.baseAirports, function(index, baseAirport) {
        if (baseAirport.airportId == homeAirportId) {
            currentAirport = baseAirport
        }
    })

    if (!hasLinks && !disableChangeHome) { //only allow switching home if the airplane is free
        $.each(activeAirline.baseAirports, function(index, baseAirport) {
            var option = $("<option value='" + baseAirport.airportId + "'>" + getAirportText(baseAirport.city, baseAirport.airportCode) + "</option>")
            if (baseAirport.headquarter) {
                homeOptionsSelect.prepend(option)
            } else {
                homeOptionsSelect.append(option)
            }

            if (baseAirport.airportId == homeAirportId) {
                option.attr("selected", "selected")
            }
        })
    }


    if (currentAirport) {
        $("#ownedAirplaneDetail .homeView .home").text(getAirportText(currentAirport.city, currentAirport.airportCode))
    } else {
        $("#ownedAirplaneDetail .homeView .home").text("-")
    }
    if (disableChangeHome) { //explicitly disabled it, do not even show any reason
        $("#ownedAirplaneDetail .homeView .editDisabled").hide()
        $("#ownedAirplaneDetail .homeView .edit").hide()
    } else if (hasLinks) {
        $("#ownedAirplaneDetail .homeView .editDisabled").show()
        $("#ownedAirplaneDetail .homeView .edit").hide()
    } else {
        $("#ownedAirplaneDetail .homeView .editDisabled").hide()
        $("#ownedAirplaneDetail .homeView .edit").show()
    }
    $("#ownedAirplaneDetail .homeView").show()
    $("#ownedAirplaneDetail .homeEdit").hide()
}

function toggleAirplaneHome() {
    $("#ownedAirplaneDetail .homeView").hide()
    $("#ownedAirplaneDetail .homeEdit").show()
}

function showAirplaneCanvas() {
	setActiveDiv($("#airplaneCanvas"))
	highlightTab($('#airplaneCanvasTab'))

	loadAirplaneModels()
    loadAirplaneModelOwnerInfo()
    updateAirplaneModelTable()
}

function toggleAirplaneConfiguration() {
  $("#ownedAirplaneDetail .configuration-view").hide()
  $("#ownedAirplaneDetail .configuration-edit").show()
  refreshAirplaneConfigurationOption($("#ownedAirplaneDetail"))
}


function switchAirplaneConfigurationOption(containerDiv, indexDiff) {
    var currentIndex = containerDiv.find(".configuration-options").data("selectedIndex")
    var optionCount =  containerDiv.find(".configuration-options").data("optionCount")
    var index = currentIndex + indexDiff
    if (index < 0) {
      index = optionCount - 1
    } else if (index >= optionCount) {
      index = 0
    }
    containerDiv.find(".configuration-options").data("selectedIndex", index)
    refreshAirplaneConfigurationOption(containerDiv)
}
function cancelAirplaneConfigurationOption() {
    $("#ownedAirplaneDetail .configuration-options").data("selectedIndex", 0)
    $("#ownedAirplaneDetail .configuration-edit").hide()
    $("#ownedAirplaneDetail .configuration-view").show()
}
function refreshAirplaneConfigurationOption(containerDiv) {
    var currentIndex = containerDiv.find(".configuration-options").data("selectedIndex")
    var optionCount =  containerDiv.find(".configuration-options").data("optionCount")

    for (i = 0 ; i < optionCount; i++) {
        if (currentIndex == i) {
            $(containerDiv.find(".configuration-options").children()[i]).show()
        } else {
            $(containerDiv.find(".configuration-options").children()[i]).hide()
        }
    }
}

function confirmAirplaneConfigurationOption() {
    var airlineId = activeAirline.id
    var airplane = $("#ownedAirplaneDetail").data("airplane")
    var currentIndex = $("#ownedAirplaneDetail .configuration-options").data("selectedIndex")
    var selectedConfigurationId = $($("#ownedAirplaneDetail .configuration-options").children()[currentIndex]).data("configurationId")
    if (selectedConfigurationId != airplane.configurationId) { //then need to update
        $.ajax({
                type: 'PUT',
                url: "airlines/" + airlineId + "/airplanes/" + airplane.id + "/configuration/" + selectedConfigurationId,
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                async: false,
                success: function(result) {
                    loadOwnedAirplaneDetails(result.id, null, $("#ownedAirplaneDetailModal").data("close-callback"))
                    $("#ownedAirplaneDetailModal").data("hasChange", true)
                },
                error: function(jqXHR, textStatus, errorThrown) {
                        console.log(JSON.stringify(jqXHR));
                        console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
                }
            });
    } else { //flip back to view mode
        $("#ownedAirplaneDetail .configuration-view").show()
        $("#ownedAirplaneDetail .configuration-edit").hide()
    }
}

function confirmAirplaneHome() {
    var airlineId = activeAirline.id
    var airplane = $("#ownedAirplaneDetail").data("airplane")
    var selectedAirportId = $('#ownedAirplaneDetail .homeOptions').find(":selected").val()

    if (selectedAirportId != airplane.homeAirportId) { //then need to update
        $.ajax({
                type: 'PUT',
                url: "airlines/" + airlineId + "/airplanes/" + airplane.id + "/home/" + selectedAirportId,
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                async: false,
                success: function(result) {
                    loadOwnedAirplaneDetails(result.id, null, $("#ownedAirplaneDetailModal").data("close-callback"))
                    $("#ownedAirplaneDetailModal").data("hasChange", true)
                },
                error: function(jqXHR, textStatus, errorThrown) {
                        console.log(JSON.stringify(jqXHR));
                        console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
                }
            });
    } else { //flip back to view mode
        $("#ownedAirplaneDetail .homeView").show()
        $("#ownedAirplaneDetail .homeEdit").hide()
    }
}

function cancelAirplaneHome() {
    $("#ownedAirplaneDetail .homeView").show()
    $("#ownedAirplaneDetail .homeEdit").hide()
}

function getAssignedAirplanesCount(compareKey, compareValue, modelId) {
    var count = 0;
    $.each(loadedModelsOwnerInfoById[modelId].assignedAirplanes, function( key, airplane ) {
            if (airplane[compareKey] == compareValue) {
                count ++
             }
        });

    $.each(loadedModelsOwnerInfoById[modelId].availableAirplanes, function( key, airplane ) {
        if (airplane[compareKey] == compareValue) {
            count ++
        }
    });

    $.each(loadedModelsOwnerInfoById[modelId].constructingAirplanes, function( key, airplane ) {
        if (airplane[compareKey] == compareValue) {
            count ++
        }
    });
    return count
}
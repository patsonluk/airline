var loadedModelsById = {}
var loadedModelsOwnerInfo = []
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
		url: "airlines/"+ airlineId + "/airplanes/model/" + modelId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    async: false,
	    success: function(result) {
            existingInfo = loadedModelsById[modelId] //find the existing info
            //update the existing info with the newly loaded result
            var newInfo = result[modelId]
            if (newInfo) { //then has owned airplanes with this model
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
	        existingInfo.isFullLoad = true
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
	loadedModelsOwnerInfo = []
	$.ajax({
		type: 'GET',
		url: "airlines/"+ airlineId + "/airplanes?simpleResult=true&groupedResult=true",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    async: false,
	    success: function(ownedModels) { //a list of model with airplanes
	    	//add airplane ownership info to existing model entries
	    	$.each(loadedModelsById, function(modelId, model) {
	    	    var ownedAirplanesInfo = ownedModels[modelId]
	    	    if (ownedAirplanesInfo) { //then own some airplanes of this model
	    	        model.assignedAirplanes = ownedAirplanesInfo.assignedAirplanes
	    	        model.availableAirplanes = ownedAirplanesInfo.availableAirplanes
	    	        model.constructingAirplanes = ownedAirplanesInfo.constructingAirplanes
	    	        model.assignedAirplanes.sort(sortByProperty('condition'))
                    model.availableAirplanes.sort(sortByProperty('condition'))
	    	    } else {
	    	        model.assignedAirplanes = []
                    model.availableAirplanes = []
                    model.constructingAirplanes = []
	    	    }
                model.totalOwned = model.assignedAirplanes.length + model.availableAirplanes.length + model.constructingAirplanes.length
       			loadedModelsOwnerInfo.push(model)
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
		var row = $("<div class='table-row clickable' data-model-id='" + modelOwnerInfo.id + "' onclick='selectAirplaneModel(loadedModelsById[" + modelOwnerInfo.id + "])'></div>")
		if (modelOwnerInfo.isFavorite) {
		    row.append("<div class='cell'>" + modelOwnerInfo.name + "<img src='assets/images/icons/heart.png' height='10px'></div>")
        } else {
            row.append("<div class='cell'>" + modelOwnerInfo.name + "</div>")
		}
		row.append("<div class='cell'>" + modelOwnerInfo.family + "</div>")
		row.append("<div class='cell' align='right'>" + commaSeparateNumber(modelOwnerInfo.price) + "</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.capacity + "</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.range + " km</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.fuelBurn + "</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.lifespan / 52 + " yrs</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.speed + " km/h</div>")
		row.append("<div class='cell' align='right'>" + modelOwnerInfo.runwayRequirement + " m</div>")
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

		row.append("<div class='cell'>" +  getAirlineLogoImg(usedAirplane.ownerId) + usedAirplane.ownerName + "</div>")

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
    var buyAirplaneFunction = function(dummyQuantity, homeAirportId, selectedConfigurationId) {
        buyUsedAirplane(airplane.id, homeAirportId, selectedConfigurationId)
    }
    promptBuyAirplane(airplane.modelId, airplane.condition.toFixed(2), airplane.dealerValue, 0, null, false, buyAirplaneFunction)
}

function promptBuyNewAirplane(modelId, fromPlanLink, explicitHomeAirportId) {
    var buyAirplaneFunction = function(quantity, homeAirportId, selectedConfigurationId) {
        buyAirplane(modelId, quantity, homeAirportId, selectedConfigurationId, fromPlanLink)
    }

    promptBuyAirplane(modelId, 100, loadedModelsById[modelId].price, loadedModelsById[modelId].constructionTime, explicitHomeAirportId, true, buyAirplaneFunction)
}

function updateAirplaneTotalPrice(totalPrice) {
    $('#buyAirplaneModal .totalPrice .value').text("$" + commaSeparateNumber(totalPrice))
    if (totalPrice == 0) {
        disableButton($('#buyAirplaneModal .add'), "Amount should not be 0")
    } else if (totalPrice > activeAirline.balance) {
        $('#buyAirplaneModal .add')
        disableButton($('#buyAirplaneModal .add'), "Not enough cash")
    } else {
        enableButton($('#buyAirplaneModal .add'))
    }
}

function validateAirplaneQuantity() {
    if ($('#buyAirplaneModal .quantity .input').val() === "") {
        return 0
    }
    //validate
    var quantity = $('#buyAirplaneModal .quantity .input').val()
    if (!($.isNumeric(quantity))) {
        quantity = 1
    }
    quantity = Math.floor(quantity)

    if (quantity < 1) {
        quantity = 1
    }

    $('#buyAirplaneModal .quantity .input').val(quantity)
    return quantity
}

function promptBuyAirplane(modelId, condition, price, deliveryTime, explicitHomeAirportId, multipleAble, buyAirplaneFunction) {
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

	if (multipleAble) {
	    $('#buyAirplaneModal .quantity .input').val(1)
	    $('#buyAirplaneModal .quantity .input').on('input', function(e){
	        var quantity = validateAirplaneQuantity()
            updateAirplaneTotalPrice(quantity * price)
        });
	    $('#buyAirplaneModal .quantity').show()

	    updateAirplaneTotalPrice(1 * price)
	    $('#buyAirplaneModal .totalPrice').show()
	} else {
	    $('#buyAirplaneModal .quantity').hide()
	    $('#buyAirplaneModal .totalPrice').hide()
	    enableButton($('#buyAirplaneModal .add'))
	}

    var homeOptionsSelect = $("#buyAirplaneModal .homeOptions").empty()
    var hasValidBase = false
    $.each(activeAirline.baseAirports, function(index, baseAirport) {
        if (baseAirport.airportRunwayLength >= model.runwayRequirement) {
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
            hasValidBase = true
        }

        if (hasValidBase) {
            enableButton($('#buyAirplaneModal .add'))
        } else {
            disableButton($('#buyAirplaneModal .add'), "No base with runway >= " + model.runwayRequirement + "m")
        }
    })
    var airlineId = activeAirline.id

    $.ajax({
        type: 'GET',
        url: "airlines/" + airlineId + "/configurations?modelId=" + modelId,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            $("#buyAirplaneModal .configuration-options").empty()
            if (result.configurations.length == 0) {
                $('#buyAirplaneModal .table-row.seatConfiguration').hide()
                $("#buyAirplaneModal .configuration-options").removeData("selectedIndex")
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

                buyAirplaneFunction($('#buyAirplaneModal .quantity .input').val(), $("#buyAirplaneModal .homeOptions").find(":selected").val(), selectedConfigurationId)
            })
            $('#buyAirplaneModal').fadeIn(200)
        },
         error: function(jqXHR, textStatus, errorThrown) {
                        console.log(JSON.stringify(jqXHR));
                        console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}


function buyAirplane(modelId, quantity, homeAirportId, configurationId, fromPlanLink) {
	fromPlanLink = fromPlanLink || false
	var airlineId = activeAirline.id
	var url = "airlines/" + airlineId + "/airplanes?modelId=" + modelId + "&quantity=" + quantity + "&airlineId=" + airlineId + "&homeAirportId=" + homeAirportId + "&configurationId=" + configurationId
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
	    },
	    beforeSend: function() {
            $('body .loadingSpinner').show()
        },
        complete: function(){
            $('body .loadingSpinner').hide()
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
	    	showAirplaneCanvas()
	    	closeModal($('#ownedAirplaneDetailModal'))
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
	    },
        beforeSend: function() {
             $('body .loadingSpinner').show()
         },
         complete: function(){
             $('body .loadingSpinner').hide()
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
	    },
        beforeSend: function() {
             $('body .loadingSpinner').show()
        },
        complete: function(){
             $('body .loadingSpinner').hide()
        }
	});
}


function updateModelInfo(modelId) {
	loadAirplaneModels()
	model = loadedModelsById[modelId]
	$('#airplaneModelDetails .selectedModel').val(modelId)
	$('#airplaneModelDetails #modelName').text(model.name)
	$('#airplaneModelDetails .modelFamily').text(model.family)
	$('#airplaneModelDetails #capacity').text(model.capacity)
	$('#airplaneModelDetails #airplaneType').text(model.airplaneType)
	$('#airplaneModelDetails .turnaroundTime').text(model.turnaroundTime)
	$('#airplaneModelDetails .runwayRequirement').text(model.runwayRequirement)
	$('#airplaneModelDetails #fuelBurn').text(model.fuelBurn)
	$('#airplaneModelDetails #range').text(model.range + "km")
	$('#airplaneModelDetails #speed').text(model.speed + "km/h")
	$('#airplaneModelDetails #lifespan').text(model.lifespan / 52 + " years")

	var $manufacturerSpan = $('<span>' + model.manufacturer + '&nbsp;</span>')
	$manufacturerSpan.append(getCountryFlagImg(model.countryCode))
	$('#airplaneModelDetails .manufacturer').empty()
	$('#airplaneModelDetails .manufacturer').append($manufacturerSpan)
	$('#airplaneModelDetails .price').text("$" + commaSeparateNumber(model.price))
	
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
	$('#airplaneCanvas .modelName').text(model.name)
	$('#airplaneCanvas .modelFamily').text(model.family)
	$('#airplaneCanvas #capacity').text(model.capacity)
	$('#airplaneCanvas #airplaneType').text(model.airplaneType)
	$('#airplaneCanvas .turnaroundTime').text(model.turnaroundTime)
	$('#airplaneCanvas .runwayRequirement').text(model.runwayRequirement)
	$('#airplaneCanvas #fuelBurn').text(model.fuelBurn)
	$('#airplaneCanvas #range').text(model.range + " km")
	$('#airplaneCanvas #speed').text(model.speed + " km/h")
	$('#airplaneCanvas #lifespan').text(model.lifespan / 52 + " years")

    $('#airplaneCanvas .manufacturer').empty()
    var $manufacturerSpan = $('<span>' + model.manufacturer + '&nbsp;</span>')
    $manufacturerSpan.append(getCountryFlagImg(model.countryCode))
    $('#airplaneCanvas .manufacturer').append($manufacturerSpan)

	if (model.originalPrice) {
	    var priceSpan = $("<span><span style='text-decoration: line-through'>$" + commaSeparateNumber(model.originalPrice) + "</span>&nbsp;$" + commaSeparateNumber(model.price) + "</span>")
	    priceSpan.append(getDiscountsTooltip(model.discounts.price))
	    $('#airplaneCanvas .price').html(priceSpan)
	} else {
	    $('#airplaneCanvas .price').text("$" + commaSeparateNumber(model.price))
    }
	
	if (model.constructionTime == 0) {
		$('#airplaneCanvas .delivery').text("immediate")
		$('#airplaneCanvas .delivery').removeClass('warning')
		$('#airplaneCanvas .add').text('Purchase')
	} else {
	    if (model.originalConstructionTime) {
            var deliverySpan = $("<span><span style='text-decoration: line-through'>" + model.originalConstructionTime + " weeks</span>&nbsp;" + model.constructionTime + " weeks</span>")
            deliverySpan.append(getDiscountsTooltip(model.discounts.construction_time))
            $('#airplaneCanvas .delivery').html(deliverySpan)
        } else {
            $('#airplaneCanvas .delivery').text(model.constructionTime + " weeks")
        }
		$('#airplaneCanvas .delivery').addClass('warning')
		$('#airplaneCanvas .add').text('Place Order')
	}
	if (model.rejection) {
		disableButton($('#airplaneCanvas .add'), model.rejection)
	} else {
		enableButton($('#airplaneCanvas .add'))
	}
	loadAirplaneModelStats(model)

	$('#airplaneCanvas #airplaneModelDetail').fadeIn(200)

}

function getDiscountsTooltip(discounts) {
    var tooltipDiv = $('<div class="tooltip"></div>')
    tooltipDiv.append("<img src='assets/images/icons/information.png'>")
    var tooltipTextSpan = $('<span class="tooltiptext" style="width: 200px;"></span>')
    tooltipDiv.append(tooltipTextSpan)
    var tooltipList = $('<ul></ul>')
    tooltipTextSpan.append(tooltipList)

    $.each(discounts, function(index, discount) {
        tooltipList.append("<li>" + discount.discountDescription + "</li>")
    })
    return tooltipDiv
}

function loadAirplaneModelStats(modelInfo) {
    var url
    var favoriteIcon = $("#airplaneModelDetail .favorite")
    var model = loadedModelsById[modelInfo.id]
    if (activeAirline) {
        url = "airlines/" + activeAirline.id + "/airplanes/model/" + model.id + "/stats",
        favoriteIcon.show()
    } else {
        url = "airplane-models/" + model.id + "/stats"
        favoriteIcon.hide()
    }

	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    async: false,
	    dataType: 'json',
	    success: function(stats) {
	    	updateTopOperatorsTable(stats)
	    	$('#airplaneCanvas .total').text(stats.total)
	    	if (stats.favorite !== undefined) {
	    	    favoriteIcon.off() //remove all listeners

                if (stats.favorite.rejection) {
                    $("#setFavoriteModal").data("rejection", stats.favorite.rejection)
                } else {
                    $("#setFavoriteModal").removeData("rejection")
                }

                if (modelInfo.isFavorite) {
                    favoriteIcon.attr("src", "assets/images/icons/heart.png")
                    $("#setFavoriteModal").data("rejection", "This is already the Favorite")
                } else {
                    favoriteIcon.attr("src", "assets/images/icons/heart-empty.png")
                }

                $("#setFavoriteModal").data("model", model)
	    	}
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateTopOperatorsTable(stats) {
    var statsTable = $("#airplaneModelDetail .topOperators")
	statsTable.children("div.table-row").remove()


	$.each(stats.topAirlines, function(index, entry) {
		var row = $("<div class='table-row'></div>")
		var airline = entry.airline
		row.append("<div class='cell'>" + getAirlineLogoImg(airline.id) +  airline.name + "</div>")
		row.append("<div class='cell' align='right'>" + entry.airplaneCount + "</div>")

		var percentage = (entry.airplaneCount * 100.0 / stats.total).toFixed(2)
		row.append("<div class='cell' align='right'>" + percentage + "%</div>")
		statsTable.append(row)
	});
	//append total row
    var row = $("<div style='display:table-row'></div>")
    row.append("<div class='cell' style='border-top: 1px solid #6093e7;'>All</div>")
    row.append("<div class='cell' align='right' style='border-top: 1px solid #6093e7;'>" + stats.total + "</div>")
    row.append("<div class='cell' align='right' style='border-top: 1px solid #6093e7;'>-</div>")

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

function toggleCondition(container, checkbox) {
    if (checkbox.is(':checked')) {
        container.find('.condition').show()
    } else {
        container.find('.condition').hide()
    }
}

function showAirplaneBase(modelId) {
    if (!loadedModelsOwnerInfo) {
        loadAirplaneModelOwnerInfo()
    }

    $("#airplaneBaseModal .inventoryContainer").empty()
    var model = loadedModelsById[modelId]

    $("#airplaneBaseModal .modelName").text(model.name)

    $.each(activeAirline.baseAirports, function(index, base) {
        var inventoryDiv = $("<div style='width : 95%; min-height : 85px;' class='section config'></div>")
        if (base.headquarter) {
            inventoryDiv.append("<div style='display : inline-block;'><img src='assets/images/icons/building-hedge.png' style='vertical-align:middle;'></div>&nbsp;<div style='display : inline-block;'><h4>" + getCountryFlagImg(base.countryCode) + getAirportText(base.city, base.airportName) + "</h4></div>")
        } else {
            inventoryDiv.append("<div style='display : inline-block;'><img src='assets/images/icons/building-low.png' style='vertical-align:middle;'></div>&nbsp;<div style='display : inline-block;'><h4>" + getCountryFlagImg(base.countryCode) + getAirportText(base.city, base.airportName) + "</h4></div>")
        }

        var assignedAirplanesCount = getAssignedAirplanesCount("homeAirportId", base.airportId, model.id)
        addAirplaneInventoryDivByBase(inventoryDiv, model.id, "homeAirportId", base.airportId)

        if (base.headquarter) {
            $("#airplaneBaseModal .inventoryContainer").prepend(inventoryDiv)
        } else {
            $("#airplaneBaseModal .inventoryContainer").append(inventoryDiv)
        }
        inventoryDiv.attr("ondragover", "allowUnassignedAirplaneIconDragOver(event)")
        inventoryDiv.attr("ondrop", "onAirplaneIconBaseDrop(event, " + base.airportId + ")");
    })
    toggleUtilizationRate($("#airplaneBaseModal .inventoryContainer"), $("#airplaneBaseModal .toggleUtilizationRateBox"))
    toggleCondition($("#airplaneBaseModal .inventoryContainer"), $("#airplaneBaseModal .toggleConditionBox"))
    $('#airplaneBaseModal').fadeIn(200)
}

//inventory for ALL airplanes
function showAllAirplaneInventory(modelId) {
    if (!loadedModelsOwnerInfo) {
        loadAirplaneModelOwnerInfo()
    }

    $("#allAirplaneInventoryModal .inventoryContainer").empty()
    var info = loadedModelsById[modelId]
    if (!info.isFullLoad) {
        loadAirplaneModelOwnerInfoByModelId(modelId) //refresh to get the utility rate
    }

    $("#allAirplaneInventoryModal .modelName").text(info.name)
    var inventoryDiv = $("<div style='width : 95%; min-height : 85px;' class='section'></div>")
    var airplanesDiv = $("<div style= 'width : 100%; overflow: auto;'></div>")
    var empty = true

    var allAirplanes = $.merge($.merge($.merge([], info.assignedAirplanes), info.availableAirplanes), info.constructingAirplanes)
    $.each(allAirplanes, function( key, airplane ) {
        var airplaneId = airplane.id
        var li = $("<div style='float: left;' class='clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this), refreshAllAirplaneInventoryAfterAirplaneUpdate)'></div>").appendTo(airplanesDiv)
        var airplaneIcon = getAirplaneIcon(airplane, info.badConditionThreshold)
        enableAirplaneIconDrag(airplaneIcon, airplaneId)
        enableAirplaneIconDrop(airplaneIcon, airplaneId, "refreshAllAirplaneInventoryAfterAirplaneUpdate")
        li.append(airplaneIcon)
        empty = false
    });

    if (empty) {
        airplanesDiv.append("<h4>-</h4>")
    }

    inventoryDiv.append(airplanesDiv)
    $("#allAirplaneInventoryModal .inventoryContainer").append(inventoryDiv)

    toggleUtilizationRate($("#allAirplaneInventoryModal .inventoryContainer"), $("#allAirplaneInventoryModal .toggleUtilizationRateBox"))
    toggleCondition($("#allAirplaneInventoryModal .inventoryContainer"), $("#allAirplaneInventoryModal .toggleConditionBox"))

    $('#allAirplaneInventoryModal').fadeIn(200)
}


function refreshBaseAfterAirplaneUpdate() {
    loadAirplaneModelOwnerInfoByModelId(selectedModelId) //refresh the loaded airplanes on the selected model
    updateAirplaneModelTable()
    showAirplaneBase(selectedModelId)
}

function refreshAllAirplaneInventoryAfterAirplaneUpdate() {
    loadAirplaneModelOwnerInfoByModelId(selectedModelId) //refresh the loaded airplanes on the selected model
    updateAirplaneModelTable()
    showAllAirplaneInventory(selectedModelId)
}

function addAirplaneInventoryDivByBase(containerDiv, modelId, compareKey, compareValue) {
    var airplanesDiv = $("<div style= 'width : 100%; height : 50px; overflow: auto;'></div>")
    var info = loadedModelsById[modelId]
    if (!info.isFullLoad) {
        loadAirplaneModelOwnerInfoByModelId(modelId) //refresh to get the utility rate
    }

    var empty = true

    var allAirplanes = $.merge($.merge($.merge([], info.assignedAirplanes), info.availableAirplanes), info.constructingAirplanes)
    $.each(allAirplanes, function( key, airplane ) {
        if (airplane[compareKey] == compareValue) {
            var airplaneId = airplane.id
            var li = $("<div style='float: left;' class='clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this), refreshBaseAfterAirplaneUpdate)'></div>").appendTo(airplanesDiv)
            var airplaneIcon = getAirplaneIcon(airplane, info.badConditionThreshold)

            enableAirplaneIconDrag(airplaneIcon, airplaneId, airplane.availableFlightMinutes != airplane.maxFlightMinutes)
            enableAirplaneIconDrop(airplaneIcon, airplaneId, "refreshBaseAfterAirplaneUpdate")
            li.append(airplaneIcon)
            empty = false
         }
    });

    if (empty) {
        airplanesDiv.append("<h4>-</h4>")
    }

    containerDiv.append(airplanesDiv)
}


function addAirplaneHangarDivByModel($containerDiv, modelInfo) {
    var $airplanesDiv = $("<div style= 'width : 100%; height : 70px; overflow: auto;'></div>")

    var allAirplanes = $.merge($.merge($.merge([], modelInfo.assignedAirplanes), modelInfo.availableAirplanes), modelInfo.constructingAirplanes)
    //group by base Id
    var airplanesByAirportId = {}

    $.each(allAirplanes, function( key, airplane ) {
        var airportId = airplane.homeAirportId
        var airplanesOfThisBase = airplanesByAirportId[airportId]
        if (!airplanesByAirportId[airportId]) {
           airplanesOfThisBase = []
           airplanesByAirportId[airportId] = airplanesOfThisBase
        }
        airplanesOfThisBase.push(airplane)
    })

    $.each(airplanesByAirportId, function( airportId, airplanes ) {
        var airportIata
        $.each(activeAirline.baseAirports, function(index, baseAirport) {
            if (baseAirport.airportId == airportId) {
                airportIata = baseAirport.airportCode
            }
        })
        var $airplanesByBaseDiv = $("<div style='width : 100%;'><div style='float: left; width: 35px;'>" + airportIata + ":</div></div>")
        $.each(airplanes, function( index, airplane ) {
            var airplaneId = airplane.id
            var li = $("<div style='float: left;' class='clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this), showAirplaneCanvas)'></div>").appendTo($airplanesByBaseDiv)
            var airplaneIcon = getAirplaneIcon(airplane, modelInfo.badConditionThreshold)
            li.append(airplaneIcon)
        })
        $airplanesByBaseDiv.append("<div style='clear:both; '></div>")
        $airplanesDiv.append($airplanesByBaseDiv)

    });

    $containerDiv.append($airplanesDiv)
}



function getAirplaneIconImg(airplane, badConditionThreshold, isAssigned) {
    var img = $("<img>")
    var src
    var condition = airplane.condition

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
	img.attr("src", src)
	return img
}

function getAirplaneIcon(airplane, badConditionThreshold, explicitIsAssigned) {
    var condition = airplane.condition
    var airplaneId = airplane.id
    var div = $("<div style='position: relative;'></div>")
    div.addClass("airplaneIcon")
    var isAssigned
    if (typeof explicitIsAssigned != 'undefined') {
        isAssigned = explicitIsAssigned
    } else {
        isAssigned = airplane.availableFlightMinutes != airplane.maxFlightMinutes
    }

    if (typeof explicitIsAssigned == 'undefined') {
        badConditionThreshold = airplane.badConditionThreshold
    }

    var img = getAirplaneIconImg(airplane, badConditionThreshold, isAssigned)
    div.append(img)

    //utilization label
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

	var utilizationDiv = $("<div class='utilization' style='position: absolute; right: 0; bottom: 0; color: #454544; background-color: " + color + "; font-size: 8px; font-weight: bold; display: none;'></div>")
	utilizationDiv.text(utilization)
	div.append(utilizationDiv)

	//condition label
	if (condition < 20) {
        color = "#FF9973"
    } else if (condition < 40) {
        color = "#FFC273"
    } else {
        color = "#8CB9D9"
    }


    var conditionDiv = $("<div class='condition' style='position: absolute; right: 0; top: 0; color:#454544; background-color: " + color + "; font-size: 8px; display: none;'></div>")
    conditionDiv.text(Math.floor(condition))

    div.attr("title", "#"+ airplaneId + " condition: " + condition.toFixed(2) + "% util: " + utilization + "%")
    div.append(conditionDiv)

    return div;
}

function enableAirplaneIconDrag(airplaneIcon, airplaneId, isAssigned) {
    //airplaneIcon.attr("ondrop", "onAirplaneSwapDrop(event, " + airplaneId + ")")
    //airplaneIcon.attr("ondragover", "event.preventDefault")
    airplaneIcon.attr("ondragstart", "onAirplaneDragStart(event, " + airplaneId + ", " + isAssigned + ")")
    airplaneIcon.attr("draggable", true)
}

function enableAirplaneIconDrop(airplaneIcon, airplaneId, refreshFunction) {
    airplaneIcon.attr("ondrop", "onAirplaneSwapDrop(event, " + airplaneId + ", " + refreshFunction + ")")
    airplaneIcon.attr("ondragover", "allowAirplaneIconDragOver(event)")
}

function isAirplaneIconEvent(event) {
    for (i = 0 ; i < event.dataTransfer.items.length; i ++) { //hacky way. since for security reason dataTransfer.getData() also returns empty
        if (event.dataTransfer.items[i].type === "airplane-id") {
            return true;
        }
    }
    return false;
}

function isAssignedAirplaneIconEvent(event) {
    for (i = 0 ; i < event.dataTransfer.items.length; i ++) { //hacky way. since for security reason dataTransfer.getData() also returns empty
        if (event.dataTransfer.items[i].type === "airplane-assigned") {
            return true;
        }
    }
    return false;
}

function allowAirplaneIconDragOver(event) {
    if (isAirplaneIconEvent(event)) { //only allow dropping if the item being dropped is an airplane icon
        event.preventDefault();
    }
}

function allowUnassignedAirplaneIconDragOver(event) {
    if (!isAssignedAirplaneIconEvent(event) && isAirplaneIconEvent(event)) { //only allow dropping if the airplane is unassigned
        event.preventDefault();
    }
}

function onAirplaneIconBaseDrop(event, airportId) {
  event.preventDefault();
  var airplaneId = event.dataTransfer.getData("airplane-id")
  if (airplaneId) {
    $.ajax({
                type: 'PUT',
                url: "airlines/" + activeAirline.id + "/airplanes/" + airplaneId + "/home/" + airportId,
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                async: false,
                success: function(result) {
                    refreshBaseAfterAirplaneUpdate()
                },
                error: function(jqXHR, textStatus, errorThrown) {
                        console.log(JSON.stringify(jqXHR));
                        console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
                }
            });
  }
}


function onAirplaneSwapDrop(event, currentAirplaneId, refreshFunction) {
  event.preventDefault();
  event.stopPropagation();

  var fromAirplaneId = currentAirplaneId
  var toAirplaneId = event.dataTransfer.getData("airplane-id")
  $.ajax({
          type: 'GET',
          url: "airlines/" + activeAirline.id + "/swap-airplanes/" + fromAirplaneId + "/" + toAirplaneId,
          contentType: 'application/json; charset=utf-8',
          dataType: 'json',
          success: function(result) {
            refreshFunction()
          },
          error: function(jqXHR, textStatus, errorThrown) {
                  console.log(JSON.stringify(jqXHR));
                  console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
          }
      });
}

function onAirplaneDragStart(event, airplaneId, isAssigned) {
  //event.dataTransfer.setData("text", airplaneId);
  event.dataTransfer.setData("airplane-id", airplaneId);
  if (isAssigned) {
    event.dataTransfer.setData("airplane-assigned", "true"); //do not set anything if it's not assigned
  }
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
	    	var replaceCost = model.price - airplane.sellValue
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
	highlightTab($('.airplaneCanvasTab'))

    var $selectedTab = $("#airplaneCanvas .detailsSelection.selected")
    selectAirplaneTab($selectedTab)
}

function showAirplaneMarket() {
    updateAirplaneModelTable()
}

function selectAirplaneTab($selectedTab) {
    $selectedTab.siblings().removeClass('selected')
    $selectedTab.addClass('selected')
    var selectedType = $selectedTab.data('type')

    loadAirplaneModels()
    loadAirplaneModelOwnerInfo()
    if (selectedType === 'market') {
      showAirplaneMarket()
    } else if (selectedType === 'hangar') {
      showAirplaneHangar()
    }

    var selectedDetails = $('#airplaneCanvas div.detailsGroup').children('.details.' + selectedType)
    selectedDetails.siblings().hide()
    selectedDetails.show()
}

function showAirplaneHangar() {
    loadAirplaneModelOwnerInfo()
    populatePreferredSuppliers()
    var $container = $('#airplaneCanvas .hangar .sectionContainer')
    $container.empty()
    populateHangarByModel($container)

    toggleUtilizationRate($container, $("#airplaneCanvas div.hangar .toggleUtilizationRateBox"))
    toggleCondition($container, $("#airplaneCanvas div.hangar .toggleConditionBox"))
}

function populatePreferredSuppliers() {
    $('#airplaneCanvas .hangar .preferredSupplier .supplierList').empty()
    $('#airplaneCanvas .hangar .preferredSupplier .discount').empty()
    $.ajax({
          type: 'GET',
          url: "airlines/" + activeAirline.id + "/preferred-suppliers",
          contentType: 'application/json; charset=utf-8',
          dataType: 'json',
          success: function(result) {
            var $container = $('#airplaneCanvas .preferredSuppliers')
            $.each(result, function(category, info) {
                var $categorySection = $container.find('.' + category)
                if ($categorySection.length > 0) { //Super sonic has no section for now...
                    $categorySection.find('.capacityRange').text(info.minCapacity + " - " + info.maxCapacity)
                    var $supplierList = $categorySection.find('.supplierList')
                    var $discount =  $categorySection.find('.discount')
                    $supplierList.empty()

                    $.each(info.ownership, function(manufacturer, ownedFamilies) {
                        var $manufacturerLabel = $('<span style="font-weight: bold;">' + manufacturer + '</span>')
                        var familyText = " : "
                        $.each(ownedFamilies, function(index, ownedFamily) {
                            if (index > 0) {
                                familyText += ", "
                            }
                            familyText += ownedFamily
                        })
                        var $familyList = $('<span></span>')
                        $familyList.text(familyText)

                        var $ownershipDivByManufacturer = $('<div style="margin-bottom: 10px;"></div>')
                        $ownershipDivByManufacturer.append($manufacturerLabel)
                        $ownershipDivByManufacturer.append($familyList)
                        $supplierList.append($ownershipDivByManufacturer)
                    })

                    if (info.discount) {
                        $discount.text(info.discount)
                    } else {
                        $discount.text('-')
                    }
                }
            })

          },
          error: function(jqXHR, textStatus, errorThrown) {
                  console.log(JSON.stringify(jqXHR));
                  console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
          }
      });

}
function populateHangarByModel($container) {
    $.each(loadedModelsOwnerInfo, function(index, modelOwnerInfo) {
        if (modelOwnerInfo.totalOwned > 0) {
            var modelId = modelOwnerInfo.id
            var $inventoryDiv = $("<div style='min-width : 300px; min-height : 85px; flex: 1;' class='section clickable'></div>")
            if (!modelOwnerInfo.isFullLoad) {
                loadAirplaneModelOwnerInfoByModelId(modelId) //refresh to get the utility rate
            }
            $inventoryDiv.bind('click', function() {
                selectAirplaneModel(loadedModelsById[modelId])
                $inventoryDiv.siblings().removeClass('selected')
                $inventoryDiv.addClass('selected')
            })
            $inventoryDiv.append("<h4>" + modelOwnerInfo.name  + "</h4>")
            addAirplaneHangarDivByModel($inventoryDiv, modelOwnerInfo)
            $container.append($inventoryDiv)
        }
    })
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

function promptSetFavorite() {
    var model = $('#setFavoriteModal').data("model")
    if (model.imageUrl) {
        var imageLocation = 'assets/images/airplanes/' + model.name.replace(/\s+/g, '-').toLowerCase() + '.png'
        $('#setFavoriteModal .modelIllustration img').attr('src', imageLocation)
        $('#setFavoriteModal .modelIllustration a').attr('href', model.imageUrl)
        $('#setFavoriteModal .modelIllustration').show()

    } else {
        $('#setFavoriteModal .modelIllustration').hide()
    }

    $('#setFavoriteModal .modelName').text(model.name)
    $('#setFavoriteModal .existingFavorite').hide();
    $('#setFavoriteModal .rejection').hide();

    $.ajax({
        type: 'GET',
        url: "airlines/" + activeAirline.id + "/favorite-model/" + model.id,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            if (result.priceDiscount) {
                $('#setFavoriteModal .priceDiscount').text(result.priceDiscount * 100 + "%")
            } else {
                $('#setFavoriteModal .priceDiscount').text("-")
            }
            if (result.constructionTimeDiscount) {
                $('#setFavoriteModal .constructionTimeDiscount').text(result.constructionTimeDiscount * 100 + "%")
            } else {
                $('#setFavoriteModal .constructionTimeDiscount').text("-")
            }

            var rejection = $('#setFavoriteModal').data("rejection")
            if (rejection) {
                $('#setFavoriteModal .rejection .reason').text(rejection)
                $('#setFavoriteModal .rejection').show();
                disableButton($("#setFavoriteModal .confirm"), rejection)
            } else {
                if (result.existingFavorite) {
                    $('#setFavoriteModal .existingFavoriteModelName').text(result.existingFavorite.name)
                    $('#setFavoriteModal .existingFavorite').show();
                }
                enableButton($("#setFavoriteModal .confirm"))
            }
            $('#setFavoriteModal').fadeIn(200)
        },
         error: function(jqXHR, textStatus, errorThrown) {
                        console.log(JSON.stringify(jqXHR));
                        console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function confirmSetFavorite() {
    var airlineId = activeAirline.id
    var model = $('#setFavoriteModal').data("model")
    $.ajax({
            type: 'PUT',
            url: "airlines/" + activeAirline.id + "/favorite-model/" + model.id,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            async: false,
            success: function(result) {
                closeModal($('#setFavoriteModal'))
                showAirplaneCanvas()
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
    });
}

function getAssignedAirplanesCount(compareKey, compareValue, modelId) {
    var count = 0;
    $.each(loadedModelsById[modelId].assignedAirplanes, function( key, airplane ) {
            if (airplane[compareKey] == compareValue) {
                count ++
             }
        });

    $.each(loadedModelsById[modelId].availableAirplanes, function( key, airplane ) {
        if (airplane[compareKey] == compareValue) {
            count ++
        }
    });

    $.each(loadedModelsById[modelId].constructingAirplanes, function( key, airplane ) {
        if (airplane[compareKey] == compareValue) {
            count ++
        }
    });
    return count
}
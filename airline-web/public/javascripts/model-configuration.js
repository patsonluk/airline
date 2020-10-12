function showAirplaneModelConfigurations(modelId) {
    var airlineId = activeAirline.id
    $.ajax({
        type: 'GET',
        url: "airlines/" + airlineId + "/configurations?modelId=" + modelId,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            loadedModelConfigInfo = result
            showAirplaneModelConfigurationsModal(result)
        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function refreshConfigurationAfterAirplaneUpdate() {
    //loadAirplaneModelOwnerInfo() //refresh the whole model screen - as the table might change
    loadAirplaneModelOwnerInfoByModelId(selectedModelId) //refresh the loaded airplanes on the selected model
    showAirplaneModelConfigurations(selectedModelId)
}


function showAirplaneModelConfigurationsModal(modelConfigurationInfo) {
    $("#modelConfigurationModal .configContainer").empty()
    var spaceMultipliers = modelConfigurationInfo.spaceMultipliers
    var model = modelConfigurationInfo.model

    $("#modelConfigurationModal .modelName").text(model.name)

    $.each(modelConfigurationInfo.configurations, function(index, configuration) {
        var configurationDiv = $("<div style='width : 95%; min-height : 130px;' class='section config'></div>")
        configurationDiv.data("existingConfiguration", { "economy" : configuration.economy, "business" : configuration.business, "first" : configuration.first}) //for revert
        configurationDiv.data("spaceMultipliers", spaceMultipliers) //for revert
        configurationDiv.data("configuration", configuration)
        configurationDiv.data("model", model) //for revert


        var controllerDiv = $("<div style='width : 100%; margin-bottom : 10px;'></div>")
        configurationDiv.append(controllerDiv)

        var assignedAirplanesCount = getAssignedAirplanesCount("configurationId", configuration.id, model.id)
        var seatConfigurationDiv = $("<div class='seatConfigurationGauge' style='float:left; width : 80%; '></div>")
        controllerDiv.append(seatConfigurationDiv)

        var iconsDiv = $("<div style='float:left; width : 20%;'></div>")

        if (configuration.isDefault) {
         iconsDiv.append($('<span style="margin: 2px;"><img src="assets/images/icons/24px/star.png" title="This is the default configuration"></span>'))
        } else {
         iconsDiv.append($('<span class="button" onclick="promptConfirm(\'Do you want to save and set this configuration as default?\', saveAndSetDefaultConfiguration, $(this).closest(\'.config\').data(\'configuration\'))" style="margin: 2px;"><img src="assets/images/icons/24px/star-empty.png" title="Set as default"></span>'))
        }
        iconsDiv.append($('<span class="button" onclick="promptConfirm(\'Do you want to change this configuration? This will affect ' + assignedAirplanesCount + ' airplane(s)\', saveConfiguration, $(this).closest(\'.config\').data(\'configuration\'))" style="margin: 2px;"><img src="assets/images/icons/24px/tick.png" title="Save this configuration"></span>'))
        iconsDiv.append($('<span class="button" onclick="refreshConfiguration($(this).closest(\'.config\'), $(this).closest(\'.config\').data(\'existingConfiguration\'), true)" style="margin: 2px;"><img src="assets/images/icons/24px/arrow-circle-135.png" title="Revert changes"></span>'))
        if (configuration.isDefault) {
            iconsDiv.append($('<span style="margin: 2px;"><img src="assets/images/icons/24px/cross-grey.png" title="Cannot delete default configuration"></span>'))
        } else {
            iconsDiv.append($('<span class="button" onclick="promptConfirm(\'Do you want to delete this configuration? ' + assignedAirplanesCount + ' airplane(s) with this configuration will be switched to default configuration\', deleteConfiguration, $(this).closest(\'.config\').data(\'configuration\'))" style="margin: 2px;"><img src="assets/images/icons/24px/cross.png" title="Delete this configuration"></span>'))
        }

        controllerDiv.append(iconsDiv)
        controllerDiv.append($("<div style='clear : both;'></div>"))
        var manualInputDiv = $("<div class='manual-inputs'></div>")
        var perInputSpan
        perInputSpan = $('<div style="margin-left: 10px; margin-right: 10px; display: inline-block;" class="economy">Economy: <input type="text" class="economyInput" maxlength="3" size="3" onkeyup="onManualInputUpdate($(this).closest(\'.config\'), \'economy\', $(this).val())"></div>')
        perInputSpan.append($('<span class="button" onclick="toggleInputLock($(this).closest(\'.config\'), \'economy\')"><img src="assets/images/icons/lock-unlock.png" title="Lock this value"></span>'))
        manualInputDiv.append(perInputSpan)
        perInputSpan = $('<div style="margin-left: 10px; margin-right: 10px; display: inline-block;" class="business">Business: <input type="text" class="businessInput" maxlength="3" size="3" onkeyup="onManualInputUpdate($(this).closest(\'.config\'), \'business\', $(this).val())"></div>')
        perInputSpan.append($('<span class="button" onclick="toggleInputLock($(this).closest(\'.config\'), \'business\')"><img src="assets/images/icons/lock-unlock.png" title="Lock this value"></span>'))
        manualInputDiv.append(perInputSpan)
        perInputSpan = $('<div style="margin-left: 10px; margin-right: 10px; display: inline-block;" class="first">First: <input type="text" class="firstInput" maxlength="3" size="3" onkeyup="onManualInputUpdate($(this).closest(\'.config\'), \'first\', $(this).val())"></div>')
        perInputSpan.append($('<span class="button" onclick="toggleInputLock($(this).closest(\'.config\'), \'first\')"><img src="assets/images/icons/lock-unlock.png" title="Lock this value"></span>'))
        manualInputDiv.append(perInputSpan)
        controllerDiv.append(manualInputDiv)


        addAirplaneInventoryDivByConfiguration(configurationDiv, model.id)
        configurationDiv.attr("ondragover", "allowAirplaneIconDragOver(event)")
        configurationDiv.attr("ondrop", "onAirplaneIconConfigurationDrop(event, " + configuration.id + ")");

        $("#modelConfigurationModal .configContainer").append(configurationDiv)

        //set values of the injected elements here
        configurationDiv.find('.economyInput').val(configuration.economy)
        configurationDiv.find('.businessInput').val(configuration.business)
        configurationDiv.find('.firstInput').val(configuration.first)
        plotSeatConfigurationGauge(seatConfigurationDiv, configuration, model.capacity, spaceMultipliers, updateConfigurationGauge(configurationDiv))
    })

    for (i = 0 ; i < modelConfigurationInfo.maxConfigurationCount - modelConfigurationInfo.configurations.length; i ++) { //pad the rest with empty div
        var configurationDiv = $("<div style='width : 95%; min-height : 130px; position: relative;' class='section config'></div>")
        var promptDiv = ("<div style='position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%);'><span class='button' onclick='toggleNewConfiguration(selectedModel, " + (modelConfigurationInfo.configurations.length == 0 ? "true" : "false") + ")'><img src='assets/images/icons/24px/plus.png' title='Add new configuration'><div style='float:right'><h3>Add New Configuration</h3></div></span></div>")


        configurationDiv.append(promptDiv)
        $("#modelConfigurationModal .configContainer").append(configurationDiv)
    }
    toggleUtilizationRate($("#modelConfigurationModal"), $("#modelConfigurationModal .toggleUtilizationRateBox"))
    toggleCondition($("#modelConfigurationModal"), $("#modelConfigurationModal .toggleConditionBox"))

    $('#modelConfigurationModal').fadeIn(200)
}





function addAirplaneInventoryDivByConfiguration(configurationDiv, modelId) {
    var airplanesDiv = $("<div style= 'width : 100%; height : 50px; overflow: auto;'></div>")
    var configurationId = configurationDiv.data("configuration").id
    var info = loadedModelsById[modelId]
    if (!info.isFullLoad) {
        loadAirplaneModelOwnerInfoByModelId(modelId) //refresh to get the utility rate
    }

    var allAirplanes = $.merge($.merge($.merge([], info.assignedAirplanes), info.availableAirplanes), info.constructingAirplanes)
    $.each(allAirplanes, function( key, airplane ) {
        if (airplane.configurationId == configurationId) {
            var airplaneId = airplane.id
            var li = $("<div style='float: left;' class='clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this), refreshConfigurationAfterAirplaneUpdate)'></div>").appendTo(airplanesDiv)
            var airplaneIcon = getAirplaneIcon(airplane, info.badConditionThreshold)
            enableAirplaneIconDrag(airplaneIcon, airplaneId)
            enableAirplaneIconDrop(airplaneIcon, airplaneId, "refreshConfigurationAfterAirplaneUpdate")
            li.append(airplaneIcon)
         }
    });


    configurationDiv.append(airplanesDiv)
}

function toggleInputLock(configurationDiv, newLockedClass) {
    var existingLockedClass = configurationDiv.data("locked-class")
    if (existingLockedClass != newLockedClass) {
        if (existingLockedClass) {
            configurationDiv.find('.manual-inputs .' + existingLockedClass + ' img').attr("src", "assets/images/icons/lock-unlock.png") //unlock this
            configurationDiv.find('.manual-inputs .' + existingLockedClass + ' input').prop("disabled", false)
        }

        configurationDiv.find('.manual-inputs .' + newLockedClass + ' img').attr("src", "assets/images/icons/lock.png") //lock this
        configurationDiv.find('.manual-inputs .' + newLockedClass + ' input').prop("disabled", true)
        configurationDiv.data("locked-class", newLockedClass)
    } else { //was locked, now unlock it
        configurationDiv.find('.manual-inputs .' + newLockedClass + ' img').attr("src", "assets/images/icons/lock-unlock.png") //unlock this
        configurationDiv.find('.manual-inputs .' + newLockedClass + ' input').prop("disabled", false)
        configurationDiv.removeData("locked-class")
    }
}

function toggleNewConfiguration(model, isDefault) {
       var configuration = { "id" : 0, "model" : model, "economy" : model.capacity, "business" : 0, "first" : 0 , "isDefault" : isDefault}
       saveConfiguration(configuration)
}

function refreshConfiguration(configurationDiv, values, resetLocks) {
     var seatConfigurationDiv = configurationDiv.find(".seatConfigurationGauge")

     var configuration = configurationDiv.data("configuration")
     configuration.economy = values.economy
     configuration.business = values.business
     configuration.first = values.first
     configurationDiv.find('.economyInput').val(values.economy)
     configurationDiv.find('.businessInput').val(values.business)
     configurationDiv.find('.firstInput').val(values.first)

     var model = configurationDiv.data("model")
     var spaceMultipliers = configurationDiv.data("spaceMultipliers")

     plotSeatConfigurationGauge(seatConfigurationDiv, configuration, model.capacity, spaceMultipliers, updateConfigurationGauge(configurationDiv))

     if (resetLocks) {
        configurationDiv.find('.manual-inputs img').attr("src", "assets/images/icons/lock-unlock.png") //unlock this
        configurationDiv.removeData('locked-class')
     }
}

function saveAndSetDefaultConfiguration(configuration) {
    configuration.isDefault = true
    saveConfiguration(configuration)
}

function saveConfiguration(configuration) {
    var airlineId = activeAirline.id

    $.ajax({
            type: 'PUT',
            url: "airlines/" + airlineId + "/configurations?modelId=" + configuration.model.id + "&configurationId=" + configuration.id + "&economy=" + configuration.economy + "&business=" + configuration.business + "&first=" + configuration.first + "&isDefault=" + configuration.isDefault,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(result) {
                showAirplaneModelConfigurations(configuration.model.id)
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
}

function deleteConfiguration(configuration) {
    var airlineId = activeAirline.id

    $.ajax({
            type: 'DELETE',
            url: "airlines/" + airlineId + "/configurations/" + configuration.id,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(result) {
                refreshConfigurationAfterAirplaneUpdate()
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
}

function updateConfigurationGauge(configurationDiv) {
    return function(configuration) {
       configurationDiv.find('.economyInput').val(configuration.economy)
       configurationDiv.find('.businessInput').val(configuration.business)
       configurationDiv.find('.firstInput').val(configuration.first)
    }
}

function onManualInputUpdate(configurationDiv, changedClass, newValue) {
    var model = configurationDiv.data("model")
    var spaceMultipliers = configurationDiv.data("spaceMultipliers")
    var configuration = configurationDiv.data("configuration")
    var lockedClass = configurationDiv.data("locked-class")

    var success = computeConfiguration(configuration, model, spaceMultipliers, lockedClass, changedClass, newValue)
    refreshConfiguration(configurationDiv, configuration, false)
}

function computeConfiguration(existingConfiguration, model, spaceMultipliers, lockedClass, changedClass, newValue) {
    var newValue
    if (newValue === "") {
        newValue = 0
    } else {
        newValue = parseInt(newValue)
    }

    if (isNaN(newValue)) {
        return false
    }

    if (newValue < 0) {
        return false
    }
    var maxSpace = model.capacity
    var oldValue = existingConfiguration[changedClass]

    if (spaceMultipliers[changedClass] * newValue > maxSpace) { //reject
        return false
    }


    var linkClasses = ["economy", "business", "first"]
    var newConfig = {}
    newConfig[changedClass] = newValue
    var remainingSpace = maxSpace - spaceMultipliers[changedClass] * newValue
    if (lockedClass) { //if there's locked class, then it always has priority
         newConfig[lockedClass] = existingConfiguration[lockedClass]
         remainingSpace -= existingConfiguration[lockedClass] * spaceMultipliers[lockedClass]
    }

    $.each(linkClasses, function(index, linkClass){
        if (linkClass != changedClass) {
            var existingValue = existingConfiguration[linkClass]
            if (linkClass != lockedClass) { //then we can adjust
                var newValue = Math.floor(remainingSpace / spaceMultipliers[linkClass])
                newConfig[linkClass] = newValue
                remainingSpace -= newValue * spaceMultipliers[linkClass]
            }
        }
    })
    //verify result
    var totalSpace = 0
    $.each(linkClasses, function(index, linkClass){
        totalSpace += newConfig[linkClass] * spaceMultipliers[linkClass]
    })
    if (totalSpace > model.capacity) { //failed
        return false
    } else {
       $.each(linkClasses, function(index, linkClass){
           existingConfiguration[linkClass] = newConfig[linkClass]
       })
       return true
    }
}

function onAirplaneIconConfigurationDrop(event, configurationId) {
  event.preventDefault();
  var airplaneId = event.dataTransfer.getData("airplane-id")
  if (airplaneId) {
    $.ajax({
              type: 'PUT',
              url: "airlines/" + activeAirline.id + "/airplanes/" + airplaneId + "/configuration/" + configurationId,
              contentType: 'application/json; charset=utf-8',
              dataType: 'json',
              async: false,
              success: function(result) {
                  refreshConfigurationAfterAirplaneUpdate()
              },
              error: function(jqXHR, textStatus, errorThrown) {
                      console.log(JSON.stringify(jqXHR));
                      console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
              }
          });
  }
}
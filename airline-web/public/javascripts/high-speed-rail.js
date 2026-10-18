var HSR_CAR_CLASS_CYCLE = ['economy', 'business', 'first'] //clicking on a train car cycles through Y -> J -> F -> Y
var HSR_CAR_CLASS_ORDER = { 'first' : 0, 'business' : 1, 'economy' : 2 } //first class always on the left, economy last
var HSR_CAR_CLASS_LABEL = { 'economy' : 'Economy', 'business' : 'Business', 'first' : 'First' }

/**
 * High speed rail configuration modal, for HSR departing from the fromAirportId (base airport of the current airline)
 */
function showHighSpeedRailModal(fromAirportId) {
    loadHighSpeedRailOptions(fromAirportId, null, function() {
        $('#highSpeedRailModal').fadeIn(200)
    })
}

function loadHighSpeedRailOptions(fromAirportId, selectedToAirportId, callback) {
    var $modal = $('#highSpeedRailModal')
    $.ajax({
        type: 'GET',
        url: "airlines/" + activeAirline.id + "/high-speed-rail-options/" + fromAirportId,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            $modal.data('hsrOptions', result)
            $modal.find('.fromAirport').html(getAirportSpan(result.fromAirport))

            var $table = $modal.find('.table.hsrOptions')
            $table.find('.table-row').remove()
            $modal.find('.hsrDetails').hide()

            $.each(result.options, function(index, option) {
                var $row = $('<div class="table-row clickable" style="width: 100%"></div>')
                var rail = option.rail
                $row.append($('<div class="cell">' + (rail ? getAirlineSpan(rail.airlineId, rail.airlineName) : '-') + '</div>'))
                $row.append($('<div class="cell">' + getAirportSpan(option.airport) + '</div>'))
                $row.append($('<div class="cell" align="right">' + commaSeparateNumber(option.airport.population) + '</div>'))
                $row.append($('<div class="cell" align="right">' + option.distance + ' km</div>'))
                $row.append($('<div class="cell" align="right">' + (rail ? getHighSpeedRailCarsText(rail.cars) : '-') + '</div>'))
                $row.append($('<div class="cell" align="right">' + (rail ? getHighSpeedRailCapacityText(rail.capacity) : '-') + '</div>'))
                if (rail) {
                    $row.addClass('hsrBuilt')
                }
                $row.data('option', option)
                $row.click(function() {
                    selectHighSpeedRailOption($(this))
                })
                $table.append($row)

                if (selectedToAirportId && option.airport.id == selectedToAirportId) {
                    selectHighSpeedRailOption($row)
                }
            })
            if (result.options.length == 0) {
                $table.append('<div class="table-row"><div class="cell">-</div><div class="cell">No airport within range</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div></div>')
            }
            if (callback) {
                callback()
            }
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

function selectHighSpeedRailOption($row) {
    var $modal = $('#highSpeedRailModal')
    var $details = $modal.find('.hsrDetails')
    var hsrOptions = $modal.data('hsrOptions')
    var option = $row.data('option')
    var rail = option.rail

    $modal.find('.table.hsrOptions .table-row').removeClass('selected')
    $row.addClass('selected')

    var cars = []
    if (rail) { //show the actual allocation
        $.each(HSR_CAR_CLASS_CYCLE, function(index, carClass) {
            for (var i = 0; i < rail.cars[carClass]; i ++) {
                cars.push(carClass)
            }
        })
    } else { //all economy by default
        for (var i = 0; i < hsrOptions.carCount; i ++) {
            cars.push('economy')
        }
    }
    $details.data('option', option)
    $details.data('cars', cars)

    $details.find('.fundedBy').html(rail ? getAirlineSpan(rail.airlineId, rail.airlineName) : '-')
    $details.find('.destination').html(getAirportSpan(option.airport) + ' (' + option.distance + ' km)')
    $details.find('.constructionCost').text('$' + commaSeparateNumber(option.buildCost))
    $details.find('.carCount').text(hsrOptions.carCount)
    $details.find('.rejection').hide()

    var $buildButton = $details.find('.buildButton')
    var $updateButton = $details.find('.updateButton')
    var $removeButton = $details.find('.removeButton')
    $buildButton.hide()
    $updateButton.hide()
    $removeButton.hide()
    if (!rail) {
        $buildButton.show()
    } else if (rail.airlineId == activeAirline.id) {
        $updateButton.show()
        $removeButton.show()
    } //funded by other airline, no action allowed

    //render as read only first, then check if the current airline can build/update it
    $details.data('editable', false)
    renderHighSpeedRailCars()
    $details.show()

    $.ajax({
        type: 'POST',
        url: "airlines/" + activeAirline.id + "/high-speed-rail-consideration",
        contentType: 'application/json; charset=utf-8',
        data: JSON.stringify(getHighSpeedRailInput(hsrOptions.fromAirport.id, option, cars)),
        dataType: 'json',
        success: function(consideration) {
            if ($details.data('option') !== option) { //selection changed since
                return
            }
            if (consideration.rejection) {
                $details.find('.rejection .warning').text(consideration.rejection)
                $details.find('.rejection').show()
                disableButton($buildButton, consideration.rejection)
                disableButton($updateButton, consideration.rejection)
            } else {
                enableButton($buildButton)
                enableButton($updateButton)
                $details.data('editable', true)
                renderHighSpeedRailCars()
            }
        },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function renderHighSpeedRailCars() {
    var $details = $('#highSpeedRailModal .hsrDetails')
    var cars = $details.data('cars')
    var editable = $details.data('editable')
    var seatsPerCar = $('#highSpeedRailModal').data('hsrOptions').seatsPerCar

    cars.sort(function(a, b) { return HSR_CAR_CLASS_ORDER[a] - HSR_CAR_CLASS_ORDER[b] })

    var $configuration = $details.find('.configuration')
    $configuration.empty()
    $.each(cars, function(index, carClass) {
        var title = HSR_CAR_CLASS_LABEL[carClass] + ' car - ' + commaSeparateNumber(seatsPerCar[carClass]) + ' seats per week'
        var $car = $('<img src="assets/images/icons/train-car-' + carClass + '.svg" style="margin-right: 2px; vertical-align: middle;">')
        if (editable) {
            $car.addClass('clickable')
            $car.attr('title', title + '. Click to change class')
            $car.click(function() {
                cars[index] = HSR_CAR_CLASS_CYCLE[(HSR_CAR_CLASS_CYCLE.indexOf(carClass) + 1) % HSR_CAR_CLASS_CYCLE.length]
                renderHighSpeedRailCars()
            })
        } else {
            $car.attr('title', title)
        }
        $configuration.append($car)
    })

    var carsByClass = getHighSpeedRailCarsByClass(cars)
    var capacity = {}
    $.each(HSR_CAR_CLASS_CYCLE, function(index, carClass) {
        capacity[carClass] = carsByClass[carClass] * seatsPerCar[carClass]
    })
    $details.find('.capacity').text(getHighSpeedRailCapacityText(capacity))
}

function getHighSpeedRailCarsByClass(cars) {
    var carsByClass = { 'economy' : 0, 'business' : 0, 'first' : 0 }
    $.each(cars, function(index, carClass) {
        carsByClass[carClass] ++
    })
    return carsByClass
}

function getHighSpeedRailInput(fromAirportId, option, cars) {
    var carsByClass = getHighSpeedRailCarsByClass(cars)
    var input = {
        "fromAirportId" : fromAirportId,
        "toAirportId" : option.airport.id,
        "economyCars" : carsByClass.economy,
        "businessCars" : carsByClass.business,
        "firstCars" : carsByClass.first
    }
    if (option.rail) {
        input.linkId = option.rail.id
    }
    return input
}

function getHighSpeedRailCarsText(cars) {
    return cars.economy + ' / ' + cars.business + ' / ' + cars.first
}

function getHighSpeedRailCapacityText(capacity) {
    return commaSeparateNumber(capacity.economy) + ' / ' + commaSeparateNumber(capacity.business) + ' / ' + commaSeparateNumber(capacity.first)
}

function getSelectedHighSpeedRailInput() {
    var $modal = $('#highSpeedRailModal')
    var $details = $modal.find('.hsrDetails')
    return getHighSpeedRailInput($modal.data('hsrOptions').fromAirport.id, $details.data('option'), $details.data('cars'))
}

function promptBuildHighSpeedRail() {
    var option = $('#highSpeedRailModal .hsrDetails').data('option')
    promptConfirm("Build high speed rail to " + getAirportText(option.airport.city, option.airport.iata) + " for $" + commaSeparateNumber(option.buildCost) + "?", buildHighSpeedRail)
}

function buildHighSpeedRail() {
    var input = getSelectedHighSpeedRailInput()
    submitHighSpeedRailChange('PUT', "airlines/" + activeAirline.id + "/high-speed-rails", input, input.toAirportId)
}

function updateHighSpeedRail() {
    var input = getSelectedHighSpeedRailInput()
    submitHighSpeedRailChange('PUT', "airlines/" + activeAirline.id + "/high-speed-rails/" + input.linkId, input, input.toAirportId)
}

function promptRemoveHighSpeedRail() {
    var option = $('#highSpeedRailModal .hsrDetails').data('option')
    promptConfirm("Remove high speed rail to " + getAirportText(option.airport.city, option.airport.iata) + "? There will be no refund on the construction cost.", removeHighSpeedRail)
}

function removeHighSpeedRail() {
    var option = $('#highSpeedRailModal .hsrDetails').data('option')
    submitHighSpeedRailChange('DELETE', "airlines/" + activeAirline.id + "/high-speed-rails/" + option.rail.id, null, null)
}

function submitHighSpeedRailChange(type, url, input, selectedToAirportId) {
    var fromAirportId = $('#highSpeedRailModal').data('hsrOptions').fromAirport.id
    var $details = $('#highSpeedRailModal .hsrDetails')
    $.ajax({
        type: type,
        url: url,
        contentType: 'application/json; charset=utf-8',
        data: input ? JSON.stringify(input) : null,
        dataType: 'json',
        success: function(result) {
            refreshPanels(activeAirline.id)
            if (activeAirport) {
                updateFacilityIcons(activeAirport)
                loadAirportHighSpeedRails(activeAirport)
            }
            loadHighSpeedRailOptions(fromAirportId, selectedToAirportId)
        },
        error: function(jqXHR, textStatus, errorThrown) {
            if (jqXHR.status == 400 && jqXHR.responseText) { //rejected
                $details.find('.rejection .warning').text(jqXHR.responseText)
                $details.find('.rejection').show()
            }
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

/**
 * For airport details: number of HSR from/to this airport, and the modal listing them
 */
function loadAirportHighSpeedRails(airport) {
    $.ajax({
        type: 'GET',
        url: "airports/" + airport.id + "/high-speed-rails",
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(rails) {
            $('#airportHighSpeedRailModal .table.airportHsrs').data('rails', rails) //set the loaded data to modal as well
            $('#airportDetailsHighSpeedRailCount').text(rails.length)
        },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function showAirportHighSpeedRailModal() {
    var $table = $('#airportHighSpeedRailModal .table.airportHsrs')
    $table.find('.table-row').remove()

    var rails = $table.data('rails')
    $.each(rails, function(index, rail) {
        var fromAirport = { "city" : rail.fromAirportCity, "iata" : rail.fromAirportCode, "countryCode" : rail.fromCountryCode }
        var toAirport = { "city" : rail.toAirportCity, "iata" : rail.toAirportCode, "countryCode" : rail.toCountryCode }
        var $row = $('<div class="table-row" style="width: 100%"></div>')
        $row.append($('<div class="cell">' + getAirlineSpan(rail.airlineId, rail.airlineName) + '</div>'))
        $row.append($('<div class="cell">' + getAirportSpan(fromAirport) + '</div>'))
        $row.append($('<div class="cell">' + getAirportSpan(toAirport) + '</div>'))
        $row.append($('<div class="cell" align="right">' + commaSeparateNumber(rail.toAirportPopulation) + '</div>'))
        $row.append($('<div class="cell" align="right">' + rail.distance + ' km</div>'))
        $row.append($('<div class="cell" align="right">' + getHighSpeedRailCarsText(rail.cars) + '</div>'))
        $row.append($('<div class="cell" align="right">' + getHighSpeedRailCapacityText(rail.capacity) + '</div>'))
        $row.append($('<div class="cell" align="right">' + commaSeparateNumber(rail.passenger) + '</div>'))
        $table.append($row)
    })
    if (rails.length == 0) {
        $table.append('<div class="table-row"><div class="cell">-</div><div class="cell">-</div><div class="cell">No High Speed Rail</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div></div>')
    }
    $('#airportHighSpeedRailModal').fadeIn(200)
}

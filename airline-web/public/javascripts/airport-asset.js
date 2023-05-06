function showAirportAssets(airport) {
  var $assetsDetailsDiv = $('#airportCanvas div.assetsDetails').empty()

  $.ajax({
  		type: 'GET',
  		url: "airports/" + airport.id + "/assets",
  		contentType: 'application/json; charset=utf-8',
  		dataType: 'json',
  	    success: function(assets) {
  	        $.each(assets, function(index, asset) {
                var $assetDiv = $('<div style="min-height : 85px; max-width : 270px;" class="section clickable">')
                $assetDiv.click( function() {
                    showAssetModal(asset)
                    $("#airportAssetDetailsModal").data('postUpdateFunc', function() {
                        showAirportDetails(asset.airport.id)
                    })

                })

                if (asset.status != "BLUEPRINT") {
                    $assetDiv.addClass('selected')
                } else {
                    $assetDiv.addClass('blueprint')
                }
                var $title = $('<h5>' + asset.name  + '</h5>')
                $assetDiv.append($title)
                var $assetImageDiv = $('<div style="position: relative;"></div>')
                var $assetImage = $('<img style="max-width:100%">')
                $assetImage.attr('src', 'assets/images/airport-assets/' + asset.assetType + '.png')
                $assetImageDiv.append($assetImage)


                if (asset.airline) {
                    var $airlineLogo = $(getAirlineLogoImg(asset.airline.id))
                    $airlineLogo.attr('title', asset.airline.name)
                    $airlineLogo.css('position', 'absolute')
                    $airlineLogo.css('top', '0')
                    $airlineLogo.css('right', '0')

                    $assetImageDiv.append($airlineLogo)

                    var fullStarSource = "assets/images/icons/star.png"
                    var halfStarSource = "assets/images/icons/star-half.png"
                    var $levelBar = getHalfStepImageBarByValue(fullStarSource, halfStarSource, 1, asset.level).css({ 'position' : 'absolute', 'bottom' : '0', 'right' : '0'})
                    $assetImageDiv.append($levelBar)
                }

                if (asset.status === "UNDER_CONSTRUCTION") {
                    var $constructionLogo = $('<img src="assets/images/icons/construction.gif">')
                    $constructionLogo.attr('title', asset.name + " is under construction")
                    $constructionLogo.css('position', 'absolute')
                    $constructionLogo.css('bottom', '0')
                    $constructionLogo.css('left', '0')
                    $assetImageDiv.append($constructionLogo)
                }

                $assetDiv.append($assetImageDiv)

                var $costDiv = $('<h5>$' +  commaSeparateNumber(asset.cost) + '</h5>')
                $assetDiv.append($costDiv)

                $.each(asset.boosts, function(index, boost) {
                    var $boostSpan = $('<span style="white-space: nowrap;"></span>')
                    var $boostImage = $('<img src=assets/images/icons/airport-features/' + boost.boostType + '.png style="vertical-align: middle;">')
                    $boostImage.attr('title', boost.label)
                    $boostSpan.append($boostImage)
                    var boostText = commaSeparateNumber(boost.value)
                    if (boost.value > 0) {
                        boostText = "+" + boostText
                    } else {
                        boostText = "-" + boostText
                    }

                    $boostSpan.append($('<span>' + boostText + '</span>'))
                    $assetDiv.append($boostSpan)
                })
                $assetsDetailsDiv.append($assetDiv)
  	        })
  	    },
          error: function(jqXHR, textStatus, errorThrown) {
  	            console.log(JSON.stringify(jqXHR));
  	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
  	    }
  	});
}

function downgradeAsset(asset) {
    buildOrModifyAsset(asset, true)
}

function buildOrModifyAsset(asset, downgrade) {
	var url = "airlines/" + activeAirline.id + (downgrade ? "/airport-asset/downgrade/" : "/airport-asset/") + asset.id
	var assetData = {
			"name" : $("#airportAssetDetailsModal .assetNameInput").val(),
			}
	$.ajax({
		type: 'PUT',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    data: JSON.stringify(assetData),
	    dataType: 'json',
	    success: function(result) {
	    	if (result.nameRejection) {
	    		$('#airportAssetDetailsModal .assetNameWarningDiv .warning').text(result.nameRejection)
	    		$('#airportAssetDetailsModal .assetNameWarningDiv').show()
	    	} else {
	    		refreshPanels(activeAirline.id)
	    		showAssetModal(result)
	    		var postUpdateFunc = $("#airportAssetDetailsModal").data('postUpdateFunc')
	    		if (postUpdateFunc) {
	    		    postUpdateFunc()
	    		}
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


function sellAsset(asset) {
	var url = "airlines/" + activeAirline.id + "/airport-asset/" + asset.id
    	$.ajax({
    		type: 'DELETE',
    		url: url,
    	    contentType: 'application/json; charset=utf-8',
    	    success: function(result) {
                refreshPanels(activeAirline.id)
                closeModal($("#airportAssetDetailsModal"))
                var postUpdateFunc = $("#airportAssetDetailsModal").data('postUpdateFunc')
                if (postUpdateFunc) {
                    postUpdateFunc()
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


function showAssetModal(asset) {
    $('#airportAssetDetailsModal').data('asset', asset)
    $('#airportAssetDetailsModal img.assetImage').attr('src', 'assets/images/airport-assets/' + asset.assetType + '.png')
    var owned = asset.airline && activeAirline && activeAirline.id == asset.airline.id
    $('#airportAssetDetailsModal .name').text(asset.name)
    $('#airportAssetDetailsModal .assetName').text(asset.name)
    $('#airportAssetDetailsModal .assetNameWarningDiv').hide()

    //cleanup
    $('#airportAssetDetailsModal div.table-row.property').remove()

    //general info is available already
    $('#airportAssetDetailsModal .assetAirport').empty()
    $('#airportAssetDetailsModal .assetAirport').append(getAirportSpan(asset.airport))

    $('#airportAssetDetailsModal .assetType').text(asset.assetTypeLabel)
    $('#airportAssetDetailsModal .assetDescriptions').empty()
    $.each(asset.descriptions, function(index, description) {
        $('#airportAssetDetailsModal .assetDescriptions').append($('<div class="remarks">' + description + '</div>'))
    })

    if (asset.status === "BLUEPRINT") {
        $('#airportAssetDetailsModal .assetStatus').text("Blueprint")
    } else if (asset.status === "UNDER_CONSTRUCTION") {
        $('#airportAssetDetailsModal .assetStatus').text("Under Construction - Complete in " + asset.completionDuration + " week(s)")
    } else {
        $('#airportAssetDetailsModal .assetStatus').text("Operating")
    }

    if (asset.level) {
        $('#airportAssetDetailsModal .assetLevel').empty()
        var fullStarSource = "assets/images/icons/star.png"
        var halfStarSource = "assets/images/icons/star-half.png"
        $("#airportAssetDetailsModal .assetLevel").append(getHalfStepImageBarByValue(fullStarSource, halfStarSource, 1, asset.level).css({ 'display' : 'inline-block', 'vertical-align' : 'text-bottom'}))
    } else {
        $('#airportAssetDetailsModal .assetLevel').text('-')
    }

    $('#airportAssetDetailsModal .assetBoosts').empty() //wait for detailed load, as this might contain false boost from blueprints


    $('#airportAssetDetailsModal .assetExpense').text('-')
    $('#airportAssetDetailsModal .assetProfit').text('-')
    $('#airportAssetDetailsModal .assetRevenue').text('-')
    $('#airportAssetDetailsModal .assetPerformance').empty()

    $('#airportAssetDetailsModal .assetCost').text('$' + commaSeparateNumber(asset.cost))

    $('#airportAssetDetailsModal .cost').text('$' + commaSeparateNumber(asset.cost))
    $('#airportAssetDetailsModal .constructionDuration').text(asset.constructionDuration + " weeks")

    $('#airportAssetDetailsModal .assetOwner').empty()
    if (asset.airline) {
        if (owned) { //allow name input
            $('#airportAssetDetailsModal .assetNameInput').val(asset.name)
            $('#airportAssetDetailsModal .assetName').hide()
            $('#airportAssetDetailsModal .assetNameInputDiv').show()
            $('#airportAssetDetailsModal .assetOwnerRow').show()
        } else {
            $('#airportAssetDetailsModal .assetName').text(asset.name)
            $('#airportAssetDetailsModal .assetName').show()
            $('#airportAssetDetailsModal .assetNameInputDiv').hide()
            $('#airportAssetDetailsModal .assetOwnerRow').hide()
        }

        var airlineLogo = getAirlineLogoImg(asset.airline.id)
        $(airlineLogo).attr('title', asset.airline.name)
        $('#airportAssetDetailsModal .assetOwner').append(airlineLogo)
        $('#airportAssetDetailsModal .assetOwner').append($('<span>' + asset.airline.name + '</span>'))
        $('#airportAssetDetailsModal .sellValue').text("$" + commaSeparateNumber(asset.sellValue))

    } else { //blueprint
        $('#airportAssetDetailsModal .assetNameInput').val('')
        $('#airportAssetDetailsModal .assetName').hide()
        $('#airportAssetDetailsModal .assetNameInputDiv').show()
        $('#airportAssetDetailsModal .assetOwner').text('-')
        $('#airportAssetDetailsModal .sellValue').text('-')
        $('#airportAssetDetailsModal .assetOwnerRow').hide()
    }


    //hide all action button until the ajax call finishes
    $('#airportAssetDetailsModal .buildButton').hide()
    $('#airportAssetDetailsModal .upgradeButton').hide()
    $('#airportAssetDetailsModal .downgradeButton').hide()
    $('#airportAssetDetailsModal .sellButton').hide()

    var url
    if (activeAirline) {
        url = "airlines/" + activeAirline.id + "/airport-asset/" + asset.id
    } else {
        url = "airport-asset/" + asset.id
    }
    $.ajax({
        type: 'GET',
        url: url,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(assetDetails) {
            if (owned) {
                $('#airportAssetDetailsModal .upgradeButton').show()
                $('#airportAssetDetailsModal .downgradeButton').show()
                $('#airportAssetDetailsModal .sellButton').show()
            }
            if (!asset.airline) {
                $('#airportAssetDetailsModal .buildButton').show()
            }

            if (assetDetails.rejection) {
                disableButton($('#airportAssetDetailsModal .upgradeButton'), assetDetails.rejection)
                disableButton($('#airportAssetDetailsModal .buildButton'), assetDetails.rejection)
            } else {
                enableButton($('#airportAssetDetailsModal .upgradeButton'))
                enableButton($('#airportAssetDetailsModal .buildButton'))
            }
            if (assetDetails.downgradeRejection) {
                disableButton($('#airportAssetDetailsModal .downgradeButton'), assetDetails.downgradeRejection)
            } else {
                enableButton($('#airportAssetDetailsModal .downgradeButton'))
            }


            if (assetDetails.boosts && assetDetails.boosts.length > 0) {
                $('#airportAssetDetailsModal .assetBoosts').empty()
                 $.each(assetDetails.boosts, function(index, boost) {
                    var $boostSpan = $('<span style="white-space: nowrap;"></span>')
                    var $boostImage = $('<img src=assets/images/icons/airport-features/' + boost.boostType + '.png style="vertical-align: middle;">')
                    $boostImage.attr('title', boost.label)
                    $boostSpan.append($boostImage)
                    var boostText = commaSeparateNumber(boost.value)
                    if (boost.value > 0) {
                        boostText = "+" + boostText
                    } else {
                        boostText = "-" + boostText
                    }
                    $boostSpan.append($('<span>' + boostText + '</span>'))
                    $('#airportAssetDetailsModal .assetBoosts').append($boostSpan)
                })
            } else {
                $('#airportAssetDetailsModal .assetBoosts').text('-')
            }

            refreshBoostHistory(assetDetails.boostHistory, assetDetails.baseBoosts)
            //add ??? mystery rows
            if (asset.status === "UNDER_CONSTRUCTION") {
                $.each(assetDetails.baseBoosts, function(index, entry) {
                    var $row = $("<div class='table-row'></div>")

                    $row.append("<div class='cell'>" + assetDetails.level + "</div>")
                    var $boostImage = $('<img src="assets/images/icons/airport-features/' + entry.boostType + '.png" style="vertical-align: middle;">')
                    var $cell = $("<div class='cell'></div>")
                    $cell.append($boostImage)
                    $cell.append('<span>' + entry.label + '</span>')
                    $row.append($cell)
                    $row.append("<div class='cell' align='right'>???</div>")
                    $row.append("<div class='cell' align='right'>???</div>")
                    $row.append('<div class="cell"><img src="assets/images/icons/construction.gif" style="vertical-align: middle;"></div>')
                    $('#airportAssetDetailsModal .airportBoostHistoryTable .table-header').after($row)
                })
            }

            //add public/private properties - this is different per asset type
            $.each(assetDetails.publicProperties, function(key, value){
                var $row = $('<div class="table-row property"><div class="label" style="width: 50%"><h5>' + key + ':</h5></div><div class="value" style="width: 50%">' + value + '</div></div>')
                $('#airportAssetDetailsModal div.table-row.activeBoostsRow').after($row)
            })
            $.each(assetDetails.privateProperties, function(key, value){
                var $row = $('<div class="table-row property"><div class="label" style="width: 50%"><h5>' + key + ':</h5></div><div class="value" style="width: 50%">' + value + '</div></div>')
                $('#airportAssetDetailsModal div.table-row.activeBoostsRow').after($row)
            })

            if (typeof assetDetails.expense !== 'undefined') {
                $('#airportAssetDetailsModal .assetExpense').text('$' + commaSeparateNumber(assetDetails.expense))
                $('#airportAssetDetailsModal .assetProfit').text('$' + commaSeparateNumber(assetDetails.revenue - assetDetails.expense))
            }
            if (typeof assetDetails.revenue !== 'undefined') {
                $('#airportAssetDetailsModal .assetRevenue').text('$' + commaSeparateNumber(assetDetails.revenue))
            }
            var fullStarSource = "assets/images/icons/star.png"
            var $performanceBar = getHalfStepImageBarByValue(fullStarSource, null, 0.5, assetDetails.performanceApprox).css({ 'display' : 'inline-block', 'vertical-align' : 'text-bottom'})
            $('#airportAssetDetailsModal .assetPerformance').append($performanceBar)

            if (assetDetails.countryRanking) {
                showAssetVisitorDetails(assetDetails.countryRanking, { "Transit" : assetDetails.transitPax, "Final Destination" : assetDetails.destinationPax})
            } else {
                $('#airportAssetDetailsModal .visitorByAir').hide()
            }
        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });


    $('#airportAssetDetailsModal').fadeIn(200)
}

function showAssetVisitorDetails(countryRanking, paxByType) {
    plotPie(countryRanking, null , $("#airportAssetDetailsModal .visitorCompositionByCountryPie"), "countryName", "passengerCount")
    plotPie(paxByType, null , $("#airportAssetDetailsModal .visitorCompositionByPassengerTypePie"))
    $('#airportAssetDetailsModal .visitorByAir').show()

    var max = 5;
	var index = 0;
    $('#airportAssetDetailsModal .topCountryTable .table-row').remove()
    $.each(countryRanking, function(key, entry) {
        $('#airportAssetDetailsModal .topCountryTable').append("<div class='table-row data-row'><div class='cell' style='width: 70%;'>" + getCountryFlagImg(entry.countryCode) + entry.countryName
                   + "</div><div class='cell' style='width: 30%; text-align: right;'>" + commaSeparateNumber(entry.passengerCount) + "</div></div>")
        index ++;
        if (index >= max) {
            return false;
        }
    });

    $('#airportAssetDetailsModal .passengerTypeTable .table-row').remove()
    $.each(paxByType, function(key, entry) {
        $('#airportAssetDetailsModal .passengerTypeTable').append("<div class='table-row data-row'><div class='cell' style='width: 70%;'>" + key
                   + "</div><div class='cell' style='width: 30%; text-align: right;'>" + commaSeparateNumber(entry) + "</div></div>")
    });
}

function refreshBoostHistory(history, baseBoosts) {
    var $table = $('#airportAssetDetailsModal .airportBoostHistoryTable')
    $table.find('div.table-row').remove()

    $.each(history, function(index, entry) {
        var $row = $("<div class='table-row'></div>")

        $row.append("<div class='cell'>" + entry.level + "</div>")
        var $boostImage = $('<img src="assets/images/icons/airport-features/' + entry.boostType + '.png" style="vertical-align: middle;">')
        var $cell = $("<div class='cell'></div>")
        $cell.append($boostImage)
        $cell.append('<span>' + entry.label + '</span>')
        $row.append($cell)
        $row.append("<div class='cell' align='right'>+" + commaSeparateNumber(entry.gain) + "</div>")
        $row.append("<div class='cell' align='right'>" + commaSeparateNumber(entry.value) + "</div>")
        $upgradeFactorCell = $('<div class="cell"></div>')
        $upgradeFactorCell.append(getUpgradeFactorIcon(entry.upgradeFactor))
        $row.append($upgradeFactorCell)
        $table.append($row)
    })

    //append base boosts
    $.each(baseBoosts, function(index, entry) {
        var $row = $("<div class='table-row'></div>")

        $row.append("<div class='cell'>Base</div>")
        var $boostImage = $('<img src="assets/images/icons/airport-features/' + entry.boostType + '.png" style="vertical-align: middle;">')
        var $cell = $("<div class='cell'></div>")
        $cell.append($boostImage)
        $cell.append('<span>' + entry.label + '</span>')
        $row.append($cell)
        $row.append("<div class='cell' align='right'>-</div>")
        $row.append("<div class='cell' align='right'>" + commaSeparateNumber(entry.value) + "</div>")
        $row.append('<div class="cell"></div>')
        $table.append($row)
    })

    //in case no boosts at all from this property
    if (baseBoosts.length == 0) {
        $emptyRow = $('<div class="table-row"><div class="cell">-</div><div class="cell">-</div><div class="cell">-</div><div class="cell">-</div><div class="cell"></div></div>')
        $table.append($emptyRow)
    }
}


function getUpgradeFactorIcon(value) {
    var $icon = $('<img style="vertical-align: middle;">')
    if (value >= 0.9) {
        icon = "smiley-kiss.png"
        description = "Awesome"
    } else if (value >= 0.8) {
        icon = "smiley-lol.png"
        description = "Great"
    } else if (value >= 0.7) {
        icon = "smiley.png"
        description = "Good"
    } else if (value >= 0.5) {
        icon = "smiley-neutral.png"
        description = "So-so"
    } else if (value >= 0.3) {
        icon = "smiley-sad.png"
        description = "Bad"
    } else {
        icon = "smiley-cry.png"
        description = "Terrible"
    }
    $icon.attr('src', 'assets/images/icons/' + icon)
    $icon.attr('title', description)
    return $icon
}
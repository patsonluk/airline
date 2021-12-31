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
                $assetDiv.append($assetImageDiv)

                var $costDiv = $('<h5>$' +  commaSeparateNumber(asset.cost) + '</h5>')
                $assetDiv.append($costDiv)

                $.each(asset.boosts, function(index, boost) {
                    var $boostDiv = $('<div style="display: flex; align-items: center;"></div>')
                    var $boostImage = $('<img src=assets/images/icons/airport-features/' + boost.boostType + '.png>')
                    $boostImage.attr('title', boost.label)
                    $boostDiv.append($boostImage)
                    var boostText = commaSeparateNumber(boost.value)
                    if (boost.value > 0) {
                        boostText = "+" + boostText
                    } else {
                        boostText = "-" + boostText
                    }

                    $boostDiv.append($('<span>' + boostText + '</span>'))
                    $assetDiv.append($boostDiv)
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

function buildOrUpgradeAsset(asset) {
	var url = "airlines/" + activeAirline.id + "/airport-asset/" + asset.id
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
	    		showAirportDetails(asset.airport.id)
	    		showAssetModal(result)
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
                showAirportDetails(asset.airport.id)
                closeModal($("#airportAssetDetailsModal"))
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
    var owned = asset.airline && activeAirline && activeAirline.id == asset.airline.id
    $('#airportAssetDetailsModal .name').text(asset.name)
    $('#airportAssetDetailsModal .assetNameWarningDiv').hide()

    //general info is available already
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
    if (asset.upkeep) {
        $('#airportAssetDetailsModal .assetExpense').text('$' + commaSeparateNumber(asset.upkeep))
    } else {
        $('#airportAssetDetailsModal .assetExpense').text('-')
    }
    $('#airportAssetDetailsModal .assetCost').text('$' + commaSeparateNumber(asset.cost))

    if (asset.income) {
        $('#airportAssetDetailsModal .assetIncome').text('$' + commaSeparateNumber(asset.income))
    } else {
        $('#airportAssetDetailsModal .assetIncome').text('-')
    }
    if (asset.profit) {
        $('#airportAssetDetailsModal .assetProfit').text('$' + commaSeparateNumber(asset.profit))
    } else {
        $('#airportAssetDetailsModal .assetProfit').text('-')
    }

    $('#airportAssetDetailsModal .cost').text('$' + commaSeparateNumber(asset.cost))
    $('#airportAssetDetailsModal .constructionDuration').text(asset.constructionDuration + " weeks")

    $('#airportAssetDetailsModal .assetOwner').empty()
    if (asset.airline) {
        if (owned) { //allow name input
            $('#airportAssetDetailsModal .assetNameInput').val(asset.name)
            $('#airportAssetDetailsModal .assetName').hide()
            $('#airportAssetDetailsModal .assetNameInputDiv').show()
        } else {
            $('#airportAssetDetailsModal .assetName').text(asset.name)
            $('#airportAssetDetailsModal .assetName').hide()
            $('#airportAssetDetailsModal .assetNameInputDiv').show()
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
    }

    //hide all action button until the ajax call finishes
    $('#airportAssetDetailsModal .buildButton').hide()
    $('#airportAssetDetailsModal .upgradeButton').hide()
    $('#airportAssetDetailsModal .sellButton').hide()

    if (activeAirline) {
        var url = "airlines/" + activeAirline.id + "/airport-asset/" + asset.id
        $.ajax({
            type: 'GET',
            url: url,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(assetDetails) {
                if (owned) {
                    $('#airportAssetDetailsModal .upgradeButton').show()
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
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
    }

    $('#airportAssetDetailsModal').fadeIn(200)
}







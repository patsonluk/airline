function showCampaignModal() {
//    if (airlineId) {
//        updateHeatmap(airlineId)
//    }
    updateCampaignTable()
    var $locationSearchInput = $('#campaignModal .searchInput input')

    $locationSearchInput.on('confirmSelection', function(e) {
        draftCampaign($('#campaignModal .searchInput input').data("selectedId"))
    })

    if (!campaignMap) {
     campaignMap = new google.maps.Map($('#campaignModal .campaignMap')[0], {
                                              //gestureHandling: 'none',
                                              disableDefaultUI: true,
                                              scrollwheel: false,
                                              draggable: true,
                                              panControl: true,
                                              styles: getMapStyles()
                                          })
    }

    updateAirlineDelegateStatus($('#campaignModal div.delegateStatus'), function(delegateInfo) {
            $('#campaignModal div.delegateSection').data("availableDelegates", delegateInfo.availableCount)
        })
    $('#campaignModal div.delegateSection').data("delegatesRequired", 0)
    $('#campaignModal .campaignDetails').hide()
    $('#campaignModal .draftCampaign').hide()
    $('#campaignModal').data('closeCallback', updateCampaignSummary)
    $('#campaignModal').fadeIn(500)
}

function toggleDraftCampaign() {
    $('#campaignModal .campaignDetails').hide()
    $('#campaignModal .searchInput .airport').val('')

    $('#campaignModal .draftCampaign').fadeIn(500)

}

function changeCampaignDelegateCount(delta) {
    changeTaskDelegateCount($('#campaignModal .delegateSection'), delta, function(delegateCount) {
        if (delegateCount <= 0) {
            disableButton($('#campaignModal .campaignDetails .save'), "must assign at least one delegate")
        } else {
            enableButton($('#campaignModal .campaignDetails .save'))
        }
        $('#campaignModal .campaignDetails .cost').text('$' + commaSeparateNumber(delegateCount * $('#campaignModal .campaignDetails').data('costPerDelegate')))
    })
}


var loadedCampaigns = []



function updateCampaignTable() {
    $.ajax({
        type: 'GET',
        url: "airlines/" + activeAirline.id + "/campaigns?fullLoad=true",
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            loadedCampaigns = result
            refreshCampaignTable()
            var selectedSortHeader = $('#campaignModal .campaignTableHeader .cell.selected')
		    refreshCampaignTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
        },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
    });

}

function refreshCampaignTable(sortProperty, sortOrder) {
	var $campaignTable = $("#campaignModal .campaignTable")
	$campaignTable.children("div.table-row").remove()

	//sort the list
	//loadedLinks.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	loadedCampaigns = sortPreserveOrder(loadedCampaigns, sortProperty, sortOrder == "ascending")

    var selectedCampaign = $('#campaignModal').data('selectedCampaign')
	$.each(loadedCampaigns, function(index, campaign) {
		var row = $("<div class='table-row clickable' onclick='selectCampaign($(this))'></div>")
		row.data("campaign", campaign)

		row.append("<div class='cell'>" + getCountryFlagImg(campaign.principalAirport.countryCode) + getAirportText(campaign.principalAirport.city, campaign.principalAirport.iata) + "</div>")
		row.append("<div class='cell'>" + campaign.radius + "</div>")
		row.append("<div class='cell'>" + campaign.level + "</div>")
		row.append("<div class='cell'>" + campaign.population + "</div>")
		row.append("<div class='cell'>" + campaign.area.length + "</div>")

		if (selectedCampaign && selectedCampaign.id == campaign.id) {
			row.addClass("selected")
		}

		$campaignTable.append(row)
	});

	if (loadedCampaigns.length == 0) {
	    $campaignTable.append("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div></div>")

	    $('#campaignModal .addCampaign').addClass('glow')
	} else {
	    $('#campaignModal .addCampaign').removeClass('glow')
	}
}
function selectCampaign(row) {
    //update table
	row.siblings().removeClass("selected")
	row.addClass("selected")
	var campaign = row.data('campaign')
	$('#campaignModal').data('selectedCampaign', campaign)
	$('#campaignModal .campaignMap').data('radius', campaign.radius)
    $('#campaignModal .campaignMap').data('selectedAirportId', campaign.principalAirport.id)

    var $delegateSection = $('#campaignModal div.delegateSection')
    var delegates = campaign.delegates
    delegates.sort(function(a, b) { //sort, the most senior comes first
        return a.startCycle - b.startCycle
    })
    $delegateSection.data('originalDelegates', delegates)
    $delegateSection.data('assignedDelegateCount', delegates.length)

    $('#campaignModal .campaignDetails .create').hide()
    $('#campaignModal .campaignDetails .update').show()
    $('#campaignModal .campaignDetails .delete').show()

	refreshCampaign()
}

function draftCampaign(selectedAirportId) {
    $('#campaignModal #campaignTable .table-row.selected').removeClass('selected')
    $('#campaignModal .campaignMap').data('radius', MIN_CAMPAIGN_RADIUS)
    $('#campaignModal .campaignMap').data('selectedAirportId', selectedAirportId)
    $('#campaignModal').removeData('selectedCampaign')

    var $delegateSection = $('#campaignModal div.delegateSection')
    $delegateSection.data('originalDelegates', [])
    $delegateSection.data('assignedDelegateCount', 0)
    disableButton($('#campaignModal .campaignDetails .save'), "must assign at least one delegate")

    $('#campaignModal .campaignDetails .create').show()
    $('#campaignModal .campaignDetails .update').hide()
    $('#campaignModal .campaignDetails .delete').hide()

    refreshCampaign()
}

function createCampaign() {
    var $delegateSection = $('#campaignModal .delegateSection')
    var assignedDelegateCount = $delegateSection.data('assignedDelegateCount')
    $.ajax({
        type: 'POST',
        url: "airlines/" + activeAirline.id + "/campaigns",
        contentType: 'application/json; charset=utf-8',
        data:  JSON.stringify({
         'delegateCount' : assignedDelegateCount,
         'airportId' : $("#campaignModal .campaignMap").data('selectedAirportId'),
         'radius' : $('#campaignModal .campaignMap').data('radius')
         }) ,
        dataType: 'json',
        success: function(result) {
            closeCampaignDetails(true)
            updateCampaignTable()
            updateTopBarDelegates(activeAirline.id)
        },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function updateCampaign() {
    var $delegateSection = $('#campaignModal .delegateSection')
    var assignedDelegateCount = $delegateSection.data('assignedDelegateCount')
    $.ajax({
        type: 'POST',
        url: "airlines/" + activeAirline.id + "/campaigns",
        contentType: 'application/json; charset=utf-8',
        data:  JSON.stringify({
         'delegateCount' : assignedDelegateCount,
         'radius' : $('#campaignModal .campaignMap').data('radius'),
         'campaignId' : $('#campaignModal').data('selectedCampaign').id
         }) ,
        dataType: 'json',
        success: function(result) {
            closeCampaignDetails(true)
            updateCampaignTable()
            updateTopBarDelegates(activeAirline.id)
        },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}



function deleteCampaign() {
    var airport = $('#campaignModal').data('selectedCampaign').principalAirport
    promptConfirm("Do you want to delete this campaign at " + getAirportText(airport.city, airport.iata), function() {
        $.ajax({
            type: 'DELETE',
            url: "airlines/" + activeAirline.id + "/campaigns/" + $('#campaignModal').data('selectedCampaign').id,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(result) {
                closeCampaignDetails(true)
                updateCampaignTable()
                updateTopBarDelegates(activeAirline.id)
            },
            error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
        }
    )
}



var campaignMap
var campaignMapElements = []

function populateCampaignMap(principalAirport, campaignArea, candidateArea, radius) {
    $.each(campaignMapElements, function() { this.setMap(null)})
    campaignMapElements = []

    //+ 200 to include candidate area * 1000 to convert to meters, * 2 to convert to diameter, * 1.2 to make in a slight bigger than what needs to be
    var zoom = getGoogleZoomLevel((radius + 200) * 1000 * 2 * 1.2, $('#campaignModal .campaignMap'), principalAirport.latitude)
    campaignMap.setZoom(zoom)

    campaignMap.setCenter({lat: principalAirport.latitude, lng: principalAirport.longitude}); //this would eventually trigger an idle

    var airportMapCircle = new google.maps.Circle({
                center: {lat: principalAirport.latitude, lng: principalAirport.longitude},
                radius: radius * 1000, //in meter
                strokeColor: "#32CF47",
                strokeOpacity: 0.2,
                strokeWeight: 2,
                fillColor: "#32CF47",
                fillOpacity: 0.3,
                map: campaignMap
            });
    campaignMapElements.push(airportMapCircle)
    populateCampaignAirportMarkers(campaignMap, campaignArea, true)
    populateCampaignAirportMarkers(campaignMap, candidateArea, false)
    google.maps.event.addListenerOnce(campaignMap, 'idle', function() {
        setTimeout(function() { //set a timeout here, otherwise it might not render part of the map...
            campaignMap.setCenter({lat: principalAirport.latitude, lng: principalAirport.longitude}); //this would eventually trigger an idle
            google.maps.event.trigger(campaignMap, 'resize'); //this refreshes the map
            console.log('resize')
        }, 2000);
    });
}

function populateCampaignAirportMarkers(campaignMap, airports, hasCoverage) {
    $.each(airports, function(index, airport) {
        var icon
        if (hasCoverage) {
            icon = getAirportIcon(airport)
        } else {
            icon = $("#map").data("disabledAirportMarker")
        }
        var position = {lat: airport.latitude, lng: airport.longitude};
          var marker = new google.maps.Marker({
                position: position,
                map: campaignMap,
                airport: airport,
                icon : icon
              });

            var infowindow
           	marker.addListener('mouseover', function(event) {
           		$("#campaignAirportPopup .airportName").text(getAirportText(airport.city, airport.iata))
           		$("#campaignAirportPopup .airportPopulation").text(airport.population)
           		infowindow = new google.maps.InfoWindow({
           		       disableAutoPan : true
                 });

                 var popup = $("#campaignAirportPopup").clone()
                 popup.show()
                 infowindow.setContent(popup[0])


           		infowindow.open(campaignMap, marker);
           	})
           	marker.addListener('mouseout', function(event) {
           		infowindow.close()
           		infowindow.setMap(null)
           	})
           	campaignMapElements.push(marker)

     })
}

var MIN_CAMPAIGN_RADIUS = 100
var MAX_CAMPAIGN_RADIUS = 1000
function changeCampaignRadius(delta) {
    currentRadius = $('#campaignModal .campaignMap').data('radius')
    var newRadius = currentRadius + delta
    if (newRadius >= MIN_CAMPAIGN_RADIUS && newRadius <= MAX_CAMPAIGN_RADIUS) {
        $('#campaignModal .campaignMap').data('radius', newRadius)
        refreshCampaign()
    }
}

function updateCampaignRadiusControl(radius) {
    if (radius > MIN_CAMPAIGN_RADIUS) {
        enableButton($("#campaignModal .radiusControl .decrease"))
    }
    if (radius < MAX_CAMPAIGN_RADIUS) {
        enableButton($("#campaignModal .radiusControl .increase"))
    }

    if (radius <= MIN_CAMPAIGN_RADIUS) {
        disableButton($("#campaignModal .radiusControl .decrease"), "Min campaign radius at " + MIN_CAMPAIGN_RADIUS + " km" )
    } else if (radius >= MAX_CAMPAIGN_RADIUS) {
        disableButton($("#campaignModal .radiusControl .increase"), "Max campaign radius at " + MAX_CAMPAIGN_RADIUS + " km" )
    }
}



function refreshCampaign() {
    var radius = $('#campaignModal .campaignMap').data('radius')
    var selectedAirportId = $('#campaignModal .campaignMap').data('selectedAirportId')
    updateCampaignRadiusControl(radius)
    $('#campaignModal span.radius').text(radius)
    $.ajax({
        type: 'GET',
        url: "airlines/" + activeAirline.id + "/campaign-airports/" + selectedAirportId + "?radius=" + radius,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            populateCampaignMap(result.principalAirport, result.area, result.candidateArea, radius)
            //var campaign = $('#campaignModal').data('selectedCampaign')
            updateCampaignDetails(result)
            $('#campaignModal .draftCampaign').hide()
            $('#campaignModal .campaignDetails').fadeIn(500)
        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function updateCampaignDetails(campaign) {
    var $delegateSection = $('#campaignModal div.delegateSection')
    $('#campaignModal .campaignDetails .principalAirport').text(getAirportText(campaign.principalAirport.city, campaign.principalAirport.iata))
    $('#campaignModal .campaignDetails .population').text(commaSeparateNumber(campaign.population))
    $('#campaignModal .campaignDetails .airports').text(campaign.area.length)
    var delegateLevel = 0
    $.each($delegateSection.data('originalDelegates'), function(index, delegate) {
        delegateLevel += delegate.level
    })

    $('#campaignModal .campaignDetails .delegateLevel').text(delegateLevel)
    $('#campaignModal .campaignDetails').data('costPerDelegate', campaign.costPerDelegate)
    $('#campaignModal .campaignDetails .awarenessBonus').text(campaign.bonus.awareness)
    $('#campaignModal .campaignDetails .loyaltyBonus').text(campaign.bonus.loyalty)

    //update delegate section
    var $delegateSection = $('#campaignModal div.delegateSection')
    $('#campaignModal .campaignDetails .cost').text('$' + commaSeparateNumber($delegateSection.data('assignedDelegateCount') * campaign.costPerDelegate))
    refreshAssignedDelegates($delegateSection)


}

function closeCampaignDetails(updated) {
    if (updated) {
        updateAirlineDelegateStatus($('#officeCanvas .delegateStatus'))
    }
    $('#campaignModal .campaignDetails').fadeOut(500)
}

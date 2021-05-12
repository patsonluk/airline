var historyFlightMarkers = []
//var flightMarkerAnimations = []
var historyPaths = {}

function showLinkHistoryView() {
	if (!$('#worldMapCanvas').is(":visible")) {
		showWorldMap()
	}

	loadCurrentAirlineAlliance(function(allianceDetails) {
		currentAirlineAllianceMembers = []
		if (allianceDetails.allianceId) {
			var alliance = loadedAlliancesById[allianceDetails.allianceId]
			if (alliance) {
				$.each(alliance.members, function(index, member) {
					currentAirlineAllianceMembers.push(member.airlineId)
				})
			}
		}
	})
	clearAllPaths() //clear all flight paths

    //populate control panel
	$("#linkHistoryControlPanel .transitAirlineList .table-row").remove()

	$("#linkHistoryControlPanel .routeList").empty()
	$("#linkHistoryControlPanel").data("showForward", true)
	var link = loadedLinksById[selectedLink]
	var forwardLinkDescription = "<div style='display: flex; align-items: center;' class='clickable selected' onclick='toggleLinkHistoryDirection(true, $(this))'>" + getAirportText(link.fromAirportCity, link.fromAirportCode) + "<img src='assets/images/icons/arrow.png'>" + getAirportText(link.toAirportCity, link.toAirportCode) + "</div>"
    var backwardLinkDescription = "<div style='display: flex; align-items: center;' class='clickable' onclick='toggleLinkHistoryDirection(false, $(this))'>" + getAirportText(link.toAirportCity, link.toAirportCode) + "<img src='assets/images/icons/arrow.png'>" + getAirportText(link.fromAirportCity, link.fromAirportCode) + "</div>"

    $("#linkHistoryControlPanel .routeList").append(forwardLinkDescription)
    $("#linkHistoryControlPanel .routeList").append(backwardLinkDescription)

    $("#linkHistoryControlPanel").show()

    $('#linkHistoryControlPanel').data('cycleDelta', 0)
	loadLinkHistory(selectedLink)
}

function loadLinkHistory(linkId) {
    $.each(historyPaths, function(index, path) { //clear all history path
        path.setMap(null)
        path.shadowPath.setMap(null)
    })
    historyPaths = {}
	var linkInfo = loadedLinksById[linkId]
    var airlineNamesById = {}
    var cycleDelta = $('#linkHistoryControlPanel').data('cycleDelta')
    $("#linkHistoryControlPanel .transitAirlineList").empty()
    $.ajax({
        type: 'GET',
        url: "airlines/" + activeAirline.id + "/related-link-consumption/" + linkId + "?cycleDelta=" + cycleDelta,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(linkHistory) {
            var forwardTransitPaxByAirlineId = {}
            var backwardTransitPaxByAirlineId = {}

            if (!jQuery.isEmptyObject(linkHistory)) {
                $.each(linkHistory.relatedLinks, function(step, relatedLinksOnStep) {
                    $.each(relatedLinksOnStep, function(key, relatedLink) {
                        drawLinkHistoryPath(relatedLink, false, linkId, step)
                        if (linkInfo.fromAirportId != relatedLink.fromAirportId || linkInfo.toAirportId != relatedLink.toAirportId || linkInfo.airlineId != linkInfo.airlineId) { //transit should not count the selected link
                            airlineNamesById[relatedLink.airlineId] = relatedLink.airlineName
                            if (!forwardTransitPaxByAirlineId[relatedLink.airlineId]) {
                                forwardTransitPaxByAirlineId[relatedLink.airlineId] = relatedLink.passenger
                            } else {
                                forwardTransitPaxByAirlineId[relatedLink.airlineId] = forwardTransitPaxByAirlineId[relatedLink.airlineId] + relatedLink.passenger
                            }
                        }
                    })
                })
                $.each(linkHistory.invertedRelatedLinks, function(step, relatedLinksOnStep) {
                    $.each(relatedLinksOnStep, function(key, relatedLink) {
                        drawLinkHistoryPath(relatedLink, true, linkId, step)
                        if (linkInfo.fromAirportId != relatedLink.fromAirportId || linkInfo.toAirportId != relatedLink.toAirportId || linkInfo.airlineId != linkInfo.airlineId) { //transit should not count the selected link
                            airlineNamesById[relatedLink.airlineId] = relatedLink.airlineName
                            if (!backwardTransitPaxByAirlineId[relatedLink.airlineId]) {
                                backwardTransitPaxByAirlineId[relatedLink.airlineId] = relatedLink.passenger
                            } else {
                                backwardTransitPaxByAirlineId[relatedLink.airlineId] = backwardTransitPaxByAirlineId[relatedLink.airlineId] + relatedLink.passenger
                            }
                        }
                    })
                })
                var forwardItems = Object.keys(forwardTransitPaxByAirlineId).map(function(key) {
                  return [key, forwardTransitPaxByAirlineId[key]];
                });
                var backwardItems = Object.keys(backwardTransitPaxByAirlineId).map(function(key) {
                  return [key, backwardTransitPaxByAirlineId[key]];
                });
                //now sort them
                forwardItems.sort(function(a, b) {
                    return b[1] - a[1]
                })
                backwardItems.sort(function(a, b) {
                    return b[1] - a[1]
                })
                //populate the top 5 transit airline table
                forwardItems = $(forwardItems).slice(0, 5)
                backwardItems = $(backwardItems).slice(0, 5)
                $.each(forwardItems, function(index, entry) { //entry : airlineId, pax counts
                    var tableRow = $("<div class='table-row' style='display: none;'></div>")
                    tableRow.addClass("forward")
                    var airlineId = entry[0]
                    tableRow.append("<div class='cell' style='width: 70%'>" + getAirlineLogoImg(airlineId) + airlineNamesById[airlineId] + "</div>")
                    tableRow.append("<div class='cell' style='width: 30%'>" + entry[1] + "</div>")

                    $("#linkHistoryControlPanel .transitAirlineList").append(tableRow)
                })
                $.each(backwardItems, function(index, entry) { //entry : airlineId, pax counts
                    var tableRow = $("<div class='table-row' style='display: none;'></div>")
                    tableRow.addClass("backward")
                    var airlineId = entry[0]
                    tableRow.append("<div class='cell' style='width: 70%'>" + getAirlineLogoImg(airlineId) + airlineNamesById[airlineId] + "</div>")
                    tableRow.append("<div class='cell' style='width: 30%'>" + entry[1] + "</div>")

                    $("#linkHistoryControlPanel .transitAirlineList").append(tableRow)
                })

            }
            showLinkHistory()
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


function toggleLinkHistoryDirection(showForward, routeDiv) {
    routeDiv.siblings().removeClass("selected")
    routeDiv.addClass("selected")

    $("#linkHistoryControlPanel").data("showForward", showForward)
    showLinkHistory()
}

function hideLinkHistoryView() {
	$.each(historyPaths, function(index, path) {
	    path.setMap(null)
	})
    historyPaths = {}

	clearHistoryFlightMarkers()
	updateLinksInfo() //redraw all flight paths

	$("#linkHistoryControlPanel").hide()
}

function drawLinkHistoryPath(link, inverted, watchedLinkId, step) {
	var from = new google.maps.LatLng({lat: link.fromLatitude, lng: link.fromLongitude})
	var to = new google.maps.LatLng({lat: link.toLatitude, lng: link.toLongitude})
	var pathKey = link.fromAirportId + "|"  + link.toAirportId + "|" + inverted

	var lineSymbol = {
	    path: google.maps.SymbolPath.FORWARD_OPEN_ARROW
	};

	var isWatchedLink = link.linkId == watchedLinkId

	var relatedPath
	if (!historyPaths[pathKey]) {
		relatedPath = new google.maps.Polyline({
			 geodesic: true,
		     strokeColor: "#DC83FC",
		     strokeOpacity: 0.8,
		     strokeWeight: 2,
		     path: [from, to],
		     icons: [{
			      icon: lineSymbol,
			      offset: '50%'
			    }],
		     zIndex : 1100,
		     inverted : inverted,
		     watched : isWatchedLink,
		     step : step
		});

		shadowPath = new google.maps.Polyline({
			 geodesic: true,
		     strokeOpacity: 0.0001,
		     strokeWeight: 25,
		     path: [from, to],
		     zIndex : 401,
		     inverted : inverted,
		     link : link,
		     thisAirlinePassengers : 0,
		     thisAlliancePassengers : 0,
		     otherAirlinePassengers : 0
		});

		relatedPath.shadowPath = shadowPath

		historyPaths[pathKey] = relatedPath
	} else {
		relatedPath = historyPaths[pathKey]
	}

	if (link.airlineId == activeAirline.id) {
		relatedPath.shadowPath.thisAirlinePassengers += link.passenger
	} else if (currentAirlineAllianceMembers.length > 0 && $.inArray(link.airlineId, currentAirlineAllianceMembers) != -1) {
		relatedPath.shadowPath.thisAlliancePassengers += link.passenger
	} else {
		relatedPath.shadowPath.otherAirlinePassengers += link.passenger
	}
}

function clearHistoryFlightMarkers() {
    $.each(historyFlightMarkers, function(index, markersOnAStep) {
        $.each(markersOnAStep, function(index, marker) {
        //window.clearInterval(marker.animation)
    	    marker.setMap(null)
        })
    })
    historyFlightMarkers = []

    if (historyFlightMarkerAnimation) {
        window.clearInterval(historyFlightMarkerAnimation)
        historyFlightMarkerAnimation = null
    }
}
var historyFlightMarkerAnimation

function animateHistoryFlightMarkers(framesPerAnimation) {
    var currentStep = 0
    var currentFrame = 0
    var animationInterval = 50
    historyFlightMarkerAnimation = window.setInterval(function() {
        $.each(historyFlightMarkers[currentStep], function(index, marker) {
            if (!marker.isActive) {
                marker.isActive = true
                marker.elapsedDuration = 0
                marker.setPosition(marker.from)
                marker.setMap(map)
            } else  {
                marker.elapsedDuration += 1

                if (marker.elapsedDuration == marker.totalDuration) { //arrived
                    marker.isActive = false
                    //console.log("next departure " + marker.nextDepartureFrame)
                } else {
                    var newPosition = google.maps.geometry.spherical.interpolate(marker.from, marker.to, marker.elapsedDuration / marker.totalDuration)
                    marker.setPosition(newPosition)
                }
            }
  		})
  		if (currentFrame == framesPerAnimation) {
      	   fadeOutMarkers(historyFlightMarkers[currentStep], animationInterval)
  		   currentStep = (++ currentStep) % historyFlightMarkers.length
           currentFrame = 0
        } else {
           currentFrame ++
        }
    }, animationInterval)

}

function fadeOutMarkers(markers, animationInterval) {
    var opacity = 1.0
    var animation = window.setInterval(function () {
        if (opacity <= 0) {
            $.each(markers, function(index, marker) {
                marker.setMap(null)
                marker.setOpacity(1)
            })
            window.clearInterval(animation)
        } else {
            $.each(markers, function(index, marker) {
                marker.setOpacity(opacity)
            })
            opacity -= 0.1
        }
    }, animationInterval)
}


function drawHistoryFlightMarker(line, framesPerAnimation, totalPassengers) {
	if (currentAnimationStatus) {
		var from = line.getPath().getAt(0)
		var to = line.getPath().getAt(1)
		var icon
        if (totalPassengers > 200) {
	       icon = "dot-5.png"
        } else if (totalPassengers > 100) {
           icon = "dot-4.png"
        } else if (totalPassengers > 50) {
           icon = "dot-3.png"
        } else if (totalPassengers > 25) {
           icon = "dot-2.png"
        } else {
           icon = "dot-1.png"
        }

		var image = {
	        url: "assets/images/markers/" + icon,
	        origin: new google.maps.Point(0, 0),
	        anchor: new google.maps.Point(6, 6),
	    };

        var marker = new google.maps.Marker({
            position: from,
            from : from,
            to : to,
            icon : image,
            elapsedDuration : 0,
            totalDuration : framesPerAnimation,
            isActive: false,
            clickable: false
        });

        //flightMarkers.push(marker)
        var step = line.step
        var historyFlightMarkersOfThisStep = historyFlightMarkers[step]
        if (!historyFlightMarkersOfThisStep) {
            historyFlightMarkersOfThisStep = []
            historyFlightMarkers[step] = historyFlightMarkersOfThisStep
        }
        historyFlightMarkersOfThisStep.push(marker)
	}
}




function showLinkHistory() {
    var showAlliance = $("#linkHistoryControlPanel .showAlliance").is(":checked")
    var showOther = $("#linkHistoryControlPanel .showOther").is(":checked")
    var showForward = $("#linkHistoryControlPanel").data("showForward")
    var showAnimation = $("#linkHistoryControlPanel .showAnimation").is(":checked")

    var cycleDelta = $("#linkHistoryControlPanel").data('cycleDelta')
    $("#linkHistoryControlPanel .cycleDeltaText").text(cycleDelta * -1 + 1)
    var disablePrev = false
    var disableNext= false
    if (cycleDelta <= -29) {
        disablePrev = true
    } else if (cycleDelta >= 0) {
        disableNext = true
    }

    $("#linkHistoryControlPanel img.prev").prop("onclick", null).off("click");
    if (disablePrev) {
        $('#linkHistoryControlPanel img.prev').attr("src", "assets/images/icons/arrow-180-grey.png")
        $('#linkHistoryControlPanel img.prev').removeClass('clickable')
    } else {
        $('#linkHistoryControlPanel img.prev').attr("src", "assets/images/icons/arrow-180.png")
        $('#linkHistoryControlPanel img.prev').addClass('clickable')
        $("#linkHistoryControlPanel img.prev").click(function() {
            $("#linkHistoryControlPanel").data('cycleDelta', $("#linkHistoryControlPanel").data('cycleDelta') - 1)
            loadLinkHistory(selectedLink)
        })
    }

    $("#linkHistoryControlPanel img.next").prop("onclick", null).off("click");
    if (disableNext) {
        $('#linkHistoryControlPanel img.next').attr("src", "assets/images/icons/arrow-grey.png")
        $('#linkHistoryControlPanel img.next').removeClass('clickable')
        $("#linkHistoryControlPanel img.next").prop("onclick", null).off("click");
    } else {
        $('#linkHistoryControlPanel img.next').attr("src", "assets/images/icons/arrow.png")
        $('#linkHistoryControlPanel img.next').addClass('clickable')
        $("#linkHistoryControlPanel img.next").click(function() {
            $("#linkHistoryControlPanel").data('cycleDelta', $("#linkHistoryControlPanel").data('cycleDelta') + 1)
            loadLinkHistory(selectedLink)
        })
    }

    $("#linkHistoryControlPanel .transitAirlineList .table-row").hide()
    if (showForward) {
        $("#linkHistoryControlPanel .transitAirlineList .table-row.forward").show()
    } else {
        $("#linkHistoryControlPanel .transitAirlineList .table-row.backward").show()
    }

    var framesPerAnimation = 50
    clearHistoryFlightMarkers()
    $.each(historyPaths, function(key, historyPath) {
        if (((showForward && !historyPath.inverted) || (!showForward && historyPath.inverted))  //match direction
        && (historyPath.shadowPath.thisAirlinePassengers > 0
         || (showAlliance && historyPath.shadowPath.thisAlliancePassengers > 0)
         || (showOther && historyPath.shadowPath.otherAirlinePassengers))) {
            var totalPassengers = historyPath.shadowPath.thisAirlinePassengers + historyPath.shadowPath.thisAlliancePassengers + historyPath.shadowPath.otherAirlinePassengers
            if (totalPassengers < 100) {
                var newOpacity = 0.2 + totalPassengers / 100 * (historyPath.strokeOpacity - 0.2)
                if (!historyPath.watched) {
                    historyPath.setOptions({strokeOpacity : newOpacity})
                }
            }
            var infowindow;
            historyPath.shadowPath.addListener('mouseover', function(event) {
                var link = this.link

                $("#linkHistoryPopupFrom").html(getCountryFlagImg(link.fromCountryCode) + getAirportText(link.fromAirportCity, link.fromAirportCode))
                $("#linkHistoryPopupTo").html(getCountryFlagImg(link.toCountryCode) + getAirportText(link.toAirportCity, link.toAirportCode))
                $("#linkHistoryThisAirlinePassengers").text(this.thisAirlinePassengers)
                if (showAlliance) {
                    $("#linkHistoryThisAlliancePassengers").text(this.thisAlliancePassengers)
                    $("#linkHistoryThisAlliancePassengers").closest(".table-row").show()
                } else {
                    $("#linkHistoryThisAlliancePassengers").closest(".table-row").hide()
                }
                if (showOther) {
                    $("#linkHistoryOtherAirlinePassengers").text(this.otherAirlinePassengers)
                    $("#linkHistoryOtherAirlinePassengers").closest(".table-row").show()
                 } else {
                    $("#linkHistoryOtherAirlinePassengers").closest(".table-row").hide()
                 }

                infowindow = new google.maps.InfoWindow({
                     maxWidth : 400});

                var popup = $("#linkHistoryPopup").clone()
    			popup.show()
                infowindow.setContent(popup[0])

                infowindow.setPosition(event.latLng);
                infowindow.open(map);
            })
            historyPath.shadowPath.addListener('mouseout', function(event) {
                infowindow.close()
            })


            if (historyPath.shadowPath.thisAirlinePassengers > 0) {
                historyPath.setOptions({strokeColor: "#DC83FC"})
            } else if (showAlliance && historyPath.shadowPath.thisAlliancePassengers > 0) {
                historyPath.setOptions({strokeColor: "#E28413"})
            } else {
                historyPath.setOptions({strokeColor: "#888888"})
            }


            if (historyPath.watched) {
                highlightPath(historyPath)
            }

            if (showAnimation) {
                drawHistoryFlightMarker(historyPath, framesPerAnimation, totalPassengers)
            }

            historyPath.setMap(map)
            historyPath.shadowPath.setMap(map)
            polylines.push(historyPath)
            polylines.push(historyPath.shadowPath)
         } else {
            historyPath.setMap(null)
            historyPath.shadowPath.setMap(null)
         }
    })
    if (showAnimation) {
        animateHistoryFlightMarkers(framesPerAnimation)
    }

}


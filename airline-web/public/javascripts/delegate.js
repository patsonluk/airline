function changeTaskDelegateCount($delegateSection, delta, callback) {
    var assignedDelegateCount = $delegateSection.data('assignedDelegateCount')
    var availableDelegates = $delegateSection.data('availableDelegates')
    var originalDelegates = $delegateSection.data('originalDelegates')
    var delegatesRequired = $delegateSection.data('delegatesRequired')

    var newLength = -1
    if (delta > 0) {
        if (availableDelegates >= delta) {
            newLength = assignedDelegateCount + delta
        }
    } else if (delta < 0) {
        if (assignedDelegateCount + delta >= delegatesRequired) {
            newLength = assignedDelegateCount + delta
        }
    }

    if (newLength != -1) {
        $delegateSection.data('availableDelegates', availableDelegates - delta)
        $delegateSection.data('assignedDelegateCount', newLength)
        refreshAssignedDelegates($delegateSection)
        if (callback) {
            callback(newLength)
        }
    }
}

function refreshAssignedDelegates($delegateSection) {
    $delegateIcons = $delegateSection.find('div.assignedDelegatesIcons')
    $delegateIcons.empty()
    //var $delegateSection = $('#airlineCountryRelationshipModal .delegateSection')
    var originalDelegates = $delegateSection.data('originalDelegates')
    var assignedDelegateCount = $delegateSection.data('assignedDelegateCount')

    if (assignedDelegateCount == 0) {
       $delegateIcons.append("<span>None</span>")
    }

    $.each(originalDelegates.slice(0, assignedDelegateCount), function(index, assignedDelegate) {
            var delegateIcon = $('<img src="assets/images/icons/delegate-level-' + assignedDelegate.level + '.png" title="' + assignedDelegate.levelDescription + "&nbsp;(level " + assignedDelegate.level + (assignedDelegate.nextLevelCycleCount ? " - promotion in " + assignedDelegate.nextLevelCycleCount + " week(s)" : "") + ')"/>')
            $delegateIcons.append(delegateIcon)
    })

    if (assignedDelegateCount > originalDelegates.length) {
        for (i = 0; i < assignedDelegateCount - originalDelegates.length; i ++) {
            var delegateIcon = $('<img src="assets/images/icons/delegate-level-0.png" title="New"/>')
            $delegateIcons.append(delegateIcon)
        }
    }
}


function refreshAirlineDelegateStatus($delegateStatusDiv, delegateInfo) {
    $delegateStatusDiv.empty()
    var availableDelegates = delegateInfo.availableCount

    var delegateIcons = []
    //delegate info
    for (i = 0 ; i < availableDelegates; i ++) {
        var $delegateIconDiv = $('<div style="position: relative; display: inline-block;"><img src="assets/images/icons/user-silhouette-available.png" title="Available Delegate"/></div>')
        $delegateIconDiv.expirable = true;
        delegateIcons.push($delegateIconDiv)
    }

    $.each(delegateInfo.busyDelegates, function(index, busyDelegate) {
        var $delegateIconDiv = $('<div style="position: relative; display: inline-block;"></div>')
        var $delegateIcon
        if (busyDelegate.completed) {
            $delegateIcon = $('<img src="assets/images/icons/user-silhouette-unavailable.png" title="' + busyDelegate.coolDown + ' week(s) cool down remaining. Previous task : ' + busyDelegate.taskDescription + '"/>')
            $delegateIconDiv.expirable = true;
        } else {
            $delegateIcon = $('<img src="assets/images/icons/user-silhouette-busy.png" title="Busy with task - ' + busyDelegate.taskDescription + '"/>')
        }

        $delegateIconDiv.append($delegateIcon)

        if (busyDelegate.coolDown) {
            var $coolDownDiv = $("<div style='position: absolute; left: 1px; bottom: 0; background-color: #a4f5b0; color: #454544; font-size: 8px; font-weight: bold;'></div>")
            $coolDownDiv.text(busyDelegate.coolDown)
            $delegateIconDiv.append($coolDownDiv)
        }
        delegateIcons.push($delegateIconDiv)
    })


    var iconIndex = delegateIcons.length //mark the available ones first
    $.each(delegateInfo.boosts, function(index, boost) {
        for (i = 0 ; i < boost.amount; i ++) {
            while (--iconIndex > 0 && !delegateIcons[iconIndex].expirable) {
                //find the first icon (traverse from the back) that can be marked as expirable
            }
            var $boostRemainingDiv = $("<div style='position: absolute; left: 1px; top: 0; background-color: #a4f5b0; color: #454544; font-size: 8px; font-weight: bold;' title='Boost expiring in " + boost.remainingCycles + " week(s)'>" + boost.remainingCycles + "</div>")
            delegateIcons[iconIndex].append($boostRemainingDiv)
        }
    })

    $.each(delegateIcons, function(index, $icon) {
        $delegateStatusDiv.append($icon)
    })
}


function updateAirlineDelegateStatus($delegateStatusDiv, successFunction) {
    $delegateStatusDiv.empty()
    var airlineId = activeAirline.id

	$.ajax({
		type: 'GET',
		url: "delegates/airline/" + activeAirline.id,
		contentType: 'application/json; charset=utf-8',
		dataType: 'json',
	    success: function(delegateInfo) {
	        refreshAirlineDelegateStatus($delegateStatusDiv, delegateInfo)

            if (successFunction) {
                successFunction(delegateInfo)
            }
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateTopBarDelegates(airlineId) {
    $.ajax({
    		type: 'GET',
    		url: "airlines/" + airlineId,
    	    contentType: 'application/json; charset=utf-8',
    	    dataType: 'json',
    	    success: function(airline) {
    	    	refreshTopBarDelegates(airline)
    	    },
    	    error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    	});
}

function refreshTopBarDelegates(airline) {
    $('#topBar .delegatesShortcut').empty()
	var availableDelegates = airline.delegatesInfo.availableCount
	var busyDelegates = airline.delegatesInfo.busyDelegates.length
	var $delegateIconDiv = $('<div style="position: relative; display: inline-block;"></div>').appendTo($('#topBar .delegatesShortcut'))
    var $delegateIcon = $('<img>').appendTo($delegateIconDiv)

    if (availableDelegates <= 0) {
        $delegateIcon.attr('src', 'assets/images/icons/user-silhouette-unavailable.png')
        var minCoolDown = -1
        $.each(airline.delegatesInfo.busyDelegates, function(index, busyDelegate) {
            if (busyDelegate.completed) {
                if (minCoolDown == -1 || busyDelegate.coolDown < minCoolDown) {
                    minCoolDown = busyDelegate.coolDown
                }
            }
        })
        if (minCoolDown != -1) {
            var $coolDownDiv = $("<div style='position: absolute; left: 1px; bottom: 0; background-color: #FFC273; color: #454544; font-size: 8px; font-weight: bold;'></div>")
            $coolDownDiv.text(minCoolDown)
            $delegateIconDiv.append($coolDownDiv)
        }
        $delegateIconDiv.attr('title', "Next delegate available in " + minCoolDown + " weeks. Delegates (available/total) : 0/" + busyDelegates)
    } else {
        $delegateIcon.attr('src', 'assets/images/icons/user-silhouette-available.png')
        var $availableCountDiv = $("<div style='position: absolute; left: 1px; bottom: 0; background-color: #a4f5b0; color: #454544; font-size: 8px; font-weight: bold;'></div>")
        $availableCountDiv.text(availableDelegates)
        $delegateIconDiv.append($availableCountDiv)
        $delegateIconDiv.attr('title', "Delegates (available/total) : " + availableDelegates + "/" + (availableDelegates + busyDelegates))
    }



}

function toggleDelegateStatusModal() {
    if (!$("#delegateStatusModal").is(":visible")) {
        updateAirlineDelegateStatus($('#delegateStatusModal .delegateStatus'))
        $('#delegateStatusModal').fadeIn(500)
    } else {
        closeModal($('#delegateStatusModal'))
    }
}
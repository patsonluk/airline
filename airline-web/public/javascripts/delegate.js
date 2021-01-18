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

    //delegate info
    for (i = 0 ; i < availableDelegates; i ++) {
        var delegateIcon = $('<img src="assets/images/icons/user-silhouette-available.png" title="Available Delegate"/>')
        $delegateStatusDiv.append(delegateIcon)
    }

    $.each(delegateInfo.busyDelegates, function(index, busyDelegate) {
        var $delegateIconDiv = $('<div style="position: relative; display: inline-block;"></div>')
        var $delegateIcon
        if (busyDelegate.completed) {
            $delegateIcon = $('<img src="assets/images/icons/user-silhouette-unavailable.png" title="' + busyDelegate.coolDown + ' week(s) cool down remaining. Previous task : ' + busyDelegate.taskDescription + '"/>')
        } else {
            $delegateIcon = $('<img src="assets/images/icons/user-silhouette-busy.png" title="Busy with task - ' + busyDelegate.taskDescription + '"/>')
        }

        $delegateIconDiv.append($delegateIcon)

        if (busyDelegate.coolDown) {
            var $coolDownDiv = $("<div style='position: absolute; left: 1px; bottom: 0; background-color: #a4f5b0; color: #454544; font-size: 8px; font-weight: bold;'></div>")
            $coolDownDiv.text(busyDelegate.coolDown)
            $delegateIconDiv.append($coolDownDiv)
        }
        $delegateStatusDiv.append($delegateIconDiv)
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
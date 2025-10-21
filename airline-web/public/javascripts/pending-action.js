function handlePendingActions(pendingActions) {
    $('.left-tab .tab-icon .pendingAction').remove()

    //for now only display the first one

    let eventMarked = false
    $.each(pendingActions, function(index, pendingAction) {
        const category = pendingAction.category
        if (category === 'OLYMPICS_VOTE' || category === 'OLYMPICS_PAX_REWARD' || category === 'OLYMPICS_VOTE_REWARD') {
            if (!eventMarked) { //only process the first one for now (highest precedence)
                const $pendingActionDiv = $('<div style="position: absolute; right: -5px; bottom: -5px; height: 20px; width: 20px;" class="pendingAction"></div>').appendTo($('.left-tab .tab-icon[data-link="event"]'))
                let $icon
                if (category === 'OLYMPICS_VOTE') {
                    $icon = $('<img src="assets/images/icons/exclamation.png">')
                    $icon.attr('title', "Olympics Voting Active")
                } else if (category === 'OLYMPICS_PAX_REWARD') {
                    $icon = $('<img src="assets/images/icons/present.png">')
                    $icon.attr('title', "Unclaimed Olympics passenger reward (" + pendingAction.params['duration'] + ' week(s) left)' )
                } else if (category === 'OLYMPICS_VOTE_REWARD') {
                    $icon = $('<img src="assets/images/icons/present.png">')
                    $icon.attr('title', "Unclaimed Olympics vote reward (" + pendingAction.params['duration'] + ' week(s) left)' )
                }
                $pendingActionDiv.append($icon)
                eventMarked = true
            }
        } else if (category === 'ALLIANCE_PENDING_APPLICATION') {
            var $pendingActionDiv = $('<div style="position: absolute; right: -5px; bottom: -5px; height: 20px; width: 20px;" class="pendingAction"></div>').appendTo($('.left-tab .tab-icon[data-link="alliance"]'))
            var $icon = $('<img src="assets/images/icons/exclamation.png">')
            $icon.attr('title', "Pending Application")
            $pendingActionDiv.append($icon)
        }
    })
}

function checkPendingActions() {
    if (activeAirline) {
        $.ajax({
            type: 'GET',
            url: "broadcaster-direct/trigger-prompts-check/" + activeAirline.id,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(dummy) {
                // this simply triggers it directly on the Broadcaster, which will push the update via ws channel
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
    }
}


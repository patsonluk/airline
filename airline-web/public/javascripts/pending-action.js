function handlePendingActions(pendingActions) {
    $('.left-tab .tab-icon').remove('.pendingAction')
    $.each(pendingActions, function(index, pendingAction) {
        if (pendingAction === 'OLYMPICS_VOTE') {
            var $pendingActionDiv = $('<div style="position: absolute; right: -5px; bottom: -5px; height: 20px; width: 20px;" class="pendingAction"></div>').appendTo($('.left-tab .tab-icon[data-link="event"]'))
            var $icon = $('<img src="assets/images/icons/exclamation.png">')
            $icon.attr('title', "Olympics Voting Active")
            $pendingActionDiv.append($icon)
        }
    })

}


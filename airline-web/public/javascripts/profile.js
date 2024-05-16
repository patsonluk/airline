function updateProfiles(profiles) {
    $('#profiles').find('.profile-section').remove()
    $.each(profiles, function(index, profile) {
        $('#profiles').append(createProfileDiv(profile, index))
    })
    $('#profilesModal').fadeIn(500)
}

function createProfileDiv(profile, profileId) {
	var $profileDiv = $('<div" class="profile-section" onclick="selectProfile(' + profileId + ', this)"><h2>' + profile.name +'</h2><p class="pb-2">' + profile.description + '</p></div>')
    var $list = $('<ul></ul>').appendTo($profileDiv)
    $list.append('<li class="dot">$' + commaSeparateNumber(profile.cash) + '&nbsp;cash</li>')
    if (profile.airplanes.length > 0) {
        var $airplaneLi = $('<li class="dot"></li>')
        $airplaneLi.appendTo($list).append('<span>' + profile.airplanes.length + ' X&nbsp;</span>')
        var $airplaneSpan = $('<span>' + profile.airplanes[0].name + '</span>')
        //$airplaneSpan.css('text-decoration-style', 'dashed')
        $airplaneSpan.css('text-decoration', 'underline dashed')

        $airplaneSpan.bind('click', function() {
            showAirplaneQuickSummary($(this), profile.airplanes[0])
        })
        $airplaneSpan.mouseover(function() {
            showAirplaneQuickSummary($(this), profile.airplanes[0])
        }).mouseout(function() {
            $('#airplaneSummaryTooltip').hide()
        })


        $airplaneLi.append($airplaneSpan)
    }
    $('<li class="dot"></li>').appendTo($list).text(profile.reputation + " reputation")
    $('<li class="dot"></li>').appendTo($list).text(profile.quality + " employee quality")
    if (profile.loan) {
        $('<li class="dot"></li>').appendTo($list).text("Outstanding loan of $" + commaSeparateNumber(profile.loan.remainingAmount) + " weekly payment of $" + commaSeparateNumber(profile.loan.weeklyPayment))
    }
    $profileDiv.append($list)

	if ($('#profileId').val() == profileId) {
		selectProfile(profileId, $profileDiv)
	}

	return $profileDiv
}

function selectProfile(profileId, profileDiv) {
	$('#profileId').val(profileId)
	$(profileDiv).siblings("div").removeClass("selected")
	$(profileDiv).addClass("selected")
}

function showAirplaneQuickSummary($trigger, airplane) {
    var yPos = $trigger.offset().top - $(window).scrollTop() + $trigger.height()
    var xPos = $trigger.offset().left - $(window).scrollLeft() + $trigger.width() - $('#airplaneSummaryTooltip').width() / 2

    $('#airplaneSummaryTooltip .capacity').text(airplane.capacity)
    $('#airplaneSummaryTooltip .range').text(airplane.range)
    $('#airplaneSummaryTooltip .airplaneValue').text(commaSeparateNumber(airplane.value))
    $('#airplaneSummaryTooltip .condition').text(airplane.condition)
    $('#airplaneSummaryTooltip .lifespan').text(airplane.lifespan / 52)

    $('#airplaneSummaryTooltip').css('top', yPos + 'px')
    $('#airplaneSummaryTooltip').css('left', xPos + 'px')
    $('#airplaneSummaryTooltip').show()

    $('#airplaneSummaryTooltip').off('click.close').on('click.close', function() {
        $(this).hide()
    })
}

function buildHqWithProfile() {
    $.ajax({
            type: 'PUT',
            url: "airlines/" + activeAirline.id + "/profiles/" + $('#profileId').val() + "?airportId=" + activeAirportId,
            data: { } ,
    		contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(result) {
                closeModal($('#profilesModal'))
                updateAllPanels(activeAirline.id)
                $('#planLinkFromAirportId').val(activeAirline.headquarterAirport.airportId)
                loadAllCountries() //has a home country now, reload country info
                showWorldMap()
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
}
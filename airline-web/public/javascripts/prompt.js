var promptQueue
var tutorialQueues
var promptInterval
var activePrompt
var activeTutorial
var tutorialsCompleted

function initPrompts() {
    promptQueue = []
    if (promptInterval) {
        clearInterval(promptInterval)
        promptInterval = undefined
    }
    activePrompt = undefined
//    if (activeAirline.headquarterAirport && !activeAirline.initialized) {
//      $.ajax({
//            type: 'GET',
//            url: "airlines/" + activeAirline.id + "/profiles",
//            data: { } ,
//            contentType: 'application/json; charset=utf-8',
//            dataType: 'json',
//            success: function(profiles) {
//                $.each(profiles, function(index, profile) {
//                       //populate profile
//                })
//                queuePrompt('chooseProfile')
//            },
//            error: function(jqXHR, textStatus, errorThrown) {
//                    console.log(JSON.stringify(jqXHR));
//                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
//            }
//        });
//
//    }

    $('.left-tab').bind('click.tutorial', function() {
        var pageId = $(this).data('link')
        checkTutorial(pageId)
    })
    tutorialsCompleted = new Set()
    tutorialQueue = []
    $.ajax({
            type: 'GET',
            url: "airlines/" + activeAirline.id + "/completed-tutorial",
            data: { } ,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(completedTutorials) {
                $.each(completedTutorials, function(index, entry) {
                       tutorialsCompleted.add(entry)
                })
                checkTutorial('worldMap')
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });

    $('#broadcastMessagePopup').data('function', function(message) {
        $('#broadcastMessagePopup .sendMessage').text(message)
        $('#broadcastMessagePopup').fadeIn(500)
    })

    $('#airlineMessagePopup').data('function', function(message) {
        $('#airlineMessagePopup .sendMessage').text(message)
        $('#airlineMessagePopup').fadeIn(500)
    })
}

function queuePrompt(promptId, args) {
    promptQueue.push({ id : promptId, args : args })
    if (!promptInterval) {
        promptInterval = setInterval('showPrompt()', 100)
    }
}

function showPrompt() {
    if (!$('#annoucementModal').is(':visible')) {
        if (!activePrompt) {
            if (promptQueue.length > 0) {
                activePrompt = promptQueue.shift()
                var promptId = '#' + activePrompt.id
                if ($(promptId).data("function")) {
                    $(promptId).data("function")(activePrompt.args)
                } else {
                    $(promptId).fadeIn(500)
                }
                $(promptId).data('closeCallback', function() {
                    closePrompt($(promptId))
                })
            } else if (!activeTutorial && tutorialQueue.length > 0) {
                activeTutorial = tutorialQueue.shift()
                var tutorialId = '#' + activeTutorial
                $(tutorialId).fadeIn(500)
                $(tutorialId).data('closeCallback', function() {
                    closeTutorial($(tutorialId))
                })
            }
        }
    }


    if (promptQueue.length == 0 && tutorialQueue.length == 0) {
        clearInterval(promptInterval)
        promptInterval = undefined
    }
}


function closePrompt($promptModal) {
    activePrompt = undefined
//    var callback = $promptModal.data('callback')
//    if (callback) {
//        callback()
//    }
}


function queueTutorial(tutorial) {
    if (!tutorialsCompleted.has(tutorial) && !tutorialQueue.includes(tutorial)) {
        tutorialQueue.push(tutorial)
    }
    if (!promptInterval) {
        promptInterval = setInterval('showPrompt()', 100)
    }

}

function checkTutorial(pageId) {
    if (activeUser && !activeAirline.skipTutorial) {
        if (pageId === "worldMap" || pageId === "/") {
            if (!activeAirline.headquarterAirport) {
                queueTutorial("tutorialWelcome")
                queueTutorial("tutorialViewAirport")
            } else if ($.isEmptyObject(flightPaths)) {
                queueTutorial("tutorialSetupLink")
            }
        } else if (pageId == "airport") {
            queueTutorial("tutorialAirportDetails")
            if (!activeAirline.headquarterAirport) {
                queueTutorial("tutorialBuildHq")
            }
        } else if (pageId == "oil") {
            queueTutorial("tutorialOilIntro1")
            queueTutorial("tutorialOilIntro2")
            queueTutorial("tutorialOilIntro3")
        }
    }
}

function closeTutorial($tutorialModal) {
    activeTutorial = undefined
    tutorialsCompleted.add($tutorialModal.attr('id'))
    $.ajax({
        type: 'POST',
        url: "airlines/" + activeAirline.id + "/completed-tutorial/" + $tutorialModal.attr('id') + "?category=" + $tutorialModal.data('category'),
        data: { } ,
		contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {

        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function promptSkipTutorial(event) {
    event.stopPropagation()
    promptConfirm("Disable all tutorials?", setSkipTutorial, true)
}


function setSkipTutorial(skipTutorial) {

    if (!skipTutorial) {
        tutorialsCompleted = new Set()
    } else {
        if (activeTutorial) {
            closeModal($('#' + activeTutorial))
        }
    }
    activeAirline.skipTutorial = skipTutorial
    activeTutorial = undefined
    tutorialQueue = []

    $.ajax({
        type: 'POST',
        url: "airlines/" + activeAirline.id + "/tutorial?skipTutorial=" + skipTutorial,
        data: { } ,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {

        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}


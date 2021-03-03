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

    $('.tab-icon').bind('click.tutorial', function() {
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
    promptQueue.push({ htmlId : promptId, args : args }) //id here determines the ID of the html popup, not the prompt itself
    if (!promptInterval) {
        promptInterval = setInterval('showPrompt()', 100)
    }
}

function queueNotice(noticeJson) {
    //map the category to html element ID here
    var htmlId
    var category = noticeJson.category
    if (category === "LEVEL_UP") {
        htmlId = "levelUpPopup"
    } else if (category === "LOYALIST") {
        htmlId = "loyalistMilestonePopup"
    } else {
        console.warn("Unhandled notice " + noticeJson)
    }

    if (htmlId) {
        queuePrompt(htmlId, noticeJson)
    }
}

function queueTutorialByJson(json) {
    //map the category/id to html element ID here
    var htmlId
    var category = json.category
    if (category === "airlineGrade") {
        htmlId = "tutorialAirlineGrade"
    } else if (category === "loyalist") {
        htmlId = "tutorialLoyalistMilestone"
    } else {
        console.warn("Unhandled tutorial " + noticeJson)
    }

    if (htmlId) {
        queueTutorial(htmlId)
    }
}

function showPrompt() {
    if (!$('#announcementModal').is(':visible')) {
        if (!activePrompt) {
            if (promptQueue.length > 0) {
                activePrompt = promptQueue.shift()
                var promptId = '#' + activePrompt.htmlId
                if ($(promptId).data("function")) {
                    $(promptId).data("function")(activePrompt.args)
                } else {
                    $(promptId).fadeIn(500)
                }
                if ($(promptId).hasClass('notice')) {
                    $.ajax({
                        type: 'POST',
                        url: "airlines/" + activeAirline.id + "/completed-notice/" + $(promptId).data('id') + "?category=" + $(promptId).data('category'),
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
                    $(promptId).data('closeCallback', function() {
                        closeNotice($(promptId))
                    })
                } else {
                    $(promptId).data('closeCallback', function() {
                        closePrompt($(promptId))
                    })
                }
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

function closeNotice($promptModal) {
    closePrompt($promptModal)

//    $.ajax({
//        type: 'POST',
//        url: "airlines/" + activeAirline.id + "/completed-notice/" + $promptModal.data('id') + "?category=" + $promptModal.data('category'),
//        data: { } ,
//        contentType: 'application/json; charset=utf-8',
//        dataType: 'json',
//        success: function(result) {
//
//        },
//        error: function(jqXHR, textStatus, errorThrown) {
//                console.log(JSON.stringify(jqXHR));
//                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
//        }
//    });
}


function closePrompt($promptModal) {
    activePrompt = undefined
    var callback = $promptModal.data('promptCloseCallback')
    if (callback) {
        callback()
    }
}


function queueTutorial(tutorial) {
    if (!tutorialsCompleted.has(tutorial) && !tutorialQueue.includes(tutorial)) {
        tutorialQueue.push(tutorial)
    }
    if (!promptInterval) {
        promptInterval = setInterval('showPrompt()', 100)
    }

}

function checkTutorial(flowId) {
    if (activeUser && !activeAirline.skipTutorial) {
        if (flowId === "worldMap" || flowId === "/") {
            if (!activeAirline.headquarterAirport) {
                queueTutorial("tutorialWelcome")
                queueTutorial("tutorialControl")
                queueTutorial("tutorialViewAirport")
            } else if ($.isEmptyObject(flightPaths)) {
                queueTutorial("tutorialSetupLink1")
            }
        } else if (flowId == "airport") {
            queueTutorial("tutorialAirportDetails")
            if (!activeAirline.headquarterAirport) {
                queueTutorial("tutorialBuildHq")
            }
        } else if (flowId == "office") {
            queueTutorial("tutorialOffice1")
            queueTutorial("tutorialOffice2")
            queueTutorial("tutorialOffice3")
        } else if (flowId == "oil") {
            queueTutorial("tutorialOilIntro1")
            queueTutorial("tutorialOilIntro2")
            queueTutorial("tutorialOilIntro3")
        } else if (flowId == "planLink") {
            queueTutorial("tutorialSetupLink2")
            queueTutorial("tutorialSetupLink3")
        } else if (flowId == "negotiation") {
            queueTutorial("tutorialNegotiation1")
            queueTutorial("tutorialNegotiation2")
            queueTutorial("tutorialNegotiation3")
            queueTutorial("tutorialNegotiation4")
            queueTutorial("tutorialNegotiation5")
        } else if (flowId == "search") {
            queueTutorial("tutorialSearch1")
            queueTutorial("tutorialSearch2")
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

function initNotices() {
    $('#levelUpPopup').data('function', function(json) {
        $('#levelUpPopup').data('id', json.id)
        $('#levelUpPopup').data('category', json.category)
        showLevelUpPopup(json.level, json.description)
    })
    $('#levelUpPopup').data('promptCloseCallback', stopFirework)

    $('#loyalistMilestonePopup').data('function', function(json) {
        $('#loyalistMilestonePopup').data('id', json.id)
        $('#loyalistMilestonePopup').data('category', json.category)
        showLoyalistPopup(json.level, json.description)
    })
    $('#loyalistMilestonePopup').data('promptCloseCallback', stopFirework)
}

function showLevelUpPopup(level, description) {
    var $popup = $('#levelUpPopup')
    var $starBar = $popup.find('.levelStarBar')
    $starBar.empty()
    $starBar.append(getGradeStarsImgs(level))

    $('#levelUpPopup .description').text(description)
    updateHeadquartersMap($('#levelUpPopup .headquartersMap'), activeAirline.id)
    $popup.fadeIn(500)

    startFirework(20000, Math.floor(level / 2) + 1)
}

function showLoyalistPopup(level, description) {
    var $popup = $('#loyalistMilestonePopup')

    $('#loyalistMilestonePopup .description').text(description)
    updateHeadquartersMap($('#loyalistMilestonePopup .headquartersMap'), activeAirline.id)
    $popup.fadeIn(500)

    startFirework(20000, level)
}
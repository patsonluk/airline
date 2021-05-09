function adminAction(action, targetUserId, callback) {
	var url = "/admin-action/" + action + "/" + targetUserId
	var selectedAirlineId =  $("#rivalDetails .adminActions").data("airlineId")

    var data = { }
	$.ajax({
		type: 'PUT',
		url: url,
	    data: JSON.stringify(data),
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
            showRivalsCanvas(selectedAirlineId)
            if (callback) {
                callback()
            }
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function invalidateImage(imageType) {
	var url = "/admin/invalidate-image/" + activeAirportId +  "/" + imageType
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
            showAirportDetails(activeAirportId)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}
function isAdmin() {
    return activeUser && activeUser.level >= 10
}

function isSuperAdmin() {
    return activeUser && activeUser.level >= 20
}

function initAdminActions() {
    if (isAdmin()) {
        $(".adminActions").show()
    } else {
        $(".adminActions").hide()
    }
    if (isSuperAdmin()) {
        $(".superAdminActions").show()
    } else {
        $(".superAdminActions").hide()
    }
}


function showAdminActions(airline) {
    $("#rivalDetails .adminActions").data("airlineId", airline.id)
    $("#rivalDetails .adminActions").data("userId", airline.userId)
    $("#rivalDetails .adminActions .username").text(airline.username)
    $("#rivalDetails .adminActions .userId").text(airline.userId)
    $("#rivalDetails .adminActions .status").text(airline.userStatus)
}

function ban() {
    adminAction("ban", $("#rivalDetails .adminActions").data("userId"))
}
function unban() {
    adminAction("un-ban", $("#rivalDetails .adminActions").data("userId"))
}
function banChat() {
    adminAction("ban-chat", $("#rivalDetails .adminActions").data("userId"))
}
function invalidateCustomization() {
    var airlineId = $("#rivalDetails .adminActions").data("airlineId")
    var url = "/admin/invalidate-customization/" + airlineId
    $.ajax({
        type: 'GET',
        url: url,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            showRivalsCanvas(selectedAirlineId)
        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function switchUser() {
    adminAction("switch", $("#rivalDetails .adminActions").data("userId"), function() { loadUser(false)})
}

function promptAirlineMessage() {
    var selectedAirlineId =  $("#rivalDetails .adminActions").data("airlineId")
    var airline = loadedRivalsById[selectedAirlineId]
    $('#sendAirlineMessageModal .airlineName').text(airline.name)
    $('#sendAirlineMessageModal .sendMessage').val('')
    $('#sendAirlineMessageModal').fadeIn(500)
}

function sendAirlineMessage() {
    var selectedAirlineId = $("#rivalDetails .adminActions").data("airlineId")
    var url = "/admin/send-airline-message/" + selectedAirlineId

    var data = { "message" : $('#sendAirlineMessageModal .sendMessage').val() }
    $.ajax({
        type: 'PUT',
        url: url,
        data: JSON.stringify(data),
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            closeModal($('#sendAirlineMessageModal'))
        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function promptBroadcastMessage() {
    $('#sendBroadcastMessageModal .sendMessage').val('')
    $('#sendBroadcastMessageModal').fadeIn(500)
}

function sendBroadcastMessage() {
    var url = "/admin/send-broadcast-message"
    var data = { "message" : $('#sendBroadcastMessageModal .sendMessage').val() }
    	$.ajax({
    		type: 'PUT',
    		url: url,
    	    data: JSON.stringify(data),
    	    contentType: 'application/json; charset=utf-8',
    	    dataType: 'json',
    	    success: function(result) {
                closeModal($('#sendBroadcastMessageModal'))
    	    },
            error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    	});
}

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
function isAdmin() {
    return activeUser && activeUser.level >= 10
}

function isSuperAdmin() {
    return activeUser && activeUser.level >= 20
}


function showAdminActions(airline) {
    $("#rivalDetails .adminActions").data("airlineId", airline.id)
    $("#rivalDetails .adminActions").data("userId", airline.userId)
    $("#rivalDetails .adminActions .username").text(airline.username)
    $("#rivalDetails .adminActions .userId").text(airline.userId)
    $("#rivalDetails .adminActions .status").text(airline.userStatus)


    if (isAdmin()) {
        $("#rivalDetails .adminActions").show()
    } else {
        $("#rivalDetails .adminActions").hide()
    }
    if (isSuperAdmin()) {
        $("#rivalDetails .superAdminActions").show()
    } else {
        $("#rivalDetails .superAdminActions").hide()
    }
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
function switchUser() {
    adminAction("switch", $("#rivalDetails .adminActions").data("userId"), function() { loadUser(false)})
}
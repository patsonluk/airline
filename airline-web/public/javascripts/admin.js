function adminAction(action, targetUserId) {
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

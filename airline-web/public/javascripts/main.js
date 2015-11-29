var map
var markers
var activeAirline
var activeUser

$( document ).ready(function() {
	if ($.cookie('sessionActive')) {
		loadUser(false)
	} else {
		refreshLoginBar()
	}
})

function refreshLoginBar() {
	if (!activeUser) {
		setActiveDiv($("#loginDiv"))
	} else {
		$("#currentUserName").text(activeUser.userName)
		setActiveDiv($("#logoutDiv"))
	}
}

function loadUser(isLogin) {
	var ajaxCall = {
	  type: "POST",
	  url: "login",
	  success: function(user) {
		  if (user) {
			  activeUser = user
			  $.cookie('sessionActive', 'true');
			  $("#loginUserName").val("")
			  $("#loginPassword").val("")
			  refreshLoginBar()
		  }
		  if (user.airlineIds.length > 0) {
			  selectAirline(user.airlineIds[0])
		  }
		  
	  },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	}
	if (isLogin) {
		var userName = $("#loginUserName").val()
		var password = $("#loginPassword").val()
		 ajaxCall.headers = {
			    "Authorization": "Basic " + btoa(userName + ":" + password)
			  }

	}
	
	$.ajax(ajaxCall);
}

function login()  {
	loadUser(true)
}

function logout() {
	$.ajax
	({
	  type: "POST",
	  url: "logout",
	  async: false,
	  success: function(message) {
	    	console.log(message)
	    	activeUser = null
	    	activeAirline = null
	    	$.removeCookie('sessionActive')
	    	refreshLoginBar()
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}


function initMap() {
  map = new google.maps.Map(document.getElementById('map'), {
	center: {lat: 20, lng: 150.644},
   	zoom : 2,
   	minZoom : 2
  });
  google.maps.event.addListener(map, 'zoom_changed', function() {
	    var zoom = map.getZoom();
	    // iterate over markers and call setVisible
	    $.each(markers, function( key, marker ) {
	        marker.setVisible(isShowMarker(marker, zoom));
	    })
  });  
  getAirports()
}


function updateAllPanels(airlineId) {
	updateAirlineInfo(airlineId)
}

function updateAirlineInfo(airlineId) {
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airline) {
	    	$("#balance").text(airline.balance)
	    	activeAirline = airline
	    	updateAirplaneList($("#airplaneList"))
	    	updateLinksInfo()
	    	updateAirportMarkers(airline)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function setActiveDiv(activeDiv) {
//	activeDiv.siblings().hide(500)
//activeDiv.show(500)
	if (activeDiv.siblings(":visible").length){
		activeDiv.siblings(":visible").fadeOut(200, function() { activeDiv.fadeIn(200) })
	} else {
		activeDiv.fadeIn(200)
	}
}


function appendConsole(message) {
	$('#console').append( message + '<br/>')
}


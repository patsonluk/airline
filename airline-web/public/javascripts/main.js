var map
var markers
var activeAirline
var activeUser
var selectedLink
var activeWatchedLink
var currentTime

$( document ).ready(function() {
	if ($.cookie('sessionActive')) {
		loadUser(false)
	} else {
		refreshLoginBar()
	}
	if ($("#floatMessage").val()) {
		showFloatMessage($("#floatMessage").val())
	}
	$(window).scroll(function()
	{
  		$('#floatBackButton').animate({top: ($(window).scrollTop() + 100) + "px" },{queue: false, duration: 350});
	});
	
	//plotSeatConfigurationGauge($("#seatConfigurationGauge"), {"first" : 0, "business" : 0, "economy" : 220}, 220)
})

function showFloatMessage(message, timeout = 3000) {
	$("#floatMessageBox").text(message)
	var centerX = $("#floatMessageBox").parent().width() / 2 - $("#floatMessageBox").width() / 2 
	$("#floatMessageBox").css({ top:"-=20px", left: centerX, opacity:100})
	$("#floatMessageBox").show()
	$("#floatMessageBox").animate({ top:"0px" }, "fast", function() {
		if (timeout > 0) {
			setTimeout(function() { 
				console.log("closing")
				$('#floatMessageBox').animate({ top:"-=20px",opacity:0 }, "slow", function() {
					$('#floatMessageBox').hide()
				})
			}, timeout)
		}
	})
	
	//scroll the message box to the top offset of browser's scroll bar
	$(window).scroll(function()
	{
  		$('#floatMessageBox').animate({top:$(window).scrollTop()+"px" },{queue: false, duration: 350});
	});
}

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
			  showFloatMessage("Successfully logged in")
			  refreshLoginBar()
		  }
		  if (user.airlineIds.length > 0) {
			  selectAirline(user.airlineIds[0])
		  }
		  
	  },
	    error: function(jqXHR, textStatus, errorThrown) {
	    	if (jqXHR.status == 401) {
	    		showFloatMessage("Incorrect username or password")
	    	} else {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    	}
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
	    	showFloatMessage("Successfully logged out")
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
   	minZoom : 2,
   	gestureHandling: 'greedy'
  });
  google.maps.event.addListener(map, 'zoom_changed', function() {
	    var zoom = map.getZoom();
	    // iterate over markers and call setVisible
	    $.each(markers, function( key, marker ) {
	        marker.setVisible(isShowMarker(marker, zoom));
	    })
  });  
  
  $("#vipButton").index = 1
  map.controls[google.maps.ControlPosition.RIGHT_BOTTOM].push($("#vipButton")[0]);
  
  $("#linkHistoryButton").index = 2
  map.controls[google.maps.ControlPosition.RIGHT_BOTTOM].push($("#linkHistoryButton")[0]);
  
  getAirports()
}


function updateAllPanels(airlineId) {
	updateAirlineInfo(airlineId)
}

//does not remove or add any components
function refreshPanels(airlineId) {
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airline) {
	    	refreshTopBar(airline)
	    	refreshLinks()
	    	if (selectedLink) {
	    		refreshLinkDetails(selectedLink)
	    	}
	    	updateAirplaneList() //refresh all airplane list for now
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function getAirlineCategory(reputation) {
	if (reputation < 10) {
		return "New Airline"
	} else if (reputation < 20) {
		return "Local Airline"
	} else if (reputation < 30) {
		return "Municipal Airline"
	} else if (reputation < 40) {
		return "Regional Airline"
	} else if (reputation < 50) {
		return "Continental Airline"
	} else if (reputation < 60) {
		return "Lesser International Airline"
	} else if (reputation < 70) {
		return "Third-class International Airline"
	} else if (reputation < 80) {
		return "Second-class International Airline"
	} else if (reputation < 90) {
		return "Major Internation Airline"
	} else {
		return "Top Internation Airline"
	}
}

var totalmillisecPerWeek = 7 * 24 * 60 * 60 * 1000
var refreshInterval = 100 //100 millsec
var incrementPerInterval = totalmillisecPerWeek / (4 * 60 * 1000) * refreshInterval //current 4 minutes per week
var refreshIntervalId
var days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];


function updateTime(cycle, fraction) {
	currentTime = (cycle + fraction) * totalmillisecPerWeek 
	if (!refreshIntervalId) { //start incrementing
		refreshIntervalId = setInterval( function() {
			currentTime += incrementPerInterval
			var date = new Date(currentTime)
			$("#currentTime").text("(" + days[date.getDay()] + ") " + padBefore(date.getMonth() + 1, "0", 2) + '/' + padBefore(date.getDate(), "0", 2) + '/' +  date.getFullYear() + " " + padBefore(date.getHours(), "0", 2) + ":00")
		}, refreshInterval);
	}
}


function appendConsole(message) {
	$('#console').append( message + '<br/>')
}


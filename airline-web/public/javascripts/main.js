var map
var markers
var baseMarkers = []
var activeAirline
var activeUser
var selectedLink
var currentTime
var currentCycle

$( document ).ready(function() {
	if ($.cookie('sessionActive')) {
		loadUser(false)
	} else {
		hideUserSpecificElements()
		refreshLoginBar()
		getAirports();
		printConsole("Please log in")
	}
	loadAllCountries()
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
	  async: false,
	  success: function(user) {
		  if (user) {
			  activeUser = user
			  $.cookie('sessionActive', 'true');
			  $("#loginUserName").val("")
			  $("#loginPassword").val("")
			  
			  if (isLogin) {
				  showFloatMessage("Successfully logged in")
			  }
			  
			  refreshLoginBar()
			  printConsole('') //clear console
			  getAirports();
			  showUserSpecificElements();
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

function onGoogleLogin(googleUser) {
	var profile = googleUser.getBasicProfile();
    console.log('ID: ' + profile.getId()); // Do not send to your backend! Use an ID token instead.
	console.log('Name: ' + profile.getName());
	console.log('Image URL: ' + profile.getImageUrl());
	console.log('Email: ' + profile.getEmail()); // This is null if the 'email' scope is not present.
	loginType='plain'
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
	    	hideUserSpecficElements()
	    	$.removeCookie('sessionActive')
	    	//refreshLoginBar()
	    	//showFloatMessage("Successfully logged out")
	    	location.reload();
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
	
	removeMarkers()
}

function showUserSpecificElements() {
	$('.user-specific-tab').show()
	$('#topBarDetails').show()
}

function hideUserSpecificElements() {
	$('.user-specific-tab').hide()
	$('#topBarDetails').hide()
}


function initMap() {
  map = new google.maps.Map(document.getElementById('map'), {
	center: {lat: 20, lng: 150.644},
   	zoom : 2,
   	minZoom : 2,
   	gestureHandling: 'greedy',
   	styles: 
   	[
   	  {
   	    "elementType": "geometry",
   	    "stylers": [
   	      {
   	        "color": "#1d2c4d"
   	      }
   	    ]
   	  },
   	  {
   	    "elementType": "labels.text.fill",
   	    "stylers": [
   	      {
   	        "color": "#8ec3b9"
   	      }
   	    ]
   	  },
   	  {
   	    "elementType": "labels.text.stroke",
   	    "stylers": [
   	      {
   	        "color": "#1a3646"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "administrative.country",
   	    "elementType": "geometry.stroke",
   	    "stylers": [
   	      {
   	        "color": "#4b6878"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "administrative.land_parcel",
   	    "elementType": "labels",
   	    "stylers": [
   	      {
   	        "visibility": "off"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "administrative.land_parcel",
   	    "elementType": "labels.text.fill",
   	    "stylers": [
   	      {
   	        "color": "#64779e"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "administrative.province",
   	    "elementType": "geometry.stroke",
   	    "stylers": [
   	      {
   	        "color": "#4b6878"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "landscape.man_made",
   	    "elementType": "geometry.stroke",
   	    "stylers": [
   	      {
   	        "color": "#334e87"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "landscape.natural",
   	    "elementType": "geometry",
   	    "stylers": [
   	      {
   	        "color": "#023e58"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "poi",
   	    "elementType": "geometry",
   	    "stylers": [
   	      {
   	        "color": "#283d6a"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "poi",
   	    "elementType": "labels.text",
   	    "stylers": [
   	      {
   	        "visibility": "off"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "poi",
   	    "elementType": "labels.text.fill",
   	    "stylers": [
   	      {
   	        "color": "#6f9ba5"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "poi",
   	    "elementType": "labels.text.stroke",
   	    "stylers": [
   	      {
   	        "color": "#1d2c4d"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "poi.business",
   	    "stylers": [
   	      {
   	        "visibility": "off"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "poi.park",
   	    "elementType": "geometry.fill",
   	    "stylers": [
   	      {
   	        "color": "#023e58"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "poi.park",
   	    "elementType": "labels.text.fill",
   	    "stylers": [
   	      {
   	        "color": "#3C7680"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "road",
   	    "stylers": [
   	      {
   	        "visibility": "off"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "road",
   	    "elementType": "geometry",
   	    "stylers": [
   	      {
   	        "color": "#304a7d"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "road",
   	    "elementType": "labels.icon",
   	    "stylers": [
   	      {
   	        "visibility": "off"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "road",
   	    "elementType": "labels.text.fill",
   	    "stylers": [
   	      {
   	        "color": "#98a5be"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "road",
   	    "elementType": "labels.text.stroke",
   	    "stylers": [
   	      {
   	        "color": "#1d2c4d"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "road.highway",
   	    "elementType": "geometry",
   	    "stylers": [
   	      {
   	        "color": "#2c6675"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "road.highway",
   	    "elementType": "geometry.stroke",
   	    "stylers": [
   	      {
   	        "color": "#255763"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "road.highway",
   	    "elementType": "labels.text.fill",
   	    "stylers": [
   	      {
   	        "color": "#b0d5ce"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "road.highway",
   	    "elementType": "labels.text.stroke",
   	    "stylers": [
   	      {
   	        "color": "#023e58"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "road.local",
   	    "elementType": "labels",
   	    "stylers": [
   	      {
   	        "visibility": "off"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "transit",
   	    "stylers": [
   	      {
   	        "visibility": "off"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "transit",
   	    "elementType": "labels.text.fill",
   	    "stylers": [
   	      {
   	        "color": "#98a5be"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "transit",
   	    "elementType": "labels.text.stroke",
   	    "stylers": [
   	      {
   	        "color": "#1d2c4d"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "transit.line",
   	    "elementType": "geometry.fill",
   	    "stylers": [
   	      {
   	        "color": "#283d6a"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "transit.station",
   	    "elementType": "geometry",
   	    "stylers": [
   	      {
   	        "color": "#3a4762"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "water",
   	    "elementType": "geometry",
   	    "stylers": [
   	      {
   	        "color": "#0e1626"
   	      }
   	    ]
   	  },
   	  {
   	    "featureType": "water",
   	    "elementType": "labels.text.fill",
   	    "stylers": [
   	      {
   	        "color": "#4e6d70"
   	      }
   	    ]
   	  }
   	]
   		
  });
  google.maps.event.addListener(map, 'zoom_changed', function() {
	    var zoom = map.getZoom();
	    // iterate over markers and call setVisible
	    $.each(markers, function( key, marker ) {
	        marker.setVisible(isShowMarker(marker, zoom));
	    })
  });  
  
//  $("#vipButton").index = 1
//  map.controls[google.maps.ControlPosition.RIGHT_BOTTOM].push($("#vipButton")[0]);
  
//  $("#linkHistoryButton").index = 2
//  map.controls[google.maps.ControlPosition.RIGHT_BOTTOM].push($("#linkHistoryButton")[0]);
  
//  map.controls[google.maps.ControlPosition.TOP_CENTER].push($("#hideLinkHistoryButton")[0]);
//  var linkControlDiv = document.createElement('div');
//  linkControlDiv.id = 'linkControlDiv';
//  var linkControl = new LinkHistoryControl(linkControlDiv, map);
//
//  $(linkControlDiv).hide()
//  
//  linkControlDiv.index = 1;
//  map.controls[google.maps.ControlPosition.TOP_CENTER].push(linkControlDiv);
//  map.controls[google.maps.ControlPosition.TOP_RIGHT].push(hideLinkHistoryButton);
  
}

function LinkHistoryControl(controlDiv, map) {
    // Set CSS for the control border.
    var controlUI = document.createElement('div');
    controlUI.style.backgroundColor = '#fff';
    controlUI.style.border = '2px solid #fff';
    controlUI.style.borderRadius = '3px';
    controlUI.style.boxShadow = ' 0px 1px 4px -1px rgba(0,0,0,.3)';
    //controlUI.style.cursor = 'pointer';
    controlUI.style.marginBottom = '22px';
    controlUI.style.textAlign = 'center';
    controlUI.title = 'Click to recenter the map';
    controlUI.style.padding = '8px';
    controlUI.style.margin= '10px';
    controlUI.style.verticalAlign = 'middle';
    controlDiv.appendChild(controlUI);
    

    $(controlUI).append("<img src='assets/images/icons/24-arrow-180.png' class='button' onclick='toggleLinkHistoryView(false)'  title='Toggle passenger history view'/>")
    // Set CSS for the control interior.
    $(controlUI).append("<span id='linkHistoryText' style='color: rgb(86, 86, 86); font-family: Roboto, Arial, sans-serif; font-size: 11px;'></span>");
    
    $(controlUI).append("<img src='assets/images/icons/24-arrow.png' class='button' onclick='toggleLinkHistoryView(false)'  title='Toggle passenger history view'/>")

    // Setup the click event listeners: simply set the map to Chicago.
    controlUI.addEventListener('click', function() {
      map.setCenter(chicago);
    });

  }


function updateAllPanels(airlineId) {
	updateAirlineInfo(airlineId)
	
	if (activeAirline) {
		if (!activeAirline.headquarterAirport) {
			printConsole("Zoom into the map and click on an airport icon. Select 'View Airport' to view details on the airport and build your airline Headquarter. Smaller airports will only show when you zoom close enough", 1, true, true)
		} else if ($.isEmptyObject(flightPaths)) {
			printConsole("Select another airport and click 'Plan Route' to plan your first route to it. You might want to select a closer domestic airport for shorter haul airplanes within your budget", 1, true, true)
		}
		
	}
	
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
	    	if ($("#linkDetails").is(":visible")) {
	    		refreshLinkDetails(selectedLink)
	    	}
	    	if ($("#linksCanvas").is(":visible")) {
	    		loadLinksTable()
	    	}
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

var totalmillisecPerWeek = 7 * 24 * 60 * 60 * 1000
var refreshInterval = 100 //100 millsec
var incrementPerInterval = totalmillisecPerWeek / (4 * 60 * 1000) * refreshInterval //current 4 minutes per week
var refreshIntervalId
var days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];


function updateTime(cycle, fraction) {
	currrentCycle = cycle
	currentTime = (cycle + fraction) * totalmillisecPerWeek 
	if (!refreshIntervalId) { //start incrementing
		refreshIntervalId = setInterval( function() {
			currentTime += incrementPerInterval
			var date = new Date(currentTime)
			$("#currentTime").text("(" + days[date.getDay()] + ") " + padBefore(date.getMonth() + 1, "0", 2) + '/' + padBefore(date.getDate(), "0", 2) + '/' +  date.getFullYear() + " " + padBefore(date.getHours(), "0", 2) + ":00")
		}, refreshInterval);
	}
}


function printConsole(message, messageLevel = 1, activateConsole = false, persistMessage = false) {
	var messageClass
	if (messageLevel == 1) {
		messageClass = 'actionMessage'
	} else {
		messageClass = 'errorMessage'
	}

	if (message == '') { //try to clear message, check if there was a persistent message
		var previousMessage = $('#console #consoleMessage').data('persistentMessage')
		if (previousMessage) {
			message = previousMessage
		}
	}
	
	if (persistMessage) {
		$('#console #consoleMessage').data('persistentMessage', message)
	}
	var consoleVisible = $('#console #consoleMessage').is(':visible')
	
	if (consoleVisible) {
		$('#console #consoleMessage').fadeOut('slow', function() { //fade out and reset positions
			$('#console #consoleMessage').text(message)
			$('#console #consoleMessage').removeClass().addClass(messageClass)
			$('#console #consoleMessage').fadeIn('slow')
		}) 
	} else {
		$('#console #consoleMessage').text(message)
		$('#console #consoleMessage').removeClass().addClass(messageClass)
		if (activateConsole) {
			$('#console #consoleMessage').fadeIn('slow')
		}
	}
}

function toggleConsoleMessage() {
	if ($('#console #consoleMessage').is(':visible')) {
		$('#console #consoleMessage').fadeOut('slow')
	} else {
		$('#console #consoleMessage').fadeIn('slow')
	}
}

function showWorldMap() {
	setActiveDiv($('#worldMapCanvas'));
	highlightTab($('#worldMapCanvasTab'))
	$('#sidePanel').appendTo($('#worldMapCanvas'))
	
	if (selectedLink) {
		selectLinkFromMap(selectedLink, true)
	}
}



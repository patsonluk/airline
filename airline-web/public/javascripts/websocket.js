var port = window.location.port

var wsProtocol

if (window.location.protocol == "https:"){
	wsProtocol = "wss:"
	if (!port) {
		port = 443
	}
} else {
	wsProtocol = "ws:"
	if (!port) {
		port = 80
	}
}

var wsUri = wsProtocol + "//" +  window.location.hostname + ":" + port + "/wsWithActor";
var websocket;
var selectedAirlineId

$( document ).ready(function() {})

function checkWebSocket(selectedAirlineId) {
    if (websocket.readyState === WebSocket.CLOSED) {
        connectWebSocket(selectedAirlineId)
    }
}

function connectWebSocket(airlineId) {
	websocket = new WebSocket(wsUri); 
	websocket.onopen = function(evt) {
		sendMessage(airlineId)  //send airlineId to indicate we want to listen to messages for this airline Id
		console.log("successfully open socket on airline " + airlineId)
	}; 
	websocket.onclose = function(evt) { onClose(evt) }; 
	websocket.onmessage = function(evt) { onMessage(evt) }; 
	websocket.onerror = function(evt) { onError(evt) }; 
}

function initWebSocket(airlineId) {
	selectedAirlineId = airlineId
	connectWebSocket(airlineId)
}

function onClose(evt) {}  
function onMessage(evt) { //right now the message is just the cycle #, so refresh the panels
	var json = JSON.parse(evt.data)
	if (json.ping) { //ok
        return
    }
	console.log("websocket received message : " + evt.data)
	
	if (json.messageType == "cycleInfo") { //update time
		updateTime(json.cycle, json.fraction, json.cycleDurationEstimation)
//	} else if (json.messageType == "cycleStart") { //update time
//		updateTime(json.cycle, 0)
	} else if (json.messageType == "cycleCompleted") {
		if (selectedAirlineId) {
			refreshPanels(selectedAirlineId)
		}
	} else if (json.messageType == "broadcastMessage") {
        queuePrompt("broadcastMessagePopup", json.message)
    } else if (json.messageType == "airlineMessage") {
        queuePrompt("airlineMessagePopup", json.message)
    } else if (json.messageType == "notice") {
        queueNotice(json)
    } else if (json.messageType == "tutorial") {
        queueTutorialByJson(json)
    } else if (json.messageType == "pendingAction") {
        handlePendingActions(json.actions)
    } else {
		console.warn("unknown message type " + evt.data)
	}
}  
function onError(evt) {
	console.log(evt)
} 

function sendMessage(message) {
	if (websocket.readyState === 1) {
		websocket.send(message);
	} else {
		setTimeout(function() { sendMessage(message) }, 1000)
	}
}
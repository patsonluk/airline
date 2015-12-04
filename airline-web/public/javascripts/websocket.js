var wsUri = "ws://localhost:9000/wsWithActor"; 
var balanceDiv;
var websocket;
var selectedAirlineId

$( document ).ready(function() {})

function connectWebSocket(airlineId) {
	websocket = new WebSocket(wsUri); 
	websocket.onopen = function(evt) {
		sendMessage(airlineId) 
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
function onMessage(evt) {
	if (selectedAirlineId) {
		refreshPanels(selectedAirlineId)
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
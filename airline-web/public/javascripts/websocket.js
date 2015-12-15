var wsUri = "ws://localhost:9000/wsWithActor"; 
var websocket;
var selectedAirlineId

$( document ).ready(function() {})

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
	console.log("websocket received message : " + evt.data)
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
var wsUri = "ws://localhost:9000/wsWithActor"; 
var balanceDiv;
var websocket;
var airlineId

$( document ).ready(function() {
	balanceDiv = $("#balance");
	connectWebSocket();
})

function connectWebSocket(airlineId) {
	websocket = new WebSocket(wsUri); 
	websocket.onopen = function(evt) { onOpen(evt) }; 
	websocket.onclose = function(evt) { onClose(evt) }; 
	websocket.onmessage = function(evt) { onMessage(evt) }; 
	websocket.onerror = function(evt) { onError(evt) }; 
}

function setWebSocketAirlineId(airlineId) {
	sendMessage(airlineId)
}

function onOpen(evt) {}  
function onClose(evt) {}  
function onMessage(evt) {
	balanceDiv.text(evt.data)
}  
function onError(evt) {
	balanceDiv.text("???") 
} 

function sendMessage(message) {
	if (websocket.readyState === 1) {
		websocket.send(message);
	} else {
		setTimeout(function() { sendMessage(message) }, 1000)
	}
}
angular.module("ChatApp", []).controller("ChatController", function($scope){
   
   // var ws = new WebSocket("ws://localhost:9000/chat");
   // connect to websockets endpoint of our server
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
			// Change this for production
			port = 9000
		}
	}

	var wsUri = wsProtocol + "//" +  window.location.hostname + ":" + port + "/chat"; 
    var ws = new WebSocket(wsUri);

  // binding model for the UI
  var chat = this;
  chat.messages = [];
  chat.currentMessage = "";
  chat.username = "";

  // what happens when user enters message
  chat.sendMessage = function() {
    var text = airlineName+ ": " + chat.currentMessage;
    //chat.messages.push(text);
    chat.currentMessage = "";
    // send it to the server through websockets
    ws.send(text);
  };

  // what to do when we receive message from the webserver
  ws.onmessage = function(msg) {
    chat.messages.push(msg.data);
    $scope.$digest();
  };
});





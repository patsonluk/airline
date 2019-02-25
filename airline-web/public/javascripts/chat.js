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
    var ws = new ReconnectingWebSocket(wsUri);

  // binding model for the UI
  var chat = this;
  chat.gmessages = []; // Global
  chat.amessages = []; // Alliance
  chat.currentMessage = "";
  chat.username = "";

  // what happens when user enters message
  chat.sendMessage = function() {
	  if (activeAirline && (chat.currentMessage.length > 0)) {
		var active_tab = $("li.tab-link.current").attr('data-tab');
		var text = { room: active_tab, text: chat.currentMessage };
	    //chat.messages.push(text);
	    chat.currentMessage = "";
	    // send it to the server through websockets
	    ws.send(JSON.stringify(text));
		
	  }
  };

   ws.onopen = function () {
	   $("#live-chat i").css({"background-image":"url(\"../../assets/images/icons/32px/balloon-chat.png\")"});
	   chat.gmessages.push("Chat Connected");
	   $scope.$digest();
   }
   
   ws.onclose = function () {
	   $("#live-chat i").css({"background-image":"url(\"../../assets/images/icons/32px/balloon-chat-red.png\")"});
	   chat.gmessages.push("Chat Disconnected");
	   $scope.$digest();
   }
   
  // what to do when we receive message from the webserver
  ws.onmessage = function(msg) {
	var r_text = msg.data;
	console.log(r_text);
	var r_msg = JSON.parse(r_text);
	
	if (r_msg.room == "-1") {
		chat.gmessages.push(r_msg.text);
	} else {
		chat.amessages.push(r_msg.text);
	}
    $scope.$digest();
	
	if (!$('#scroll_lockc').is(":checked")) {
		var scroller = document.getElementById("chatBox-1");
		scroller.scrollTop = scroller.scrollHeight;
		var scroller = document.getElementById("chatBox-2");
		scroller.scrollTop = scroller.scrollHeight;
	}
	if ($('.chat').is(':hidden')) {
		$('.notify-bubble').show(400);
		$('.notify-bubble').text(parseInt($('.notify-bubble').text())+1);
	}
  };
});





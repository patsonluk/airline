var lastMessageId = 0

function updateChatTabs() {
	if (activeUser.allianceName) {
		$("#allianceChatTab").text(activeUser.allianceName)
	} else {
		$("#allianceChatTab").hide()
	}
}

/**
 * Constructor for RateLimit objects
 * @constructor
 * @param {number} [history=5] The size of the tick history
 */
function RateLimit(history) {
    if (!(this instanceof RateLimit)) return new RateLimit(history);
    history = history || 5;

    /**
     * The time this RateLimit object was created
     * @type number
     */
    this.created = parseInt(new Date().getTime() / 1000);
    /**
     * Time of last tick
     * @type number
     */
    this.lasttick = 0;
    /**
     * Stores a bunch of ticks
     * @type array
     */
    this.ticklog = [];

    for (var i = 0; i < history; i++) {
        this.ticklog.push([0, {}]);
    }
}
RateLimit.prototype = {
    /**
     * Checks if the occurrences for this id are within the limit and if true,
     * does a tick and returns true, otherwise returns false.
     * @param id Any kind of identifier
     * @param {number} limit The max occurrences for this identifier
     * @param {number} [now] The time right now
     */
    tick: function(id, now) {
        if (!id) return;
        now = now || parseInt(new Date().getTime() / 1000) - this.created;
        this.lasttick = now;
        var index = now % this.ticklog.length;
        var entry = (this.ticklog[index] &&
                     this.ticklog[index][0] == now)
            ? this.ticklog[index]
            : this.ticklog[index] = [now, {}];
        var timestamp = entry[0],
            tickcount = entry[1];
        tickcount[id] = tickcount[id] ? tickcount[id] + 1: 1;
        return;
    },
    /**
     * Checks if the occurrences for this id are within the limit and if true,
     * does a tick and returns true, otherwise returns false.
     * @param id Any kind of identifier
     * @param {number} limit The max occurrences for this identifier
     * @param {number} [now] The time right now
     * @return {number} The number of ticks by a given id
     */
    count: function(id, now) {
        if (!id) return;
        now = now || parseInt(new Date().getTime() / 1000) - this.created;
        var count = 0;
        for (var i = 0; i < this.ticklog.length; i++) {
            var timestamp = this.ticklog[i][0],
                tickcount = this.ticklog[i][1];
            if (now < timestamp ||
                now - timestamp >= this.ticklog.length) {
                continue;
            }
            if (tickcount[id]) {
                count += tickcount[id];
            }
        }
        return count;
    },
    /**
     * Checks if the occurrences for this id are within the limit and if true,
     * does a tick and returns true, otherwise returns false.
     * @param id Any kind of identifier
     * @param {number} limit The max occurrences for this identifier
     * @param {number} [now] The time right now
     * @return {boolean} True if within the limit
     */
    execute: function(id, limit, now) {
        if (!id) return;
        if (!limit) return;
        now = now || parseInt(new Date().getTime() / 1000) - this.created;
        var success = this.count(id, now) < limit;
        if (success) {
            this.tick(id, now);
        }
        return success;
    }
};

var isFromEmoji = false; //yike ugly!

angular.module("ChatApp", []).controller("ChatController", function($scope, $timeout){
   // var ws = new WebSocket("ws://localhost:9000/chat");
   // connect to websockets endpoint of our server
    var port = window.location.port
    var limit = new RateLimit(60)
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
    var ws = new ReconnectingWebSocket(function() {
        return wsUri + "?last-message-id=" + lastMessageId
    });

  // binding model for the UI
  var chat = this;
  chat.gmessages = []; // Global
  chat.amessages = []; // Alliance
  chat.currentMessage = "";
  chat.username = "";

  // what happens when user enters message
  chat.sendMessage = function(event) {
      if (isFromEmoji) {
        isFromEmoji = false;
        return;
      }
      limit.tick('myevent_id');
      if (activeAirline && (chat.currentMessage.length > 0) && (limit.count('myevent_id') <= 20)) {
        var active_tab = $("li.tab-link.current").attr('data-tab');
        var text = { room: active_tab, text: chat.currentMessage, airlineId: activeAirline.id };
        //chat.messages.push(text);
        chat.currentMessage = "";
        // send it to the server through websockets
        ws.send(JSON.stringify(text));

      } else {
          $timeout(function(){
            $scope.chat.gmessages.push("Message Not Sent : Rate Filter or Invalid Message");
          });

          $timeout(function(){
            var scroller = document.getElementById("chatBox-1");
            scroller.scrollTop = scroller.scrollHeight;
          });

          chat.currentMessage = "";
      }
  };

   ws.onopen = function () {
	    $("#live-chat i").css({"background-image":"url(\"../../assets/images/icons/32px/balloon-chat.png\")"});
		$timeout(function(){ 
			chat.gmessages.push("Chat Connected");
		});
	    $timeout(function(){ 
			var scroller = document.getElementById("chatBox-1");
		    scroller.scrollTop = scroller.scrollHeight;
		});
   };
   
   ws.onclose = function () {
	   $("#live-chat i").css({"background-image":"url(\"../../assets/images/icons/32px/balloon-chat-red.png\")"});
	   $timeout(function(){ 
			chat.gmessages.push("Chat Disconnected");
	   });
	   $timeout(function(){ 
			var scroller = document.getElementById("chatBox-1");
		    scroller.scrollTop = scroller.scrollHeight;
		});
   }
   
  // what to do when we receive message from the webserver
  ws.onmessage = function(msg) {
    if (msg.data == "ping") {
        console.debug("ping from server")
        return
     //ok
    }
	var r_text = msg.data;
	//console.log(r_text);
	var r_msg = JSON.parse(r_text);
	
	if (!r_msg.allianceRoomId) {
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
	emojify.run($('.chat-history.current')[0]);             // translate emoji to images

    lastMessageId = r_msg.id
  };
});

emojify.setConfig({img_dir : 'assets/images/emoji'});


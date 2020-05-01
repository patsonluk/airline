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
  chat.username = "";

  // what happens when user enters message
  chat.sendMessage = function(event) {
      if (isFromEmoji) {
        isFromEmoji = false;
        return;
      }
      limit.tick('myevent_id');
      var currentMessage = $('#chattext').val()
      if (activeAirline && (currentMessage !== "") && (limit.count('myevent_id') <= 20)) {
        var active_tab = $("li.tab-link.current").attr('data-tab');
        var text = { room: active_tab, text: currentMessage, airlineId: activeAirline.id };
        //chat.messages.push(text);
        $('#chattext').val("");
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

          $('#chattext').val("");
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

	var date = new Date(r_msg.timestamp)
	var airlineName = r_msg.airlineName
	var userLevel = r_msg.userLevels
	var hourString = date.getHours()
	var minuteString = date.getMinutes()
	var secondString = date.getSeconds()

	if (hourString < 10) {
	    hourString = "0" + hourString
    }
    if (minuteString < 10) {
        minuteString = "0" + minuteString
    }
    if (secondString < 10) {
        secondString = "0" + secondString
    }

	var dateString = hourString + ":" + minuteString + ":" + secondString
//	var airlineSpan = $("<span>" + airlineName + "</span>")
//	var userIcon = getUserLevelImg(userLevel)
//	airlineSpan.append(userIcon)

    var prefix = "[" + dateString + "] " + airlineName + ": "
	if (!r_msg.allianceRoomId) {
		chat.gmessages.push(prefix + r_msg.text);
	} else {
		chat.amessages.push(prefix + r_msg.text);
	}
    $scope.$digest();


    var isMobileDeviceValue = isMobileDevice()
    $('.chat-history').each (function(){
        emojify.run($(this).find("li:last-child")[0]);   // translate emoji to images
        if (r_msg.imagePermission && !isMobileDeviceValue) {
            replaceImg($(this).find("li:last-child"), prefix)
        }
    })

    adjustScroll()

	if ($('.chat').is(':hidden')) {
		$('.notify-bubble').show(400);
		$('.notify-bubble').text(parseInt($('.notify-bubble').text())+1);
	}

    lastMessageId = r_msg.id
  };
});

emojify.setConfig({img_dir : 'assets/images/emoji'});

function adjustScroll() {
    if (!$('#scroll_lockc').is(":checked")) {
        $(".chat-history").each(function() {
            $(this).scrollTop($(this).prop("scrollHeight"))
        })
    }
}

var imgTag = "/img"
function replaceImg(input, prefix) {
    var text= input.text().trim().substring(prefix.length) //strip the airline name and date

    if (text.startsWith(imgTag)) {
        var src = text.substring(imgTag.length)
        var img = $("<img>")
        img.attr("src", src)
        img.css("max-height", "80px")
        img.css("max-width", "160px")
        img.attr("title", "Click to hide/show")
        img.click(function() {
            if (img.data("src")) {
                img.attr("src", img.data("src"))
                img.removeData("src")
            } else {
                img.attr("src", "assets/images/icons/cross-grey.png")
                img.data("src", src)
            }
        })
        //img.attr("src", "assets/images/emoji/banana.png")
        img.on('load', function() {
            adjustScroll()
            })
        input.html(prefix)
        input.append(img)
    }
}
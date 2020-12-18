var lastMessageId = -1
var firstMessageId = -1

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
var ws

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
    ws = new ReconnectingWebSocket(function() {
        return wsUri + "?reconnect=true&lastMessageId=" + lastMessageId
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
      adjustScroll(true)
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

    $('#chat-box .chat-history.tab-content div.loading').remove()

	if (r_msg.type === 'newSession') { //session join message
	    clearMessages(chat, $scope)
        for (i = 0; i < r_msg.messages.length ; i ++) {
            pushMessage(r_msg.messages[i], chat, $scope)
        }

        lastMessageId = r_msg.lastMessageId
        if (r_msg.messages.length > 0) {
            firstMessageId = r_msg.messages[0].id
        }
        if ($('.chat').is(':hidden')) {
            if (r_msg.unreadMessageCount > 0) {
                $('.notify-bubble').show(400);
                $('.notify-bubble').text(r_msg.unreadMessageCount);
            }
        }
        adjustScroll()
    } else if (r_msg.type === 'previous') { //scroll up
        for (i = r_msg.messages.length - 1; i >= 0 ; i --) { //prepend from latest to oldest
            var message = r_msg.messages[i]
            if (message.id < firstMessageId) { //prevent duplicate calls
                prependMessage(message, chat, $scope)
                firstMessageId = message.id
            }
        }
    } else { //incoming message from broadcast
        var $activeHistory = $("#chat-box .chat-history.current")
        var atScrollBottom = ($activeHistory[0].scrollHeight - $activeHistory[0].scrollTop === $activeHistory[0].clientHeight)

        pushMessage(r_msg, chat, $scope)
        lastMessageId = r_msg.id
        if ($('.chat').is(':hidden')) {
            $('.notify-bubble').show(400);
            $('.notify-bubble').text(parseInt($('.notify-bubble').text())+1);
        }
        if (atScrollBottom) {
            adjustScroll()
        }
    }



    //ACK if chat is active
    if ($("#live-chat h4").is(":visible") && r_msg.latest) {
        ackChatId()
    }

  };


  $(".chat-history").each(function() {
      $(this).scroll(function() {
        if ($(this).scrollTop()  <= 0 ){
           var $loadingDiv = $("<div class='loading'><img src='https://i.stack.imgur.com/FhHRx.gif'></div>")
           $('#chat-box .chat-history.tab-content').prepend($loadingDiv)
           //scrolled to top
           var text = { airlineId: activeAirline.id, firstMessageId : firstMessageId, type: "previous" };
                     // send it to the server through websockets
           ws.send(JSON.stringify(text));
        }
      })
  })
});

const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
  "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
];


function buildPrefix(r_msg) {
    var date = new Date(r_msg.timestamp)
    var airlineName = r_msg.airlineName
    var userLevel = r_msg.userLevels
    var monthString = monthNames[date.getMonth()]
    var dateString = date.getDate()
    var hourString = date.getHours()
    var minuteString = date.getMinutes()

    if (hourString < 10) {
        hourString = "0" + hourString
    }
    if (minuteString < 10) {
        minuteString = "0" + minuteString
    }

    var dateString = monthString + " " + dateString + " " + hourString + ":" + minuteString
//	var airlineSpan = $("<span>" + airlineName + "</span>")
//	var userIcon = getUserLevelImg(userLevel)
//	airlineSpan.append(userIcon)

    var prefix = "[" + dateString + "] " + airlineName + ": "
    return prefix
}

function prependMessage(r_msg, chat, $scope) {
    var prefix = buildPrefix(r_msg)
    if (!r_msg.allianceRoomId) {
        chat.gmessages.unshift(prefix + r_msg.text);
    } else {
        chat.amessages.unshift(prefix + r_msg.text);
    }

    $scope.$digest();

    var isMobileDeviceValue = isMobileDevice()
    $('.chat-history').each (function(){
        emojify.run($(this).find("li:first-child")[0]);   // translate emoji to images
        if (r_msg.imagePermission && !isMobileDeviceValue) {
            replaceImg($(this).find("li:first-child"), prefix)
        }
    })
}

function clearMessages(chat, $scope) {
    chat.gmessages.length = 0
    chat.amessages.length = 0
    $scope.$digest();
}

function pushMessage(r_msg, chat, $scope) {
    var prefix = buildPrefix(r_msg)
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
}

function ackChatId() {
      if (activeAirline && lastMessageId > -1) {
          var text = { airlineId: activeAirline.id, ackId : lastMessageId, type : "ack" };
          // send it to the server through websockets
          ws.send(JSON.stringify(text));
      }
  }



emojify.setConfig({img_dir : 'assets/images/emoji'});

function adjustScroll() {
    //if (!$('#scroll_lockc').is(":checked")) {

    $(".chat-history").each(function() {
        $(this).scrollTop($(this).prop("scrollHeight"))
    })

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
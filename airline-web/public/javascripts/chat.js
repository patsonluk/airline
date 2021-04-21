var lastMessageId = -1
var firstAllianceMessageId = -1
var firstGeneralMessageId = -1

function updateChatTabs() {
	if (activeUser.allianceName && activeUser.allianceRole != 'APPLICANT') {
		$("#allianceChatTab").text(activeUser.allianceName)
		$("#allianceChatTab").data('roomId', activeUser.allianceId)
		$("#allianceChatTab").show()
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
  chat.username = "";

  // what happens when user enters message
  chat.sendMessage = function(event) {
      if (isFromEmoji) {
        isFromEmoji = false;
        return;
      }
      limit.tick('myevent_id');
      adjustScroll()
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
            $('#chat-box li.tab-link.current ul').append('<li class="message">Message Not Sent : Rate Filter or Invalid Message</li>')
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
            $('#chat-box #chatBox-1 ul').append('<li class="status">Chat Connected</li>')
		});
	    $timeout(function(){ 
			var scroller = document.getElementById("chatBox-1");
		    scroller.scrollTop = scroller.scrollHeight;
		});
   };
   
   ws.onclose = function () {
	   $("#live-chat i").css({"background-image":"url(\"../../assets/images/icons/32px/balloon-chat-red.png\")"});
	   $timeout(function(){ 
			            $('#chat-box #chatBox-1 ul').append('<li class="status">Chat Disconnected</li>')
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
    var $activeHistory = $("#chat-box .chat-history.current")

	if (r_msg.type === 'newSession') { //session join message
	    $('#chat-box .chat-history.tab-content ul li.message').remove()
        for (i = 0; i < r_msg.messages.length ; i ++) {
            pushMessage(r_msg.messages[i])
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

            prependMessage(message)
        }
        if (r_msg.messages.length == 0) {
            $activeHistory.find('ul').prepend('<li class="message"><b>No more previous messages</b></li>')
            $activeHistory.data("historyExhausted", true)
        }

    } else { //incoming message from broadcast
        var atScrollBottom = ($activeHistory[0].scrollHeight - $activeHistory[0].scrollTop === $activeHistory[0].clientHeight)

        pushMessage(r_msg)
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
    $(this).on('touchstart', function(e) {
        var swipe = e.originalEvent.touches,
        start = swipe[0].pageY;
        $(this).on('touchmove', function(e) {
            var contact = e.originalEvent.touches,
            end = contact[0].pageY,
            distance = end-start;
            if (distance > 30  && $(this).scrollTop()  <= 0) {
                handleScrollChatTop()
            }
        })
        .one('touchend', function() {
            $(this).off('touchmove touchend');
        });
    });

      $(this).bind('wheel', function(event) {
        if (event.originalEvent.deltaY < 0 && $(this).scrollTop()  <= 0) { //scroll up and at the top
            handleScrollChatTop()
        }
      });
  })
});

function handleScrollChatTop() {
  var $activeHistory = $("#chat-box .chat-history.current")
  if ($activeHistory.data('historyExhausted') == true) { //exhausted all previous messages
    return;
  }


  $chatTab = $('#chat-box .chat-history.tab-content')
  //$(this).css('overflow', 'hidden')

  if (!$chatTab.find('.loading').length){ //scrolled to top and not already loading
     var $loadingDiv = $("<div class='loading'><img src='https://i.stack.imgur.com/FhHRx.gif'></div>")
     $chatTab.prepend($loadingDiv)
     //scrolled to top
     var activeRoomId = parseInt($('#live-chat .tab-link.current').data('roomId'))
     var firstMessageId = (activeRoomId == 0) ? firstGeneralMessageId : firstAllianceMessageId

     var text = { airlineId: activeAirline.id, firstMessageId : firstMessageId, type: "previous", roomId : activeRoomId};
               // send it to the server through websockets
     ws.send(JSON.stringify(text));
  }

}

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

function prependMessage(r_msg) {
    var prefix = buildPrefix(r_msg)
    if (!r_msg.allianceRoomId) {
        if (r_msg.id < firstGeneralMessageId) { //prevent duplicate calls
            $('#chat-box #chatBox-1 ul').prepend('<li class="message">' + prefix + r_msg.text + '</li>')

            if (firstGeneralMessageId == -1 || r_msg.id < firstGeneralMessageId) {
                firstGeneralMessageId = r_msg.id
            }
        }
    } else {
        if (r_msg.id < firstAllianceMessageId) { //prevent duplicate calls
            $('#chat-box #chatBox-2 ul').prepend('<li class="message">' + prefix + r_msg.text + '</li>')
            if (firstAllianceMessageId == -1 || r_msg.id < firstAllianceMessageId) {
                firstAllianceMessageId = r_msg.id
            }
        }
    }



    var isMobileDeviceValue = isMobileDevice()
    $('.chat-history').each (function(){
        emojify.run($(this).find("li:first-child")[0]);   // translate emoji to images
        if (r_msg.imagePermission && !isMobileDeviceValue) {
            replaceImg($(this).find("li:first-child"), prefix, false)
        }
    })


}


function pushMessage(r_msg) {
    var $activeHistory = $("#chat-box .chat-history.current")
    var atScrollBottom = ($activeHistory[0].scrollHeight - $activeHistory[0].scrollTop === $activeHistory[0].clientHeight)
    var prefix = buildPrefix(r_msg)
    if (!r_msg.allianceRoomId) {
        $('#chat-box #chatBox-1 ul').append('<li class="message">' + prefix + r_msg.text + '</li>')
        if (firstGeneralMessageId == -1 || r_msg.id < firstGeneralMessageId) {
            firstGeneralMessageId = r_msg.id
        }
    } else {
        $('#chat-box #chatBox-2 ul').append('<li class="message">' + prefix + r_msg.text + '</li>')
        if (firstAllianceMessageId == -1 || r_msg.id < firstAllianceMessageId) {
            firstAllianceMessageId = r_msg.id
        }
    }



    var isMobileDeviceValue = isMobileDevice()
    $('.chat-history').each (function(){
        emojify.run($(this).find("li:last-child")[0]);   // translate emoji to images
        if (r_msg.imagePermission && !isMobileDeviceValue) {
            replaceImg($(this).find("li:last-child"), prefix, atScrollBottom)
        }
    })

    if (r_msg.id > lastMessageId) {
        lastMessageId = r_msg.id
    }
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
        setTimeout(100, $(this).scrollTop($(this).prop("scrollHeight")))
    })

}

var imgTag = "/img"
function replaceImg(input, prefix, scrollToBottom) {
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

        if (scrollToBottom) {
            img.on('load', function() {
                adjustScroll()
                })
        }
        input.html(prefix)
        input.append(img)
    }
}
$('#live-chat header span i').on('click', function() {
	$('.chat').slideToggle(150, 'swing', function(){
            //What to do on toggle compelte...
			var final_state = $(this).is(':hidden');
			if (final_state) {
				//Collapsed
				$("#live-chat").css({"left":"10px", "width":"59px"});
				//$("#live-chat h4").text("");
				$("#live-chat h4").hide();
				$(".chatOption").hide();
			} else {
				//Expanded
				$("#live-chat").css({"left":"10px", "width":"500px", "max-width": "95%"});
				//$("#live-chat h4").text("Game Chat");
				$("#live-chat h4").show();
				$(".chatOption").show();
				
				$('.notify-bubble').hide();
				$('.notify-bubble').text("0");
				var scroller = document.getElementById("chatBox-1");
				scroller.scrollTop = scroller.scrollHeight;
				var scroller = document.getElementById("chatBox-2");
				scroller.scrollTop = scroller.scrollHeight;

                ackChatId();
			}
        });
});

$('ul.ctabs li').click(function(){
		var tab_id = $(this).attr('data-tab');

		$('ul.ctabs li').removeClass('current');
		$('.tab-content').removeClass('current');

		$(this).addClass('current');
		$("#"+tab_id).addClass('current');
		var scroller = document.getElementById("chatBox-1");
		scroller.scrollTop = scroller.scrollHeight;
		var scroller = document.getElementById("chatBox-2");
		scroller.scrollTop = scroller.scrollHeight;
});


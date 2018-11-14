$('#live-chat header').on('click', function() {
	$('.chat').slideToggle(150, 'swing', function(){
            //What to do on toggle compelte...
			var final_state = $(this).is(':hidden');
			if (final_state) {
				//Collapsed
				$("#live-chat").css({"left":"10px", "width":"60px"});
				$("#live-chat h4").text("");
			} else {
				//Expanded
				$("#live-chat").css({"left":"24px", "width":"500px"});
				$("#live-chat h4").text("Game Chat");
				$('.notify-bubble').hide();
				$('.notify-bubble').text("0");
			}
        });
});

//$( document ).ready(function() {
//	$.ajax({
//		type: 'GET',
//		url: "signup/profiles",
//	    contentType: 'application/json; charset=utf-8',
//	    dataType: 'json',
//	    success: function(profiles) {
//	    	$.each(profiles, function(index, profile) {
//	    		showProfile(profile)
//	  		});
//	    	//updateModelInfo($('#modelInfo'))
//	    },
//        error: function(jqXHR, textStatus, errorThrown) {
//	            console.log(JSON.stringify(jqXHR));
//	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
//	    }
//	});
//})

//function showProfile(profile) {
//	var html = '<div style="float:left;" class="profile-section verticalGroup clickable" onclick="selectProfile(' + profile.id + ', this)"><h1>' + profile.title +'</h1><br/><span class="label">' + profile.description + '</span><br/><br/><ul>'
//	$.each(profile.outlines, function(index, outline) {
//		html += '<li>' + outline + '</li>'
//	})
//	html += '</ul></div>'
//
//	var profileDiv = $(html).appendTo('#profiles')
//	if ($('#profileId').val() == profile.id) {
//		selectProfile(profile.id, profileDiv)
//	}
//}
//
//function selectProfile(profileId, profileDiv) {
//	$('#profileId').val(profileId)
//	$(profileDiv).siblings("div").removeClass("selected")
//	$(profileDiv).addClass("selected")
//}


function signup(form) {
	grecaptcha.ready(function() {
		grecaptcha.execute('6LespV8UAAAAAJkCUpR8_uNC3P-wZGq7vnTNKEZe', {action: 'signup'})
			.then(function(token) {
				form.append('<input type="hidden" name="recaptchaToken" value="' + token + '" />');
				form.submit()
			});
		});
}

	
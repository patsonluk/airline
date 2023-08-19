$( document ).ready(function() {
    $('input#airlineName').on('input', function() {
        var airlineName = $(this).val()
        $.ajax({
    		type: 'GET',
    		url: "signup/airline-name-check?airlineName=" + airlineName,
    	    contentType: 'application/json; charset=utf-8',
    	    dataType: 'json',
    	    success: function(result) {
    	    	if (result.ok) {
                    $('.airlineName dd.error').text('')
    	    	} else {
    	    	    $('.airlineName dd.error').text(result.rejection)
    	    	}
    	    },
            error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    	});
    })
})

function signup(form) {
	// grecaptcha.ready(function() {
	// 	grecaptcha.execute('6LefN0MnAAAAAM9UF95gET_FzuosiVBJP1gCk3GK-', {action: 'signup'})
	// 		.then(function(token) {
	// 		    $('body .loadingSpinner').show()
    //          	form.append('<input type="hidden" name="recaptchaToken" value="' + token + '" />');
	// 			form.submit()
	// 		});
	// 	});
	form.append('<input type="hidden" name="recaptchaToken" value="recaptchaToken" />');
	form.submit()
}

	

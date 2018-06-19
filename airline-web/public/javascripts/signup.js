function signup(form) {
	grecaptcha.ready(function() {
		grecaptcha.execute('6LespV8UAAAAAJkCUpR8_uNC3P-wZGq7vnTNKEZe', {action: 'signup'})
			.then(function(token) {
				form.append('<input type="hidden" name="recaptchaToken" value="' + token + '" />');
				form.submit()
			});
		});
}

	
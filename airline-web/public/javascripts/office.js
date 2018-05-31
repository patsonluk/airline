
function showOfficeCanvas() {
	setActiveDiv($("#officeCanvas"))
	highlightTab($('#officeCanvasTab'))
	
	loadBalanceSheet();
}

function loadBalanceSheet() {
//	var getUrl = "countries"
//	$.ajax({
//		type: 'GET',
//		url: getUrl,
//	    contentType: 'application/json; charset=utf-8',
//	    dataType: 'json',
//	    success: function(countries) {
//	    	$.each(countries, function(index, country) {
//	    		loadedCountriesByCode[country.countryCode] = country
//	    	});
//	    	
//	    	loadedCountries = Object.values(loadedCountriesByCode);
//	    },
//	    error: function(jqXHR, textStatus, errorThrown) {
//	            console.log(JSON.stringify(jqXHR));
//	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
//	    }
//	});
}


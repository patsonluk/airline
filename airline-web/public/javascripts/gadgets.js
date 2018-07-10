var noFlags = ["BL", "CW", "IM", "GG", "JE", "BQ", "MF", "SS", "SX", "XK"]

function generateImageBar(imageEmpty, imageFill, count, containerDiv, valueInput, indexToValueFunction, valueToIndexFunction, callback) {
	containerDiv.empty()
	var images = []
	
	if (!indexToValueFunction || !valueToIndexFunction) {
		indexToValueFunction = function(index) { return index + 1 }
		valueToIndexFunction = function(value) { return value - 1 }
	}
	
	if (valueInput.val()) { //validate the input, set the value to boundaries allowed by this bar
		if (valueToIndexFunction(valueInput.val()) < 0) {
			valueInput.val(indexToValueFunction(0))
		} else if (valueToIndexFunction(valueInput.val()) >= count) {
			valueInput.val(indexToValueFunction(count - 1))
		}
	}
	

	for (i = 0 ; i < count ; i ++) {
		var image = $("<img class='button'>")
		image.attr("src", imageEmpty)
		//image.click({index : i}, updateImageBar)

		image.data('index', i)
		image.click(updateImageBar)
		//image.click(function () { valueInput.val($(this).data('index')); })
		image.click(function () {
			var newValue = indexToValueFunction($(this).data('index'))
			var oldValue = parseInt(valueInput.val())
			valueInput.val(newValue); 
			if (callback) {
				callback(oldValue, newValue)
			}
		})
		image.hover(updateImageBar, resetImageBar)

		containerDiv.append(image)
		images.push(image)
		
		if ((i + 1) % 10 == 0) {
			containerDiv.append("<br/>")
		}
	}
	
	if (valueInput.val()) {
		//updateImageBarBySelectedIndex(valueInput.val())
		updateImageBarBySelectedIndex(valueToIndexFunction(valueInput.val()))
	}
	
	function updateImageBar(event) {
		var index = $(this).data('index')
		updateImageBarBySelectedIndex(index)
	}
	function resetImageBar() {
		if (valueInput.val()) {
		  //var index = valueInput.val()
	      var index = valueToIndexFunction(valueInput.val())
		  updateImageBarBySelectedIndex(index)
		}
	}
	function updateImageBarBySelectedIndex(index) {
		for (j = 0 ; j < count; j++) {
			if (j <= index) {
				images[j].attr("src", imageFill)
			} else {
				images[j].attr("src", imageEmpty)
			}
		}
	}
}

function shimmeringDiv(div) {
	var originalBackgroundColor = div.css("backgroundColor")
	var originalColor = div.css("color")
	div.animate( { backgroundColor:'#EDFBFF', color: '#6093e7' }, 1000, function() {
		div.animate({backgroundColor: originalBackgroundColor, color : originalColor }, 1000)
		//div.toggle( "bounce", { times: 3 }, "slow" )
	})
	setTimeout(function() { shimmeringDiv(div) }, 5000)
}

function fadeOutMarker(marker) {
	marker.opacities = [0.8, 0.6, 0.4, 0.2, 0]
	fadeOutMarkerRecursive(marker)
}
function fadeOutMarkerRecursive(marker) {
	if (marker.opacities.length > 0) {
    	marker.setOpacity(marker.opacities[0])
    	marker.opacities.shift()
    	var icon = marker.getIcon()
    	icon.anchor = new google.maps.Point(icon.anchor.x, icon.anchor.y + 2),
    	setTimeout(function() { fadeOutMarkerRecursive(marker) }, 50)
	} else {
		marker.setMap(null)
	}
}

function fadeInMarker(marker) {
	marker.opacities = [0.2, 0.4, 0.6, 0.8, 1.0]
	fadeInMarkerRecursive(marker)
}
function fadeInMarkerRecursive(marker) {
	if (marker.opacities.length > 0) {
		marker.setOpacity(marker.opacities[0])
    	marker.opacities.shift()
    	setTimeout(function() { fadeInMarkerRecursive(marker) }, 20)
	}
}

function toLinkClassValueString(linkValues, prefix, suffix) {
	if (!prefix) {
		prefix = ""
	}
	if (!suffix) {
		suffix = ""
	}
 	return prefix + linkValues.economy + suffix + " / " + prefix + linkValues.business + suffix + " / " + prefix + linkValues.first + suffix
}

function changeColoredElementValue(element, newValue) {
	var oldValue = element.data('numericValue')
	if ($.isNumeric(newValue)) {
		element.data('numericValue', parseFloat(newValue))
	}
	
	if (!element.is(':animated') && $.isNumeric(oldValue) && $.isNumeric(newValue)) { //only do coloring for numeric values
		var originalColor = element.css("color")
		var originalBackgroundColor = element.css("background-color")
		
		if (parseFloat(oldValue) < parseFloat(newValue)) {
			element.animate({"background-color" : "#A1D490", "color" : "#248F00"}, 1000, function() {
				element.animate({backgroundColor: originalBackgroundColor, color : originalColor }, 2000)
			})
		} else if (parseFloat(oldValue) > parseFloat(newValue)) {
			element.animate({"background-color" : "#F7B6A1", "color" : "#FA7246"}, 1000, function() {
				element.animate({backgroundColor: originalBackgroundColor, color : originalColor }, 2000)
			})
		} else {
			element.animate({"background-color" : "#FFFC9E", "color" : "#FFDD00"}, 1000, function() {
				element.animate({backgroundColor: originalBackgroundColor, color : originalColor }, 2000)
			})
		}
	}
	if ($.isNumeric(newValue)) {
		element.text(commaSeparateNumber(newValue))
	} else {
		element.text(newValue)
	}
}

function commaSeparateNumber(val){
    while (/(\d+)(\d{3})/.test(val.toString())){
      val = val.toString().replace(/(\d+)(\d{3})/, '$1'+','+'$2');
    }
    return val;
}

function getCountryFlagImg(countryCode) {
	var countryFlagUrl = getCountryFlagUrl(countryCode);
	var countryName = loadedCountriesByCode[countryCode].name
	if (countryFlagUrl) {
		return "<img src='" + countryFlagUrl + "' title='" + countryName +"'/>"
	} else {
		return ""
	}
}

function getCountryFlagUrl(countryCode) {
	if (!countryCode) {
		return '';
	} if ($.inArray(countryCode, noFlags) != -1) {
		return '';
	} else {
		return "assets/images/flags/" + countryCode + ".png"
	}
}

function getDurationText(duration) {
	var hour = Math.floor(duration / 60)
	var minute = duration % 60
	if (hour > 0) {
		return hour + " h " + minute + "m"
	} else {
		return minute + " m"
	}
}

function getOpennessSpan(openness) {
	var description
	var icon
	if (openness >= 8) {
		description = "Opened Market"
		icon = "globe--plus.png"
	} else if (openness >= 6) {
		description = "No International Connection"
		icon = "globe--exclamation.png"
	} else if (openness >= 4) { 
		description = "No Foreign Airline Base"
		icon = "globe--minus.png"
	} else {
		description = "No Foreign Airline"
		icon = "prohibition.png"
	}
	return "<span>" + description + "(" + openness + ")&nbsp;<img src='assets/images/icons/" + icon + "'/></span>"
	
}

function sortPreserveOrder(array, property, ascending) {
	if (ascending == undefined) {
		ascending = true
	}
    var sortOrder = 1;
    
    if(!ascending) {
        sortOrder = -1;
    }
    
	var sortArray = array.map(function(data, idx){
	    return {idx:idx, data:data}
	})

	sortArray.sort(function(a, b) {
		var aVal = a.data[property]
    	var bVal = b.data[property]
    	if (Array.isArray(aVal) && Array.isArray(bVal)) {
    		aVal = aVal.length
    		bVal = bVal.length
    	}
    	var result = (aVal < bVal) ? -1 : (aVal > bVal) ? 1 : 0;
    	if (result == 0) {
    		return a.idx - b.idx
    	} else {
    		return result * sortOrder;
    	}
	});

	var result = sortArray.map(function(val){
	    return val.data
	});
	
	console.log(result)
	
	return result;
}

function sortByProperty(property, ascending) {
	if (ascending == undefined) {
		ascending = true
	}
    var sortOrder = 1;
    
    if(!ascending) {
        sortOrder = -1;
    }
    
    return function (a,b) {
    	var aVal = a[property]
    	var bVal = b[property]
    	if (Array.isArray(aVal) && Array.isArray(bVal)) {
    		aVal = aVal.length
    		bVal = bVal.length
    	}
    	var result = (aVal < bVal) ? -1 : (aVal > bVal) ? 1 : 0;
        return result * sortOrder;
    }
}

function padAfter(str, padChar, max) {
    str = str.toString();
	return str.length < max ? padAfter(str + padChar, padChar, max) : str;
}
function padBefore(str, padChar, max) {
	str = str.toString();
	return str.length < max ? padBefore(padChar + str, padChar, max) : str;
}

function getAirportText(city, airportName) {
	if (city) {
		return city + "(" + airportName + ")"
	} else {
		return airportName
	}
}

function setActiveDiv(activeDiv) {
	var existingActiveDiv = activeDiv.siblings(":visible").filter(function (index) {
		return $(this).css("clear") != "both"
	})
	if (existingActiveDiv.length > 0){
		existingActiveDiv.fadeOut(200, function() { activeDiv.fadeIn(200) })
	} else {
		if (activeDiv.is(":visible")) { //do nothing. selecting the same div as before
			return false;
		} else {
			activeDiv.siblings().hide();
			activeDiv.fadeIn(200)
		}
	}
	
	activeDiv.parent().show()
	return true;
}

function hideActiveDiv(activeDiv) {
	if (activeDiv.is(":visible")){
		activeDiv.fadeOut(200)
		activeDiv.parent().hide()
	}
}

function toggleOnOff(element) {
	if (element.is(":visible")){
		element.fadeOut(200)
	} else {
		element.fadeIn(200)
	}
}

/**
 * Performs UI change to highlighting a tab (and unhighlighting others) 
 * @param tab
 * @returns
 */
function highlightTab(tab) {
	tab.siblings().children("span").removeClass("selected")
	//highlight the selected model
	tab.children("span").addClass("selected")
}

function highlightSwitch(selectedSwitch) {
	selectedSwitch.siblings().removeClass("selected")
	selectedSwitch.addClass("selected")
}

function closeModal(modal) {
	modal.fadeOut(200)
}
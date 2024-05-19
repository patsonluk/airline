var noFlags = ["BL", "GG", "JE", "BQ", "MF", "SS"]

function htmlEncode(str){
  return String(str).replace(/[^\w. ]/gi, function(c){
     return '&#'+c.charCodeAt(0)+';';
  });
}

function generateSimpleImageBar(imageSrc, count) {
    var containerDiv = $("<div>")

    for (i = 0 ; i < count ; i ++) {
		var image = $("<img src='" + imageSrc + "'>")
		containerDiv.append(image)
    }

    return containerDiv
}

function generateImageBar(imageEmpty, imageFill, count, containerDiv, valueInput, indexToValueFunction, valueToIndexFunction, callback) {
    generateImageBarWithRowSize(imageEmpty, imageFill, count, containerDiv, valueInput, indexToValueFunction, valueToIndexFunction, 10, callback)
}

function generateImageBarWithRowSize(imageEmpty, imageFill, count, containerDiv, valueInput, indexToValueFunction, valueToIndexFunction, rowSize, callback) {
	containerDiv.empty()
	var images = []
	
	if (!indexToValueFunction || !valueToIndexFunction) {
		indexToValueFunction = function(index) { return index + 1 }
		valueToIndexFunction = function(value) { return value - 1 }
	}

	if (valueInput.val()) { //validate the input, set the value to boundaries allowed by this bar
		if (valueToIndexFunction(valueInput.val()) < 0) { //-1 is still valid, that means none selected
			valueInput.val(indexToValueFunction(-1))
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
		
		if ((i + 1) % rowSize == 0) {
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

//function fadeOutMarker(marker) {
//	marker.opacities = [0.8, 0.6, 0.4, 0.2, 0]
//	fadeOutMarkerRecursive(marker)
//}
//function fadeOutMarkerRecursive(marker) {
//	if (marker.opacities.length > 0) {
//    	marker.setOpacity(marker.opacities[0])
//    	marker.opacities.shift()
//    	var icon = marker.getIcon()
//    	icon.anchor = new google.maps.Point(icon.anchor.x, icon.anchor.y + 2),
//    	setTimeout(function() { fadeOutMarkerRecursive(marker) }, 50)
//	} else {
//		marker.setMap(null)
//	}
//}

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
	var economyValue = linkValues.hasOwnProperty('economy') ? linkValues.economy : '-'
	var businessValue = linkValues.hasOwnProperty('business') ? linkValues.business : '-'
	var firstValue = linkValues.hasOwnProperty('first') ? linkValues.first : '-'


	if (economyValue >= 1_000_000 || businessValue >= 1_000_000 || firstValue >= 1_000_000) {
 	    return prefix + commaSeparateNumber(economyValue) + suffix + " / " + prefix + commaSeparateNumber(businessValue) + suffix + " / " + prefix + commaSeparateNumber(firstValue) + suffix
    } else {
        return prefix + economyValue + suffix + " / " + prefix + businessValue + suffix + " / " + prefix + firstValue + suffix
    }
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
				element.animate({backgroundColor: originalBackgroundColor, color : originalColor }, 2000, function() {
                    element.css({backgroundColor : "", color : ""})
                })
			})
		} else if (parseFloat(oldValue) > parseFloat(newValue)) {
			element.animate({"background-color" : "#F7B6A1", "color" : "#FA7246"}, 1000, function() {
				element.animate({backgroundColor: originalBackgroundColor, color : originalColor }, 2000, function() {
                    element.css({backgroundColor : "", color : ""})
                })
			})
		} else {
			element.animate({"background-color" : "#FFFC9E", "color" : "#FFDD00"}, 1000, function() {
				element.animate({backgroundColor: originalBackgroundColor, color : originalColor }, 2000, function() {
                    element.css({backgroundColor : "", color : ""})
                })
			})
		}
	}
	if ($.isNumeric(newValue)) {
		element.text(commaSeparateNumber(newValue))
		if (newValue < 0) {
		    element.addClass('negative')
		} else {
		    element.removeClass('negative')
		}
	} else {
		element.text(newValue)
	}
}

function commaSeparateNumber(val){
	var isNegative = val < 0
	val = Math.abs(val)
    while (/(\d+)(\d{3})/.test(val.toString())){
      val = val.toString().replace(/(\d+)(\d{3})/, '$1'+','+'$2');
    }
    return isNegative ? ('(' + val + ')') : val;
}

function getCountryFlagImg(countryCode) {
	if (countryCode) {
		var countryFlagUrl = getCountryFlagUrl(countryCode);
		var countryName = loadedCountriesByCode[countryCode].name
		if (countryFlagUrl) {
			return "<img class='flag' src='" + countryFlagUrl + "' title='" + countryName +"' style='border-radius:0;'/>"
		} else {
			return ""
		}
	} else {
		return "";
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

function getAirlineLogoImg(airlineId) {
	return "<img class='logo' loading='lazy' width='24px' height='12px' src='" + "/airlines/" + airlineId + "/logo' style='vertical-align:middle;'/>"
}


function getAirlineLabelSpan(airlineId, airlineName) {
    var $airlineLabelSpan = $('<span>' + airlineName + '</span>')
	if (airlineLabelColors[airlineId]) {
	    $airlineLabelSpan.css('color', '#' + airlineLabelColors[airlineId])
	}
	return $airlineLabelSpan[0].outerHTML
}

function getAirlineSpan(airlineId, airlineName) {
    var $airlineSpan = $('<span></span>')
	$airlineSpan.append(getAirlineLogoImg(airlineId))
	$airlineSpan.append(getAirlineLabelSpan(airlineId, airlineName))

	return $airlineSpan[0].outerHTML
}

function getAirlineLogoSpan(airlineId, airlineName) {
    var $airlineLogoSpan = $('<span></span>')
	$airlineLogoSpan.append(getAirlineLogoImg(airlineId))
	$airlineLogoSpan.attr("title", airlineName)
    return $airlineLogoSpan
}

function getUserLevelImg(level) {
	if (level <= 0) {
		return ""
	} 
	var levelTitle
	var levelIcon
	if (level == 1) {
		levelIcon = "assets/images/icons/medal-bronze-premium.png"
		levelTitle = "Patreon : Bronze"
	} else if (level == 2) {
		levelIcon = "assets/images/icons/medal-silver-premium.png"
		levelTitle = "Patreon : Silver"
	} else if (level == 3) {
		levelIcon = "assets/images/icons/medal-red-premium.png"
		levelTitle = "Patreon : Gold"
	}
	
	if (levelIcon) {
		return "<img src='" + levelIcon + "' title='" + levelTitle + "' style='vertical-align:middle;'/>"
	} else {
		return ""
	} 
}

function getAdminImg(adminStatus) {
	if (!adminStatus) {
		return ""
	}

	var	levelIcon = "assets/images/icons/star.png"
    var levelTitle = "Game Admin"

	if (levelIcon) {
		return "<img src='" + levelIcon + "' title='" + levelTitle + "' style='vertical-align:middle;'/>"
	} else {
		return ""
	}
}

function getUserModifiersSpan(modifiers) {
	if (!modifiers) {
		return ""
	}

    var result = ""
    $.each(modifiers, function(index, modifier) {
        if (modifier == "WARNED") {
           result += "<span><img src='assets/images/icons/exclamation.png' title='" + modifier + "' style='vertical-align:middle;'/></span>"
        } else if (modifier == "CHAT_BANNED") {
           result += "<span><img src='assets/images/icons/mute.png' title='" + modifier + "' style='vertical-align:middle;'/></span>"
        } else if (modifier == "BANNED") {
           result += "<span><img src='assets/images/icons/prohibition.png' title='" + modifier + "' style='vertical-align:middle;'/></span>"
        } else {
           result += "<span>" + modifier + "</span>"
        }
    })
    return result
}

function getAirlineModifiersSpan(modifiers) {
	if (!modifiers) {
		return ""
	}

    var result = ""
    $.each(modifiers, function(index, modifier) {
        if (modifier == "NERFED") {
           result += "<span><img src='assets/images/icons/ghost.png' title='" + modifier + "' style='vertical-align:middle;'/></span>"
        } else if (modifier == "BANNER_LOYALTY_BOOST") {
            result += "<span><img src='assets/images/icons/megaphone.png' title='Banner contest winner!' style='vertical-align:middle;'/></span>"
        }
//        } else { //let's no show modifiers that are not listed for now. since they could be common
//           result += "<span>" + modifier + "</span>"
    })
    return result
}

function getRankingImg(ranking) {
	var rankingIcon
	var rankingTitle
	if (ranking == 1) {
		rankingIcon = "assets/images/icons/crown.png"
		rankingTitle = "1st place"
	} else if (ranking == 2) {
		rankingIcon = "assets/images/icons/crown-silver.png"
		rankingTitle = "2nd place"
	} else if (ranking == 3) {
		rankingIcon = "assets/images/icons/crown-bronze.png"
    	rankingTitle = "3rd place"
	} else if (ranking <= 10) {
		rankingIcon = "assets/images/icons/trophy-" + ranking + ".png"
		rankingTitle = ranking + "th place"
	} else if (ranking <= 20) {
		rankingIcon = "assets/images/icons/counter-" + ranking + ".png"
        rankingTitle = ranking + "th place"
	}
	
	if (rankingIcon) {
		return "<img src='" + rankingIcon + "' title='" + rankingTitle + "' style='vertical-align:middle;'/>"
	} else {
		return "<span>" + ranking + "</span>"
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

function getYearMonthText(weekDuration) {
	var year = Math.floor(weekDuration / 52)
	var month = Math.floor(weekDuration / 4) % 12
	if (year > 0) {
		return year + " year(s) " + month + " month(s)"
	} else {
		return month + " month(s)"
	}
}


function getOpennessSpan(openness) {
	var description
	var icon
	if (openness >= 7) {
		description = "Opened Market"
		icon = "globe--plus.png"
	} else {
		description = "No International Connection"
		icon = "globe--exclamation.png"
	}/* else if (openness >= 2) {
		description = "No Foreign Airline Base"
		icon = "globe--minus.png"
	} else {
		description = "No Foreign Airline"
		icon = "prohibition.png"
	}*/
	return "<span>" + description + "(" + openness + ")&nbsp;<img src='assets/images/icons/" + icon + "'/></span>"
	
}

function scrollToRow($matchingRow, $container) {
    var row = $matchingRow[0]
    var baseOffset = $container.find(".table-row")[0].offsetTop //somehow first row is not 0...
    var realOffset = row.offsetTop - baseOffset
    $container.stop(true, true) //stop previous animation
    $container.animate ({scrollTop: realOffset}, "fast");
}

/*
Get a span with value and a tool tip of breakdown
*/
function getBoostSpan(finalValue, boosts, $tooltip) {
    var $valueSpan = $('<span>' + commaSeparateNumber(finalValue) + '</span>')
    if (boosts) {
        $valueSpan.css('color', '#41A14D')
        $tooltip.find('.table .table-row').remove()
        var $table = $tooltip.find('.table')
        $.each(boosts, function(index, entry) {
            var $row = $('<div class="table-row"><div class="cell" style="width: 70%;">' + entry.description + '</div><div class="cell" style="width: 30%; text-align:right;">+' + commaSeparateNumber(entry.boost) + '</div></div>')
            $row.css('color', 'white')
            $table.append($row)
        })

        $valueSpan.hover( function() {
             var yPos = $(this).offset().top - $(window).scrollTop() + $(this).height()
             var xPos = $(this).offset().left - $(window).scrollLeft() + $(this).width() - $tooltip.width() / 2

             $tooltip.css('top', yPos + 'px')
             $tooltip.css('left', xPos + 'px')
             $tooltip.show()
        }, function() { $tooltip.hide() }
        )
    }
    return $valueSpan
}

//clone from template if not exists, assign with new id; otherwise return the previously created object
function createIfNotExist($template, id) {
    var $target = $('#' + id)
    if ($target.length) {
        return $target
    }
    $target = $template.clone()
    $target.prop('id', id)
    $template.parent().append($target) //has to attach it somewhere

    return $target
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

function getAirportText(city, airportCode) {
	if (city) {
		return city + "(" + airportCode + ")"
	} else {
		return airportCode
	}
}

function getAirportSpan(airport) {
    return "<span style='display:inline-flex; align-items: center;'>" + getCountryFlagImg(airport.countryCode) + getAirportText(airport.city, airport.iata) + "</span>"
}

function setActiveDiv(activeDiv, callback) {
	var existingActiveDiv = activeDiv.siblings(":visible").filter(function (index) {
		return $(this).css("clear") != "both"
	})
	if (!callback && activeDiv.data("initCallback")) {
        callback = activeDiv.data("initCallback")
        activeDiv.removeData("initCallback")
	}

	if (existingActiveDiv.length > 0){
	    existingActiveDiv.removeClass('active')
	    activeDiv.addClass('active')
        existingActiveDiv.hide()
        activeDiv.fadeIn(400, callback)
	} else {
		if (activeDiv.is(":visible")) { //do nothing. selecting the same div as before
		    if (callback) {
		        callback()
		    }
			return false;
		} else {
			activeDiv.siblings().hide();
			activeDiv.addClass('active')
    	    activeDiv.fadeIn(200, callback);
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
//	tab.siblings().children("span").removeClass("selected")
//	//highlight the selected model
//	tab.children("span").addClass("selected")
    tab.siblings().find('.tab-icon').removeClass('selected')
    tab.find('.tab-icon').addClass('selected')
}

function highlightSwitch(selectedSwitch) {
	selectedSwitch.siblings().removeClass("selected")
	selectedSwitch.addClass("selected")
}

function closeModal(modal) {
    modal.fadeOut(200)
    var callback = modal.data("closeCallback")
    if (callback) {
        callback()
        modal.removeData("closeCallback")
    }
}

function closeAllModals() {
    $.each($(".modal"), function(index, modal) {
        if ($(modal).is(":visible")) {
            closeModal($(modal))
        }
    });
}

function disableButton(button, reason) {
    $(button).addClass("disabled")
    if ($(button).is(':input')) { //then have to manually add overlay
       $(button).after('<div class="overlay" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(255, 50, 50, 0.2)"></div>')
    }

    $(button).each(function() {
      $(this).data("originalClickFunction", $(this).attr("onclick"))
    })

//    if (!$(button).data("replacedTitle")) { //only store the title if it was NOT replaced (ie the original one)
//        $(button).data("originalTitle", $(button).attr("title"))
//    }
    if (isTouchDevice()) {
        $(button).find(".touchTitle").remove()
        if (reason) {
            $(button).css({position: 'relative'});
            var touchTitleSpan = $("<span style='display: none; text-transform: none;' class='touchTitle'>" + reason + "</span>");
            $(button).append(touchTitleSpan)
            var addedClickFunction = function() {
                 if (touchTitleSpan.is(":visible")) {
                     touchTitleSpan.hide()
                 } else {
                     touchTitleSpan.show()
                 }
             }
            $(button).removeAttr("onclick") //remove on click function
            $(button).click(addedClickFunction)
            $(button).data("addedClickFunction", addedClickFunction)
        }
    } else {
        if (reason) {
            //remove any existing tooltip
            $(button).find('.tooltiptext').remove()
            //add tooltip
            $(button).addClass("tooltip")
            var $descriptionSpan = $('<span class="tooltiptext below alignLeft" style="width: 400px;  text-transform: none;">')
            $descriptionSpan.text(reason)
            $(button).append($descriptionSpan)
        }
        $(button).removeAttr("onclick") //remove on click function
    }
}

var googleZoomRatio = [
    { 20 : 1128.497220 },
    { 19 : 2256.994440 },
    { 18 : 4513.988880 },
    { 17 : 9027.977761 },
    { 16 : 18055.955520 },
    { 15 : 36111.911040 },
    { 14 : 72223.822090 },
    { 13 : 144447.644200 },
    { 12 : 288895.288400 },
    { 11 : 577790.576700 },
    { 10 : 1155581.153000 },
    { 9  : 2311162.307000 },
    { 8  : 4622324.614000 },
    { 7  : 9244649.227000 },
    { 6  : 18489298.450000 },
    { 5  : 36978596.910000 },
    { 4  : 73957193.820000 },
    { 3  : 147914387.600000 },
    { 2  : 295828775.300000 },
    { 1  : 591657550.500000 }
]

function getGoogleZoomLevel(distanceMeterAsMaxDimension, $container, latitude) {
  var dimension = Math.min($container.width(), $container.height())
  var result = 1
  var adjustmentRatio = 5000 //just a relative number
  $.each(googleZoomRatio, function(index, entry) {
     var zoom = Object.keys(entry)[0];
     var ratio = entry[zoom]
     if (ratio * dimension > distanceMeterAsMaxDimension * adjustmentRatio) {
        result = parseInt(zoom)
        return false;
     }
  })
  if (result <= 8 && result > 1 && (latitude > 45 || latitude < -45)) {
    result -= 1
  }
  return result
}


function enableButton(button) {
    $(button).removeClass("disabled")

    $(button).each(function() {
        var addedClickFunction = $(this).data("addedClickFunction")
        if (addedClickFunction) {
            $(this).unbind("click", addedClickFunction)
        }
        var originalClickFunction = $(this).data("originalClickFunction")
        if (originalClickFunction) {
            $(this).attr("onclick", originalClickFunction) //set it back
        }
        if ($(this).is(':input')) { //then have to manually remove overlay
             $(this).next('div.overlay').remove()
        }
    })


    //remove tooltip


    if (isTouchDevice()) {
        $(button).find(".touchTitle").remove()
    } else {
        $(button).removeClass("tooltip")
        $(button).find('span.tooltiptext').remove()
    }
}

function addTooltip($target, text, css) {
    $target.addClass('tooltip')
    if ($target.css('overflow') === 'hidden') {
        $target.css('overflow', 'unset')
    }
    $target.children('span.tooltiptext').remove()
    var $descriptionSpan = $('<span class="tooltiptext below alignLeft" style="text-transform: none;">')
    $descriptionSpan.text(text)
    if (css) {
        $descriptionSpan.css(css)
    }
    $target.append($descriptionSpan)
}

function addTooltipHtml($target, html, css) {
    $target.addClass('tooltip')
    if ($target.css('overflow') === 'hidden') {
        $target.css('overflow', 'unset')
    }
    $target.children('span.tooltiptext').remove()
    var $descriptionSpan = $('<span class="tooltiptext below alignLeft" style="text-transform: none;">')
    $descriptionSpan.html(html)
    if (css) {
        $descriptionSpan.css(css)
    }
    $target.append($descriptionSpan)
}

function isIe() {
   if (/MSIE 10/i.test(navigator.userAgent)) {
      // This is internet explorer 10
      return true;
   }

   if (/MSIE 9/i.test(navigator.userAgent) || /rv:11.0/i.test(navigator.userAgent)) {
      return true;
   }

   if (/Edge\/\d./i.test(navigator.userAgent)){
      return true;
   }
}

function isTouchDevice() {
  try {
    document.createEvent("TouchEvent");
    return true;
  } catch (e) {
    return false;
  }
}


function bindEnter(bindToElement, actionFunction) {
   $(bindToElement).keyup(function(ev) {
       // 13 is ENTER
       if (ev.which === 13) {
          actionFunction()
       }
   });
}

function isPremium() {
    return activeUser && activeUser.level > 0
}

//from millisec
function toReadableDuration(duration) {
  var hours = Math.floor((duration / (1000 * 60 * 60)) % 24),
  days = Math.floor((duration / (1000 * 60 * 60 * 24)));
  var result = ""
  if (days > 0) {
    result += (days == 1 ? "1 day" : days + " days")
  }
  if (hours > 0) {
    result += " " + (hours == 1 ? "1 hour" : hours + " hours")
  }
  return result.trim()
}
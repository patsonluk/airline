function showAirportAssets(airport) {
  var $assetsDetailsDiv = $('#airportCanvas div.assetsDetails').empty()

  $.ajax({
  		type: 'GET',
  		url: "airports/" + airport.id + "/assets",
  		contentType: 'application/json; charset=utf-8',
  		dataType: 'json',
  	    success: function(assets) {
  	        $.each(assets, function(index, asset) {
                var $assetDiv = $('<div style="min-width : 150px; min-height : 85px; flex: 1;" class="section clickable">')
                if (asset.status != "BLUEPRINT") {
                    $assetDiv.addClass('selected')
                }
                var $title = $('<h5>' + asset.name  + '</h5>')
                if (asset.airline) {
                    var $airlineLogo = getAirlineLogoImg(asset.airline.id)
                    $airlineLogo.attr('title', asset.airline.name)
                    $title.append($airlineLogo)
                }
                $assetDiv.append($title)
                var $assetImage = $('<img style="max-width:100%">')
                $assetImage.attr('src', 'assets/images/airport-assets/' + asset.assetType + '.png')
                if (asset.status === "BLUEPRINT") {
                    $assetImage.css("filter", "grayscale(100%)")
                }
                $assetDiv.append($assetImage)
                $.each(asset.boosts, function(index, boost) {
                    var $boostDiv = $('<div></div>')
                    var $boostImage = $('<img src=' + boost.boostType + '.png>')
                    $boostDiv.append($boostImage)
                    var boostText = boost.value > 0 ? ('+' + boost.value) : boost.value
                    $boostDiv.append($('<span>' + boostText + '</span>'))
                    $assetDiv.append($boostDiv)
                })
                $assetsDetailsDiv.append($assetDiv)
  	        })
  	    },
          error: function(jqXHR, textStatus, errorThrown) {
  	            console.log(JSON.stringify(jqXHR));
  	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
  	    }
  	});
}


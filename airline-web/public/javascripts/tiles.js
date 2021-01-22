function drawTiles($container, tiles) {
    $container.empty()

    var xDimension = tiles.xDimension
    var yDimension = tiles.yDimension
    var tileWidth = 48
    var xStep = tileWidth / 2
    var yStep = 14
    var xCenter = ($container.width() - tileWidth) / 2
    var yCenter = $container.height() / 2 + (yDimension / 2) * yStep

    $.each(tiles.tiles, function(index, tile) {
        if (tile.master) { //only care about master
            var zIndex = xDimension - tile.x + tile.y
            var x = xCenter + (tile.x + tile.y - yDimension + 1) * xStep
            var y = yCenter - (xDimension - tile.x + tile.y + tile.ySize) * yStep
            var title = x + "," + "y"
            var $tileImg = $('<img style="position: absolute;">')
            $tileImg.attr('title', title)
            $tileImg.attr('src', tile.source)
            $tileImg.css({'z-index' : zIndex, 'bottom' : y + 'px', 'left' : x + 'px', 'width' : tileWidth * tile.xSize})
            $container.append($tileImg)
        }
    })
//
//
//
//    	<img src='@routes.Assets.versioned("images/buildings/office-3.png")' style="position: absolute; bottom: 42px; left: 36px;  z-index: 1;  width: 72px;"/>
//    								<img src='@routes.Assets.versioned("images/buildings/office-1.png")' style="position: absolute; bottom: 21px; left: 0px; z-index: 2; width: 72px"/>
//    								<img src='@routes.Assets.versioned("images/buildings/office-2.png")' style="position: absolute; bottom: 21px; left: 72px;  z-index: 2; width: 72px;"/>
//    								<img src='@routes.Assets.versioned("images/buildings/office-1.png")' style="position: absolute; bottom: 0px; left: 36px; z-index: 3; width: 72px"/>
}

function updateHeadquartersMap($container, airlineId) {
	$.ajax({
		type: 'GET',
		url: "airlines/" + airlineId + "/headquarters-map",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(tiles) {
	        var checkInterval = setInterval(() => {
	            if ($container.is(':visible')) {
	                drawTiles($container, tiles)
	                clearInterval(checkInterval)
	            }
	        }, 100)


	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}
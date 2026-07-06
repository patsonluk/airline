$(document).ready(function() {
    $('#heatmapControlPanel input[name=heatmapType]').change(function() {
        if (rivalMapAirlineId) {
          airlineId = rivalMapAirlineId
        } else {
          airlineId = activeAirline.id
        }

        if (airlineId) {
            updateHeatmap(airlineId)
        }
    })
})

function toggleHeatmap() {
    if ($("#heatmapControlPanel").is(":visible")) {
        closeHeatmap()
    } else {
        showHeatmap()
    }
}

function showHeatmap() {
//    var showAlliance = $("#heatmapControlPanel .showAlliance").is(":checked")
    $('#heatmapControlPanel').data('cycleDelta', 0)
    $("#heatmapControlPanel").css('display', 'flex')

    var airlineId
    if (rivalMapAirlineId) {
      airlineId = rivalMapAirlineId
    } else {
      airlineId = activeAirline.id
    }

    if (airlineId) {
        updateHeatmap(airlineId)
    }
}

function closeHeatmap() {
    clearHeatmap()
    $("#heatmapControlPanel").hide()
}

function clearHeatmap() {
    lastHeatmapLayerData = null
    if (deckHeatmapOverlay) {
        deckHeatmapOverlay.setProps({layers: []})
    }
}

var deckHeatmapOverlay
function getDeckHeatmapOverlay() {
    if (!deckHeatmapOverlay) {
        deckHeatmapOverlay = new deck.GoogleMapsOverlay({layers: []})
    }
    return deckHeatmapOverlay
}

//old google.maps.visualization.HeatmapLayer's dissipating:false meant "radius" is the radius at zoom level 0 and grows with zoom
//so a point's real-world footprint (and therefore overlap with neighboring points) stays constant across zoom levels.
//deck.gl's radiusPixels is always screen-space, so we replicate that by rescaling it ourselves on zoom change.
const HEATMAP_REFERENCE_ZOOM = 2
const HEATMAP_BASE_RADIUS_PIXELS = 20
const HEATMAP_ZOOM_GROWTH_FACTOR = 2 //matches the true doubling of pixel density per zoom level, so a point's real-world footprint stays constant as you zoom
const HEATMAP_MIN_RADIUS_PIXELS = 20
const HEATMAP_MAX_RADIUS_PIXELS = 400
var lastHeatmapLayerData
var heatmapViewportListenerAdded = false
var heatmapViewportRedrawTimeout

function getZoomScaledRadiusPixels() {
    var radius = HEATMAP_BASE_RADIUS_PIXELS * Math.pow(HEATMAP_ZOOM_GROWTH_FACTOR, map.getZoom() - HEATMAP_REFERENCE_ZOOM)
    return Math.min(HEATMAP_MAX_RADIUS_PIXELS, Math.max(HEATMAP_MIN_RADIUS_PIXELS, radius))
}

//HeatmapLayer aggregates into a single viewport-sized texture and doesn't support wrapLongitude (that's only on path/line/polygon
//layers), so when the visible view spans both sides of the Pacific, raw longitudes more than 180 degrees from the view center
//end up aggregated in the wrong place (or dropped). Shift each point by whole 360s to the copy nearest the current center.
function wrapLngNearCenter(lng, centerLng) {
    return lng - Math.round((lng - centerLng) / 360) * 360
}

function ensureHeatmapViewportListener() {
    if (heatmapViewportListenerAdded) {
        return
    }
    heatmapViewportListenerAdded = true
    google.maps.event.addListener(map, 'bounds_changed', function() {
        if (!lastHeatmapLayerData || !$("#heatmapControlPanel").is(":visible")) {
            return
        }
        //debounce past deck.gl's own internal viewport-sync delay so we don't re-aggregate against a stale (pre-pan/zoom) viewport
        window.clearTimeout(heatmapViewportRedrawTimeout)
        heatmapViewportRedrawTimeout = window.setTimeout(renderHeatmapLayers, 600)
    })
}

var heatmapRenderVersion = 0

//an explicit colorDomain gets rescaled internally by deck.gl in ways we can't predict or control (differently for SUM vs MEAN,
//and differently again by zoom/radius), so we let deck.gl auto-compute the domain from whatever's in view like before.
//log(weight + HEATMAP_LOG_OFFSET) still compresses large weights so dense hubs don't drown out sparser areas as much, and a
//bigger offset keeps the curve closer to linear for small weights (raise it to reduce how much low-loyalist areas get boosted).
const HEATMAP_LOG_OFFSET = 5
//points this weak render as effectively fully transparent anyway, so skip aggregating/rendering them entirely to cut GPU work
const HEATMAP_MIN_VISIBLE_WEIGHT = 0.1

function normalizedWeight(weight) {
    return Math.log(weight + HEATMAP_LOG_OFFSET) - Math.log(HEATMAP_LOG_OFFSET)
}

function renderHeatmapLayers() {
    heatmapRenderVersion++
    var radiusPixels = getZoomScaledRadiusPixels()
    //map.getCenter().lng() can come back outside -180..180 after panning repeatedly around the globe in the same direction,
    //so normalize it first or every point ends up wrapped relative to the wrong reference
    var centerLng = wrapLngNearCenter(map.getCenter().lng(), 0)
    var layers = []
    if (lastHeatmapLayerData.heatmapPositiveData.length > 0) {
        layers.push(new deck.HeatmapLayer({
            id: 'heatmap-positive',
            data: lastHeatmapLayerData.heatmapPositiveData,
            getPosition: function(d) { return [wrapLngNearCenter(d.lng, centerLng), d.lat] },
            getWeight: function(d) { return normalizedWeight(d.weight) },
            colorRange: lastHeatmapLayerData.heatmapPositiveColorRange,
            radiusPixels: radiusPixels,
            //data array reference is reused across pans, so force deck.gl to fully re-aggregate every render
            updateTriggers: {getPosition: heatmapRenderVersion, getWeight: heatmapRenderVersion}
        }))
    }

    if (lastHeatmapLayerData.heatmapNegativeData.length > 0) {
        layers.push(new deck.HeatmapLayer({
            id: 'heatmap-negative',
            data: lastHeatmapLayerData.heatmapNegativeData,
            getPosition: function(d) { return [wrapLngNearCenter(d.lng, centerLng), d.lat] },
            getWeight: function(d) { return normalizedWeight(d.weight) },
            colorRange: lastHeatmapLayerData.heatmapNegativeColorRange,
            radiusPixels: radiusPixels,
            updateTriggers: {getPosition: heatmapRenderVersion, getWeight: heatmapRenderVersion}
        }))
    }

    var overlay = getDeckHeatmapOverlay()
    overlay.setMap(map)
    overlay.setProps({layers: layers})
}

//deck.gl colorRange entries are [r, g, b, a] with a in 0-255, so convert from the css rgba(...) strings kept below for readability.
//the original alphas (authored for the old Google heatmap's much smaller radius) jump abruptly partway through instead of ramping
//smoothly, which shows up as a visible ring now that the radius is much larger, so we override alpha with a linear 0->1 ramp here.
function cssRgbaToColorRange(gradient) {
    return gradient.map(function(css, index) {
        var channels = css.match(/rgba?\(([^)]+)\)/)[1].split(',').map(function(channel) { return parseFloat(channel) })
        var alpha = index / (gradient.length - 1)
        return [channels[0], channels[1], channels[2], Math.round(alpha * 255)]
    })
}

const loyalistStatusHeatmapGradient = [
    "rgba(128, 133, 242, 0)",
    "rgba(141, 145, 243, 0.7)",
    "rgba(154, 157, 244, 0.7)",
    "rgba(167, 169, 245, 0.7)",
    "rgba(180, 181, 246, 0.7)",
    "rgba(193, 193, 247, 0.7)",
    "rgba(206, 205, 248, 0.7)",
    "rgba(219, 217, 249, 0.7)",
    "rgba(232, 229, 250, 0.7)",
    "rgba(245, 241, 251, 0.7)",
    "rgba(255, 255, 255, 1)",//
    "rgba(255, 253,  235, 1)",
    "rgba(255, 251, 215, 1)",
    "rgba(255, 249, 195, 1)",
    "rgba(255, 247, 175, 1)",
    "rgba(255, 245, 155, 1)",
    "rgba(255, 243, 135, 1)",
    "rgba(255, 241, 115, 1)",
    "rgba(255, 239, 95, 1)",
    "rgba(255, 238, 75, 1)",
    "rgba(255, 237, 52, 1)"
  ];

const loyalistTrendHeatmapPositiveGradient = [
  "rgba(40, 60, 128, 0)",
  "rgba(60, 100, 200, 0.6)",//
  "rgba(80, 120, 255, 0.8)",//
  "rgba(80, 150, 255, 1)"
];

const loyalistTrendHeatmapNegativeGradient = [
  "rgba(128, 60, 40, 0)",
  "rgba(200, 100, 60, 0.6)",//
  "rgba(255, 120, 80, 0.8)",//
  "rgba(255, 150, 80, 1)"
];

const loyalistStatusHeatmapColorRange = cssRgbaToColorRange(loyalistStatusHeatmapGradient)
const loyalistTrendHeatmapPositiveColorRange = cssRgbaToColorRange(loyalistTrendHeatmapPositiveGradient)
const loyalistTrendHeatmapNegativeColorRange = cssRgbaToColorRange(loyalistTrendHeatmapNegativeGradient)

function updateHeatmap(airlineId) {
//    $.each(historyPaths, function(index, path) { //clear all history path
//        path.setMap(null)
//        path.shadowPath.setMap(null)
//    })

    var cycleDelta = $('#heatmapControlPanel').data('cycleDelta')
    var heatmapType = $('input[name=heatmapType]:checked', '#heatmapControlPanel').val()
    $.ajax({
        type: 'GET',
        url: "airlines/" + airlineId + "/heatmap-data?heatmapType=" + heatmapType + "&cycleDelta=" + cycleDelta,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            if (!$("#heatmapControlPanel").is(":visible")) { //heatmap closed already
                clearHeatmap()
                return
            }
            var heatmapPositiveData = []
            var heatmapNegativeData = []
            $.each(result.points, function(index, entry) {
              if (Math.abs(entry.weight) < HEATMAP_MIN_VISIBLE_WEIGHT) { //renders indistinguishable from background, skip aggregating/rendering it
                return
              }
              if (entry.weight >= 0) {
                heatmapPositiveData.push({lat: entry.lat, lng: entry.lng, weight: entry.weight})
              } else {
                heatmapNegativeData.push({lat: entry.lat, lng: entry.lng, weight: entry.weight * -1})
              }
            })

            var heatmapPositiveColorRange
            var heatmapNegativeColorRange
            if (heatmapType === "loyalistImpact") {
                heatmapPositiveColorRange = loyalistStatusHeatmapColorRange
            } else if (heatmapType === "loyalistTrend") {
                heatmapPositiveColorRange = loyalistTrendHeatmapPositiveColorRange
                heatmapNegativeColorRange = loyalistTrendHeatmapNegativeColorRange
            }

            lastHeatmapLayerData = {
                heatmapPositiveData: heatmapPositiveData,
                heatmapNegativeData: heatmapNegativeData,
                heatmapPositiveColorRange: heatmapPositiveColorRange,
                heatmapNegativeColorRange: heatmapNegativeColorRange
            }
            ensureHeatmapViewportListener()
            renderHeatmapLayers()

            updateHeatmapArrows(result.minDeltaCount, airlineId)
        },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
//	    ,
//	    beforeSend: function() {
//	    	$('body .loadingSpinner').show()
//	    },
//	    complete: function(){
//	    	$('body .loadingSpinner').hide()
//	    }
    });
}

function updateHeatmapArrows(minDeltaCount, airlineId) {
    var cycleDelta = $("#heatmapControlPanel").data('cycleDelta')
    var hasPrevPrev = true
    var hasPrev = true
    var hasNext = true
    var hasNextNext = true
    if (cycleDelta <= minDeltaCount) {
        hasPrev = false
    }
    if (cycleDelta - 10 < minDeltaCount) {
        hasPrevPrev = false
    }
    if (cycleDelta >= 0) {
        hasNext = false
    }
    if (cycleDelta + 10 > 0) {
        hasNextNext = false
    }

    $("#heatmapControlPanel span.cycleDeltaText").text(cycleDelta * (-10))
    $("#heatmapControlPanel img.prev").prop("onclick", null).off("click");
    if (!hasPrev) {
        $('#heatmapControlPanel img.prev').addClass('blackAndWhite')
        $('#heatmapControlPanel img.prev').removeClass('clickable')
    } else {
        $('#heatmapControlPanel img.prev').removeClass('blackAndWhite')
        $('#heatmapControlPanel img.prev').addClass('clickable')
        $("#heatmapControlPanel img.prev").click(function() {
            $("#heatmapControlPanel").data('cycleDelta', $("#heatmapControlPanel").data('cycleDelta') - 1)
            updateHeatmap(airlineId)
        })
    }
    $("#heatmapControlPanel img.prevPrev").prop("onclick", null).off("click");
    if (!hasPrevPrev) {
        $('#heatmapControlPanel img.prevPrev').addClass('blackAndWhite')
        $('#heatmapControlPanel img.prevPrev').removeClass('clickable')
    } else {
        $('#heatmapControlPanel img.prevPrev').removeClass('blackAndWhite')
        $('#heatmapControlPanel img.prevPrev').addClass('clickable')
        $("#heatmapControlPanel img.prevPrev").click(function() {
            $("#heatmapControlPanel").data('cycleDelta', $("#heatmapControlPanel").data('cycleDelta') - 10)
            updateHeatmap(airlineId)
        })
    }

    $("#heatmapControlPanel img.next").prop("onclick", null).off("click");
    if (!hasNext) {
        $('#heatmapControlPanel img.next').addClass('blackAndWhite')
        $('#heatmapControlPanel img.next').removeClass('clickable')
    } else {
        $('#heatmapControlPanel img.next').removeClass('blackAndWhite')
        $('#heatmapControlPanel img.next').addClass('clickable')
        $("#heatmapControlPanel img.next").click(function() {
            $("#heatmapControlPanel").data('cycleDelta', $("#heatmapControlPanel").data('cycleDelta') + 1)
            updateHeatmap(airlineId)
        })
    }

     $("#heatmapControlPanel img.nextNext").prop("onclick", null).off("click");
        if (!hasNextNext) {
            $('#heatmapControlPanel img.nextNext').addClass('blackAndWhite')
            $('#heatmapControlPanel img.nextNext').removeClass('clickable')
        } else {
            $('#heatmapControlPanel img.nextNext').removeClass('blackAndWhite')
            $('#heatmapControlPanel img.nextNext').addClass('clickable')
            $("#heatmapControlPanel img.nextNext").click(function() {
                $("#heatmapControlPanel").data('cycleDelta', $("#heatmapControlPanel").data('cycleDelta') + 10)
                updateHeatmap(airlineId)
            })
        }
}


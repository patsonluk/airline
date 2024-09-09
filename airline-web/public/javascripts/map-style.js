/**
 * Note on Map Styling:
 * Google has deprecated Markers in favor of 'AdvancedMarkers', which require use of 'MapId'
 * which is a cloud based way of controlling map styles. There is no way to dynamically
 * change MapIDs of an existing map, thus to change styling you need to create a new map instance,
 * which is billed. Hence, not continuing to support the feature of changing map styles.
 * Polyline dynamic styling is still feasible, just not Map level.
 *
 * There is still a 'styles' setting on the Map, but this only works if you don't use a MapId,
 * thus since we now have to use advanced markers, we can't use this.
 *
 * Tracking Google Feature Request: https://issuetracker.google.com/issues/161501609
 */
const GOOGLE_MAPS_DARK_STYLE_MAP_ID = "<YOUR_GOOGLE_MAPID_HERE>";

var currentStyles;
var currentTypes;
var pathOpacityByStyle = {
	"dark": {
		highlight: "0.8",
		normal: "0.4"
	},
	"light": {
		highlight: "1.0",
		normal: "0.8"
	}
};

function initStyles() {
	console.log("onload cookie" + $.cookie('currentMapStyles'));
	if ($.cookie('currentMapStyles')) {
		currentStyles = $.cookie('currentMapStyles');
	} else {
		currentStyles = 'dark';
		$.cookie('currentMapStyles', currentStyles);
	}
	console.log("onload " + currentStyles);

	console.log("onload cookie" + $.cookie('currentMapTypes'));
	if ($.cookie('currentMapTypes')) {
		currentTypes = $.cookie('currentMapTypes');
	} else {
		currentTypes = 'roadmap';
		$.cookie('currentMapTypes', currentTypes);
	}
	console.log("onload " + currentTypes);
}

function getMapTypes() {
	console.log("getting " + currentTypes);
	return currentTypes;
}

function getMapColorScheme() {
	return currentStyles.toUpperCase();
}

function toggleMapLight() {
	if (currentStyles == 'dark') {
		currentStyles = 'light';
	} else {
		currentStyles = 'dark';
	}
	$.cookie('currentMapStyles', currentStyles);
	console.log($.cookie('currentMapStyles'));

	refreshLinks(false);
}
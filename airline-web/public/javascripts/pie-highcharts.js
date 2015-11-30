function plotAirportShares(airportShares, currentAirportId, container) {
	var data = []
	$.each(airportShares, function(key, airportShare) {
		var entry = {
			name : airportShare.airportName,
			y : airportShare.share
		}
		if (currentAirportId == airportShare.airportId) {
			entry["sliced"] = true
			entry["selected"] = true
		}
		data.push(entry)
	})
	container.highcharts({
		chart : {
			plotBackgroundColor : null,
			plotBorderWidth : null,
			plotShadow : false,
			height : 200,
			width : 400,
			type : 'pie',
		},
		// tooltip: {
		// format: '{point.name}: {point.percentage:.2f} %',
		// },
		plotOptions : {
			pie : {
				allowPointSelect : false,
				cursor : 'pointer',
				dataLabels : {
					enabled : false
				}
			}
		},
		series : [ {
			// name: "Airports",
			colorByPoint : true,
			data : data
		} ]
	});
}
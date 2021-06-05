function animateProgressBar(container, targetPercentage, animationDuration, callback) {
    if (targetPercentage > 100) {
        targetPercentage = 100
    }
    var gradientTopStartColor = { r: 232, g : 25, b : 87, a : 1 }
    var gradientBottomStartColor = { r: 170, g : 0, b : 51, a : 1 }
    var gradientTopEndColor = { r: 126, g : 234, b : 25, a : 1 }
    var gradientBottomEndColor = { r: 83, g : 173, b : 0, a : 1 }
    var shadowStartColor = { r: 232, g : 25, b : 87, a : 1 }
    var shadowEndColor = { r: 126, g : 234, b : 25, a : 1 }

    var barMaxLength = container.width() - 4

    var updateColorInterval = setInterval(function() { updateColor(container, gradientTopStartColor, gradientBottomStartColor, gradientTopEndColor, gradientBottomEndColor, shadowStartColor, shadowEndColor) } , 100)

//    var	currentPercentage = container.data('percentage')
//    if (!currentPercentage) {
//        currentPercentage = 0
//    }
    barLength = Math.round(targetPercentage * barMaxLength / 100);
    container.find('.bar').animate(
    {
        width:barLength
    }, { duration : animationDuration, complete: function() {
        updateColor(container, gradientTopStartColor, gradientBottomStartColor, gradientTopEndColor, gradientBottomEndColor, shadowStartColor, shadowEndColor);
        if (callback) {
            callback()
        }
        clearInterval(updateColorInterval);
    }});

}

function updateColor(container, gradientTopStartColor, gradientBottomStartColor, gradientTopEndColor, gradientBottomEndColor, shadowStartColor, shadowEndColor) {
    var barMaxLength = container.width() - 4
    var bar = container.find('.bar')
    var length = bar.css('width');
    var percentage = parseInt(length) / barMaxLength;

    var computeColor = function(startColor, endColor) {
        var r = startColor.r + (endColor.r - startColor.r) * percentage;
        var g = startColor.g + (endColor.g - startColor.g) * percentage;
        var b = startColor.b + (endColor.b - startColor.b) * percentage;
        var a = startColor.a + (endColor.a - startColor.a) * percentage;
        return 'rgba(' + r + ',' + g + ',' + b + ',' + a + ")";
    }

    bar.css('background', 'linear-gradient(to bottom, ' + computeColor(gradientTopStartColor, gradientTopEndColor) + ' 0%, ' + computeColor(gradientBottomStartColor, gradientBottomEndColor) + ' 100%)')
    bar.css('box-shadow', '0px 0px 12px 0px ' + computeColor(shadowStartColor, shadowEndColor) + ',inset 0px 1px 0px 0px rgba(255, 255, 255, 0.45),inset 1px 0px 0px 0px rgba(255, 255, 255, 0.25),inset -1px 0px 0px 0px rgba(255, 255, 255, 0.25)');

}
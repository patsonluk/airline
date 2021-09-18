//determines if the user has a set theme
function showSettings(){

    $("#settingsModal").fadeIn(500)
}
var wallpaperTemplates = [
    {
        "background" : "linear-gradient(to bottom, rgba(40, 49, 77, 0.8) 30%, rgba(29, 35, 71, 0.8) 50%, rgba(19, 25, 28, 0.8) 80%, rgba(15, 14, 14, .8) 100%), url(assets/images/background/pixel_city_1.gif)"
    },
    {
        "background" : "linear-gradient(to bottom, rgba(40, 49, 77, 0.2) 30%, rgba(29, 35, 71, 0.2) 50%, rgba(19, 25, 28, 0.2) 80%, rgba(15, 14, 14, .2) 100%), url(assets/images/background/pixel_city_2.gif)"
    },
    {
        "background" : "linear-gradient(to bottom, rgba(40, 49, 77, 0.2) 30%, rgba(29, 35, 71, 0.2) 50%, rgba(19, 25, 28, 0.2) 80%, rgba(15, 14, 14, .2) 100%), url(assets/images/background/pixel_city_3.gif)"
    },
    {
        "background" : "linear-gradient(to bottom, rgba(40, 49, 77, 0.2) 30%, rgba(29, 35, 71, 0.2) 50%, rgba(19, 25, 28, 0.2) 80%, rgba(15, 14, 14, .2) 100%), url(assets/images/background/pixel_city_4.gif)"
    }
]

refreshWallpaper()

function changeWallpaper() {
    if ($.cookie('wallpaperIndex')) {
        wallpaperIndex = parseInt($.cookie('wallpaperIndex'))
    }
    wallpaperIndex = (wallpaperIndex + 1) % wallpaperTemplates.length
    $.cookie('wallpaperIndex', wallpaperIndex)
    refreshWallpaper()
}

function refreshWallpaper() {
    var wallpaperIndex = 0
    if ($.cookie('wallpaperIndex')) {
        wallpaperIndex = parseInt($.cookie('wallpaperIndex'))
    }

    var template = wallpaperTemplates[wallpaperIndex]
    $("body").css("background", template.background)
    $("body").css("background-repeat", "no-repeat")
    $("body").css("background-attachment", "fixed")
    $("body").css("background-size", "cover")

}



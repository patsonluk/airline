//determines if the user has a set theme
function detectColorScheme(){
    var theme="dark";    //default to dark

    //local storage is used to override OS theme settings
    if(localStorage.getItem("theme")){
        theme = localStorage.getItem("theme")
    } else if (!window.matchMedia) {
        //matchMedia method not supported
        return false;
    } else {
        //CONVINCE USER DARK IS BETTER!
        //theme = window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark";
    }

    document.documentElement.setAttribute("data-theme", theme);

}
detectColorScheme();



//function that changes the theme, and sets a localStorage variable to track the theme between page loads
function switchTheme() {
    var theme = $("#switchDark").is(':checked') ? "dark" : "light"

    localStorage.setItem('theme', theme);
    document.documentElement.setAttribute('data-theme', theme);
}

$( document ).ready(function() {
    //pre-check the dark-theme checkbox if dark-theme is set
    if (document.documentElement.getAttribute("data-theme") == "dark") {
        $("#switchDark").prop('checked', true)
    } else {
        $("#switchLight").prop('checked', true)
    }
})


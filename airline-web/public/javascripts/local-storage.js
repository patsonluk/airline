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
/**
 * global variable to store device settings
 */
let deviceSettings = {};
/**
 * save any setting to local storage
 */
function saveSetting(e) {
    e.currentTarget.classList.toggle('on');
    localStorage.getItem(deviceSettings);
    deviceSettings[e.currentTarget.dataset.store] = e.currentTarget.classList.contains('on');
    localStorage.setItem('deviceSettings', JSON.stringify(deviceSettings));
}
/**
 * init
 */
window.addEventListener('DOMContentLoaded', (event) => {
    const specialToggles = document.querySelectorAll("#settingsModal .checkbox");

    if(localStorage.getItem('deviceSettings')) {
        deviceSettings = JSON.parse(localStorage.getItem('deviceSettings'));
        Object.keys(deviceSettings).forEach(key => {
            if (document.querySelector(`[data-store="${key}"]`)) {
                if (deviceSettings[key] === true) {
                    document.querySelector(`[data-store="${key}"]`).classList.add('on');
                } else {
                    document.querySelector(`[data-store="${key}"]`).classList.remove('on');
                }
            }
        });
    } else {
        specialToggles.forEach(toggle => {
            deviceSettings[toggle.dataset.store] = toggle.classList.contains('on');
        });
        localStorage.setItem('deviceSettings', JSON.stringify(deviceSettings));
    }

    specialToggles.forEach(toggle => {
        toggle.addEventListener('click', (event) => {
            saveSetting(event);
        });
    });
});

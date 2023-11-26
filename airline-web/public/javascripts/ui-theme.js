var uiThemeList = ["classic", "modern", "modern-lite"]

function changeUITheme(theme){
    if (isMobileDevice()) {
        return;
    }
	if (!uiThemeList.includes(theme)) {
		return;
	}	

	localStorage.setItem("UITheme", theme)
	if (theme === "modern-lite") {
	    $("#uiTheme").attr("href", "/assets/stylesheets/modern.css") // this currently only allows one stylesheet per theme
	    document.documentElement.setAttribute('data-ui-light-mode', true);
    } else {
        $("#uiTheme").attr("href", "/assets/stylesheets/" + theme + ".css") // this currently only allows one stylesheet per theme
        document.documentElement.removeAttribute('data-ui-light-mode');
    }
}

function loadTheme() {
    if (isMobileDevice()) {
        return;
    }
	if (!localStorage.getItem("UITheme")) {
		localStorage.setItem("UITheme", "classic")
	}
	changeUITheme(localStorage.getItem("UITheme"))
}

loadTheme();

$( document ).ready(function() {
    // preset the theme option in the dropdown
    $("option[value=\"" + localStorage.getItem("UITheme") + "\"]").attr("selected", "selected")
})
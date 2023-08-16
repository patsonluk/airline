var themeList = ["classic", "modern", "modern-lite"]

function changeUITheme(theme){
	if (!themeList.includes(theme)) {
		return;
	}	

	localStorage.setItem("UITheme", theme)
	$("#cssTheme").attr("href", "/assets/stylesheets/" + theme + ".css") // this currently only allows one stylesheet per theme
}

function loadTheme() {
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
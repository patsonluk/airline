var themeList = ["classic", "modern"]

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

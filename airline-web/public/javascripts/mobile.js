function showExtendedTopBarDetails() {
    if ($('.extendedTopBarDetails').is(":hidden")) {
        //$(".extendedTopBarDetails").show();
        $(".extendedTopBarDetails").slideDown("slow", function(){
            setTimeout(function() {
                $(".extendedTopBarDetails").slideUp("slow")
            }, 5000)
        })
    } else {
        $(".extendedTopBarDetails").slideUp("slow")
    }
    //$(".extendedTopBarDetails").css("background-color", "#FFFFFF").show("slow")
}
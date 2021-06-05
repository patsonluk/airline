var confettiCount = 100 //used to be 250
function showConfetti(container, bornThreshold) {
    removeConfetti(container)
    if (!bornThreshold) {
        bornThreshold = 3
    }
    for (var i = 0; i < confettiCount; i++) {
        var createFunction = function(i) { return function() {createConfettiBit(i, container, bornThreshold)}}
      setTimeout(createFunction(i), Math.random()*3000);
    }
}


function removeConfetti(container) {
    container.children(".confetti").remove()
}


function createConfettiBit(i, container, bornThreshold) {
  var width = Math.random() * 16; //used to be 8
  var height = width * 0.4;
  var colourIdx = Math.ceil(Math.random() * 3);
  var colour = "red";
  switch(colourIdx) {
    case 1:
      colour = "yellow";
      break;
    case 2:
      colour = "blue";
      break;
    default:
      colour = "red";
  }
  $('<div class="confetti-'+i+' '+colour+' confetti"></div>').css({
    "width" : width+"px",
    "height" : height+"px",
    "top" : -Math.random()*20+"%",
    "left" : Math.random()*100+"%",
    "opacity" : Math.random()+0.5,
    "transform" : "rotate("+Math.random()*360+"deg)"
  }).appendTo(container);
  
  dropConfetti(i, bornThreshold);
}

function dropConfetti(x, bornThreshold) {
  var $bit = $('.confetti-'+x)

  var bornCount = $bit.data('bornCount')
  if (!bornCount) {
    bornCount = 1
  } else {
    bornCount ++
  }
  $bit.data('bornCount', bornCount)

  $bit.animate({
    top: "100%",
    left: "+="+Math.random()*15+"%"
  }, Math.random()*3000 + 3000, function() {
    resetConfetti(x, bornThreshold);
  });
}

function resetConfetti(x, bornThreshold) {
  var $bit = $('.confetti-'+x)
  var bornCount = $bit.data('bornCount')
  if (bornCount < bornThreshold) {
    $bit.animate({
        "top" : -Math.random()*20+"%",
        "left" : "-="+Math.random()*15+"%"
      }, 0, function() {
        dropConfetti(x, bornThreshold);
      });
  } else {
    $bit.remove()
  }

}
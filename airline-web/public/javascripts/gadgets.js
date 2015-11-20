function generateImageBar(imageEmpty, imageFill, count, containerDiv, valueInput) {
	var images = []
	for (i = 0 ; i < count ; i ++) {
		var image = $("<img>")
		image.attr("src", imageEmpty)
		//image.click({index : i}, updateImageBar)

		image.data('index', i)
		image.click(updateImageBar)
		image.hover(updateImageBar)

		containerDiv.append(image)
		images.push(image)
	}
	if (valueInput.val()) {
		updateImageBarBySelectedIndex(valueInput.val())
	}
	
	function updateImageBar(event) {
		var index = $(this).data('index')
		updateImageBarBySelectedIndex(index)
	}
	function updateImageBarBySelectedIndex(index) {
		for (j = 0 ; j < count; j++) {
			if (j <= index) {
				images[j].attr("src", imageFill)
			} else {
				images[j].attr("src", imageEmpty)
			}
		}
		valueInput.val(index)
	}
}


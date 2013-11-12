module("mystartpage").showGadgets = (function() {

	function showGadgets(gadgetMetas) {

		function getGadgetColumns() {
			var docWidthPx = $(document).width();
			var docWidthEm = docWidthPx / parseFloat($("body").css("font-size"));
			var minWidthEm = gadgetMetas.minWidthEnum;
			var columnCount = Math.floor(docWidthEm / minWidthEm);
			columnCount = columnCount == 0 ? 1 : columnCount;
			var columnWidthPx = Math.floor(docWidthPx / columnCount) - 5;
			var columns = [];
			for (var i = 0; i < columnCount; i++) {
				var column = $('<div style="width:' + columnWidthPx
						+ 'px;position:absolute;top:0px;left:'
						+ (columnWidthPx * i) + 'px"></div>');
				columns.push(column);
				$('#gadget-container').append(column);
			}
			return columns;
		}
	
		function moveGadgetToColumns() {
			var columns = getGadgetColumns(gadgetMetas);
			var i = 0;
			$('#gadget-container > fieldset').each(function() {
				var $this = $(this);
				$this.remove();
				columns[i % columns.length].append($this);
				i++;
			});
		}

		_.each(gadgetMetas.gadgets, function(gadgetMeta) {
			mystartpage.gadgetTypeRegistry[gadgetMeta.type](gadgetMeta);
		});
		
		moveGadgetToColumns();
	}

	return showGadgets;
})();

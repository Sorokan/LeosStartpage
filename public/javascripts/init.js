module("mystartpage").init = (function() {

	function init(gadgetMetas) {		
		
		function makeHeadMapAnimateOnClick() {			
			$('#head-map').click(function() {
				$(this).animate({
					'height' : ($(this).height() == 150) 
						? "640px"
						: "150px",
					'background-position-y' : ($(this).height() == 150) 
						? "0px"
						: "-40px"
				}, 1500);
			});
		}
		
		function addCustomCss() {			
			if (typeof gadgetMetas.css !== "undefined") {
				$('head').append(
					$('<style type="text/css">' + gadgetMetas.css + '</style>'));
			}
		}
		
		function addResizeAbility() {
			$(window).resize(function() {
				window.location.reload();
			});
		}

		function showLinks() {
			if (typeof gadgetMetas.links !== "undefined") {
				_.each(gadgetMetas.links,function(link){
					$('#links').append(
						$('<a href="'+link.url+'" class="link" target="_blank">' + link.title + '</div>'));					
				});
			}
		}
		
		makeHeadMapAnimateOnClick();
		addCustomCss();
		showLinks();
		mystartpage.showGadgets(gadgetMetas);
	};
	
	return init;
	
})();
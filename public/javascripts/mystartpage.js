$(function() {

	var gadgetFrameHtml = function(cssClass, gadgetMeta) {
		var gadget = $("<fieldset class='" + cssClass + "'><legend><a href='"
				+ gadgetMeta.titlelink + "' target='_blank'>"
				+ gadgetMeta.title + "</a></legend></fieldset>");
		$("#gadget-container").append(gadget);
		return gadget;
	}

	var registry = {

		concerts : function(gadgetMeta) {
			var gadget = gadgetFrameHtml("concerts", _.extend(gadgetMeta,{titlelink:"http://www.hooolp.com"}));
			var container = $("<div></div>");
			gadget.append(container);
			$.get("/concerts/" + gadgetMeta.user + "/" + gadgetMeta.password,
					function(result) {
						_.each(result, function(artist) {
							container.append("<div>"
									+ artist.artist
									+ " <small>"
									+ _.map(artist.events, function(event) {
										return "<a href='"+event.url+"'>" + event.time + " "
												+ event.city + "</a>";
									}) + "</small></div>");
						});
					});
		},

		feed : function(gadgetMeta) {
			var gadget = gadgetFrameHtml("rss-items", gadgetMeta);
			$.getFeed({
				url : "proxyGet/" + encodeURIComponent(gadgetMeta.url),
				success : function(feed) {
					var feedContainer = $("<ul></ul>");
					gadget.append(feedContainer);
					_.each(_.first(feed.items, gadgetMeta.maxItems), function(
							item) {
						var html = $("<li><div class='tooltip'><a href='"
								+ item.link + "' target='_blank'>"
								+ "<div class='title'>" + item.title
								+ "</div></a>" + "<div class='description'>"
								+ item.description + "</div></div></li>");
						feedContainer.append(html);
					});
				},
				failure : function(failure) {
					var feedContainer = $("<div>" + failure + "</div>");
					gadget.append(feedContainer);
				},
				error : function(errorobj) {
					var feedContainer = $("<div><a href='" + gadgetMeta.url
							+ "'>" + errorobj.status + " "
							+ errorobj.statusText + "</a></div>");
					gadget.append(feedContainer);
				}
			});
		},

		weather : function(gadgetMeta) {
			var gadget = gadgetFrameHtml("weather", gadgetMeta);
			gadget.weatherfeed([ gadgetMeta.locationId ], {
				woeid : true,
				unit : 'c',
				image : true,
				country : true,
				highlow : true,
				wind : true,
				humidity : true,
				visibility : true,
				sunrise : true,
				sunset : true,
				forecast : true,
				link : false
			});
		},

		calendar : function(gadgetMeta) {
			var gadget = gadgetFrameHtml("calendar", gadgetMeta);
			var feedContainer = $("<ul></ul>");
			gadget.append(feedContainer);
			$
					.get(
							"/googleCalendar/" + gadgetMeta.user + "/"
									+ gadgetMeta.password + "/"
									+ gadgetMeta.days,
							function(result) {
								_
										.each(
												result[0],
												function(item) {
													var html = $("<li><div><div class='datetime'><div class='day'>"
															+ item.day
															+ "</div><div class='date'>"
															+ item.date
															+ "</div><div class='time'>"
															+ item.time
															+ "</div></div><div class='description'>"
															+ item.text
															+ "</div></div></li>");
													feedContainer.append(html);
												});
							});
		},

		mail : function(gadgetMeta) {
			var gadget = gadgetFrameHtml("mail", gadgetMeta);
			var feedContainer = $("<div><ul></ul></div>");
			var ul = $("ul", feedContainer);
			gadget.append(feedContainer);
			$
					.get(
							"/mails/" + gadgetMeta.user + "/"
									+ gadgetMeta.password + "/"
									+ gadgetMeta.maxItems + "/"
									+ gadgetMeta.numOfDays,
							function(result) {
								_
										.each(
												result[0],
												function(item) {
													var html = $("<li><div><div class='datetime'><div class='day'>"
															+ item.day
															+ "</div><div class='date'>"
															+ item.date
															+ "</div><div class='time'>"
															+ item.time
															+ "</div></div><div class='from'>"
															+ item.from
															+ "</div><div class='subject'>"
															+ item.subject
															+ "</div></div></li>");
													ul.append(html);
												});
							});
		},

	}

	_.each(gadgetMetas.gadgets, function(gadgetMeta) {
		registry[gadgetMeta.type](gadgetMeta);
	});

	var docWidthPx = $(document).width();
	var docWidthEm = docWidthPx / parseFloat($("body").css("font-size"));
	var minWidthEm = gadgetMetas.minWidthEnum;
	var columnCount = Math.floor(docWidthEm / minWidthEm);
	columnCount = columnCount == 0 ? 1 : columnCount;
	var columnWidthPx = Math.floor(docWidthPx / columnCount) - 5;
	var columns = [];
	for (var i = 0; i < columnCount; i++) {
		var column = $('<div style="width:' + columnWidthPx
				+ 'px;position:absolute;top:0px;left:' + (columnWidthPx * i)
				+ 'px"></div>');
		columns.push(column);
		$('#gadget-container').append(column);
	}
	var i = 0;
	$('#gadget-container > fieldset').each(function() {
		var $this = $(this);
		$this.remove();
		columns[i % columnCount].append($this);
		i++;
	});

	$('#head-map').click(function(){
		$(this).animate({
			'height': ($(this).height()==150)?"640px":"150px",
			'background-position-y': ($(this).height()==150)?"0px":"-40px"
		}, 1500 );
	});

	if (typeof gadgetMetas.css!=="undefined") {
		$('head').append($('<style type="text/css">'+gadgetMetas.css+'</style>'));
	}
	
	$(window).resize(function() {
		window.location.reload();
	});
});
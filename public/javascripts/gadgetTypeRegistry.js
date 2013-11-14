module("mystartpage").gadgetTypeRegistry = (function(){

	var gadgetFrameHtml = function(cssClass, gadgetMeta) {
		var gadget = $("<fieldset class='" + cssClass + "'><legend><a href='"
				+ gadgetMeta.titlelink + "' target='_blank'>"
				+ gadgetMeta.title + "</a></legend></fieldset>");
		$("#gadget-container").append(gadget);
		return gadget;
	}

	return {

		concerts : function(gadgetMeta) {
			var gadget = gadgetFrameHtml("concerts", _.extend(gadgetMeta, {
				titlelink : "http://www.hooolp.com"
			}));
			var container = $("<div></div>");
			gadget.append(container);
			$.get("/concerts/" + gadgetMeta.user + "/" + gadgetMeta.password,
					function(result) {
						_.each(result, function(concerts) {
							container.append("<div>"
									+ "<a href='"+concerts.artist.url+"' target='_blank'>"+concerts.artist.name+"</a>"
									+ " <small>"
									+ _.map(concerts.events, function(event) {
										return "<a href='" + event.url + "' target='_blank'>"
												+ event.time + " " + event.city
												+ "</a>";
									}).join(", ") + "</small></div>");
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
					_.each(_.first(feed.items, gadgetMeta.maxItems),
							function(item) {
								var html = $("<li><div class='tooltip'><a href='"
										+ item.link + "' target='_blank'>"
										+ "<div class='title'>" + item.title
										+ "</div></a>"
										+ "<div class='description'>"
										+ item.description + "</div></div></li>");
								feedContainer.append(html);
							});
				},
				failure : function(failure) {
					var feedContainer = $("<div>" + failure + "</div>");
					gadget.append(feedContainer);
				},
				error : function(errorobj) {
					var feedContainer = $("<div><a href='" + gadgetMeta.url + "'>"
							+ errorobj.status + " " + errorobj.statusText
							+ "</a></div>");
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
			$.get("/googleCalendar/" + gadgetMeta.user + "/" + gadgetMeta.password
					+ "/" + gadgetMeta.days, function(result) {
				_.each(result[0], function(item) {
					var html = $("<li><div><div class='datetime'><div class='day'>"
							+ item.day + "</div><div class='date'>" + item.date
							+ "</div><div class='time'>" + item.time
							+ "</div></div><div class='description'>" + item.text
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
			$.get("/mails/" + gadgetMeta.user + "/" + gadgetMeta.password + "/"
					+ gadgetMeta.maxItems + "/" + gadgetMeta.numOfDays, function(
					result) {
				_.each(result[0], function(item) {
					var html = $("<li><div><div class='datetime'><div class='day'>"
							+ item.day + "</div><div class='date'>" + item.date
							+ "</div><div class='time'>" + item.time
							+ "</div></div><div class='from'>" + item.from
							+ "</div><div class='subject'>" + item.subject
							+ "</div></div></li>");
					ul.append(html);
				});
			});
		},

	};
})();

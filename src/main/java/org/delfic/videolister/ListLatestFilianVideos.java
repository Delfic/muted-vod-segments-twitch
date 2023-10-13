package org.delfic.videolister;

import org.delfic.utils.SeleniumUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.delfic.utils.XPathUtils.getNodesForXPath;

public class ListLatestFilianVideos {
	public static final String FILIAN_LINK = "https://www.twitch.tv/filian/videos?filter=archives&sort=time";
	private static final String XPATH_ANCHOR = "//A[@data-a-target='preview-card-image-link']";

	public static void main(String[] args) {
		System.out.println(getVodLinksPerDate(FILIAN_LINK));
	}

	/**
	 * Gets the existing vod hrefs
	 *
	 * @param streamerBodListLink the URL for the streamer vod list
	 * @return a multimap that splits the VODs per date.
	 */
	public static Map<String, LocalDate> getVodLinksPerDate(final String streamerBodListLink) {
		final String vodListHtml = SeleniumUtils.getHtmlAfterJsFirstPass(streamerBodListLink);
		final Document document = Jsoup.parse(vodListHtml);
		Elements anchors = getNodesForXPath(XPATH_ANCHOR, document);
		return anchors.stream()
				.collect(Collectors.toMap(
						element -> element.attr("href"),
						element -> LocalDate.parse(element.getElementsByTag("img").attr("title"),
								DateTimeFormatter.ofPattern("MMM d, yyyy").withLocale(Locale.ROOT))
				));
	}
}

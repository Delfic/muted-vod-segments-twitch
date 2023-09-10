package org.delfic.mutedvodsements;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import us.codecraft.xsoup.Xsoup;

import java.time.Duration;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class MutedVodSegmentsDetector {
    private final static String SEGMENTS_XPATH = "//SPAN[@data-test-selector='seekbar-segment__segment']";
    private final static String DURATION_XPATH = "//P[@data-a-target='player-seekbar-duration']";
    public static final String TWITCH_VOD_BASE_PATH = "https://www.twitch.tv/videos/";

    public static void main(String[] args) {
        final String vodUrl = TWITCH_VOD_BASE_PATH + args[0];
        final String vodPageHtml = getHtmlAfterJsFirstPass(vodUrl);
        final Document document = Jsoup.parse(vodPageHtml);
        System.out.println(getMutedSegmentsFromDocument(document));
    }

    private static String getHtmlAfterJsFirstPass(final String url) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        WebDriver driver = new ChromeDriver(options);
        driver.get(url);
        final String vodPageHtml = driver.getPageSource();
        driver.quit();
        return vodPageHtml;
    }

    private static Elements getNodesForXPath(final String xPath, final Document document) {
        return Xsoup.compile(xPath).evaluate(document).getElements();
    }

    private static Duration getDurationFromDocument(final Document document) {
        final String durationString = getNodesForXPath(DURATION_XPATH, document).get(0).childNode(0).toString();
        final String[] durationSplit = durationString.split(":");
        return Duration.parse(String.format("PT%sH%sM%sS", durationSplit[0], durationSplit[1], durationSplit[2]));
    }

    private static SortedSet<Segment> getMutedSegmentsFromDocument(final Document document) {
        final Duration vodDuration = getDurationFromDocument(document);
        return getNodesForXPath(SEGMENTS_XPATH, document).stream()
                                                         .map(e -> Segment.fromSegmentNode(e, vodDuration))
                                                         .filter(Segment::isMuted)
                                                         .collect(Collectors.toCollection(TreeSet::new));
    }
}

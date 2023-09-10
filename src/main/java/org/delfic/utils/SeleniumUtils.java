package org.delfic.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Utils methods to abstract some of the functionality that uses selenium
 */
public final class SeleniumUtils {
    /**
     * Returns the HTML for the given URL
     *
     * @param url the URL to fetch the HTML for
     * @return the HTML as a 1-line string
     */
    public static String getHtmlAfterJsFirstPass(final String url) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        WebDriver driver = new ChromeDriver(options);
        driver.get(url);
        final String vodPageHtml = driver.getPageSource();
        driver.quit();
        return vodPageHtml;
    }
}

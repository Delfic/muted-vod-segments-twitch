package org.delfic.utils;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import us.codecraft.xsoup.Xsoup;

/**
 * Utils methods to abstract some of the functionality that uses XPath
 */
public final class XPathUtils {
    public static Elements getNodesForXPath(final String xPath, final Document document) {
        return Xsoup.compile(xPath).evaluate(document).getElements();
    }
}

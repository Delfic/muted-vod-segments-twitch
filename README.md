# Muted VOD segments detector for Twitch.tv

Uses Selenium to start the browser in headless mode. Required for the player to start. Then it leverages XPath to find the colored segments in the HTML, and, from the style, identify the segment start, duration and color.

## Usage

Pass in the vod id as a parameter to the main method of the `MutedVodSementDetector` class.
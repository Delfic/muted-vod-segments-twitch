package org.delfic.mutedvodsements;

import org.jsoup.nodes.Element;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Class representing a segment of the vod.
 */
public class Segment implements Comparable<Segment> {
	private static final String MUTED_COLOR = "rgba(212, 73, 73, 0.5)";
	private final Duration startDuration;
	private final Duration duration;
	private final boolean muted;
	private static final long segmentClipOffsetMillis = 5 * 1000;

	public Segment(final Duration startDuration, final Duration duration, final boolean muted) {
		this.startDuration = startDuration;
		this.duration = duration;
		this.muted = muted;
	}

	static Segment fromSegmentNode(final Element element, final Duration vodDuration) {
		final Map<String, String> stylesMap = new HashMap<>();
		String[] styles = element.attr("style").split(";");
		for (final String style : styles) {
			String[] split = style.split(":");
			stylesMap.put(split[0].trim(), split[1].trim());
		}

		double ratioFromStart = Double.parseDouble(stylesMap.get("left").split("%")[0]) / 100;
		double ratioLength = Double.parseDouble(stylesMap.getOrDefault("width", "0%").split("%")[0]) / 100;
		boolean muted = MUTED_COLOR.equals(stylesMap.get("background-color"));
		long vodMillis = vodDuration.toMillis();
		long millisStart = (long) (ratioFromStart * vodMillis);
		long millisLength = (long) (ratioLength * vodMillis);
		// it's is possible that muted segment will at the start of the vod
		// so this code in necessary to avoid negative time
		millisStart = clamp(millisStart - segmentClipOffsetMillis, 0, millisStart);
		// and also to avoid time that is more than vod duration
		millisLength = clamp(millisLength + (segmentClipOffsetMillis * 2), 0, vodMillis);
		return new Segment(Duration.ofSeconds(millisStart / 1000), Duration.ofSeconds(millisLength / 1000), muted);
	}

	static long clamp(long val, long min, long max) {
		return Math.max(min, Math.min(max, val));
	}

	@Override
	public String toString() {
		String formattedStart = String.format(Locale.US, "%02d:%02d:%02d", startDuration.toHoursPart(), startDuration.toMinutesPart(), startDuration.toSecondsPart());
		String formattedDuration = String.format(Locale.US, "%02d:%02d:%02d", duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
		return formattedStart + "-" + formattedDuration;
	}

	public boolean isMuted() {
		return muted;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final Segment segment = (Segment) o;

		if (muted != segment.muted) {
			return false;
		}
		if (!startDuration.equals(segment.startDuration)) {
			return false;
		}
		return duration.equals(segment.duration);
	}

	@Override
	public int hashCode() {
		int result = startDuration.hashCode();
		result = 31 * result + duration.hashCode();
		result = 31 * result + (muted ? 1 : 0);
		return result;
	}

	@Override
	public int compareTo(final Segment o) {
		return this.startDuration.compareTo(o.startDuration);
	}
}

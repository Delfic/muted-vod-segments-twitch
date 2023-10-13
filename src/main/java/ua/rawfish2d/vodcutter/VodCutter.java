package ua.rawfish2d.vodcutter;

import com.beust.jcommander.JCommander;
import org.delfic.mutedvodsements.MutedVodSegmentsDetector;
import org.delfic.mutedvodsements.Segment;
import org.delfic.videolister.ListLatestFilianVideos;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VodCutter {
	public static void main(String[] args) {
		new VodCutter(args);
	}

	private final List<String> localVods;
	private final Config config = new Config();

	public VodCutter(String[] args) {
		// disable annoying selenium logs
		System.setProperty("webdriver.chrome.silentOutput", "true");
		Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);

		// arrange command line arguments in class fields
		JCommander.newBuilder()
				.addObject(config)
				.build()
				.parse(args);

		// check if paths have \\ at the end, and add it if they don't
		if (!config.getVodsFolder().endsWith("\\") && !config.getVodsFolder().endsWith("/")) {
			config.setVodsFolder(config.getVodsFolder() + "\\");
		}
		if (!config.getOutputFolder().endsWith("\\") && !config.getOutputFolder().endsWith("/")) {
			config.setOutputFolder(config.getOutputFolder() + "\\");
		}

		final String vodsFolder = config.getVodsFolder();
		final String outputFolder = config.getOutputFolder();
		File outputFolderFile = new File(outputFolder);
		File inputFolderFile = new File(vodsFolder);
		// create output folder for clips if it's not a directory or doesn't exist
		if (!outputFolderFile.isDirectory() || !outputFolderFile.exists()) {
			if (!outputFolderFile.mkdir()) {
				throw new RuntimeException("Unable to create/find output folder '" + outputFolderFile + "'!");
			}
		}
		// check if input folder is a directory and does it exist
		if (!inputFolderFile.isDirectory() || !inputFolderFile.exists()) {
			throw new RuntimeException("Input folder '" + inputFolderFile + "' doesn't exist!");
		}
		// get list of files in vods folder
		localVods = listFilesUsingJavaIO(vodsFolder);

		// get twitch vods id's and date
		final String twitchLink = "https://www.twitch.tv/" + config.getChannelName() + "/videos?filter=archives&sort=time";
		final var map = ListLatestFilianVideos.getVodLinksPerDate(twitchLink);
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd").withLocale(Locale.ROOT);
		for (String vodID : map.keySet()) {
			// find local vod with same date
			final LocalDate date = map.get(vodID);
			vodID = vodID.replace("/videos/", "");
			vodID = vodID.replace("?filter=archives&sort=time", "");
			final String formattedDate = dateTimeFormatter.format(date);
			final String vodFileName = getVodFileNameWithDate(formattedDate);
			// if local vod with same date found then
			if (vodFileName != null) {
				final String vodLink = "https://www.twitch.tv/videos/" + vodID;
				System.out.println("\n====================");
				System.out.printf("Twitch VOD. Date: %s link: %s\n", formattedDate, vodID);
				System.out.printf("Found local VOD with same date %s\n", vodFileName);
				System.out.printf("Local VOD duration: %s\n", getVodDuration(vodsFolder + vodFileName));
				// get muted segments from twitch vod
				SortedSet<Segment> segments = null;
				// I guess selenium, twitch and internet connection are not perfect
				// so it works better with multiple attempts
				while (true) {
					try {
						segments = MutedVodSegmentsDetector.getMutedSegmentsFromUrl(vodLink);
						// if this attempt is successful, stop attempting
						break;
					} catch (Exception ex) {
						System.err.println("Error: " + ex.getMessage());
						System.out.println("Exception occurred while trying to get muted segments from vod (link: " + vodLink + " date: " + formattedDate + ")");
						System.out.println("Trying again...");
					}
				}
				// loop for cutting out unmuted segments from local vod
				int clipID = 0;
				for (Segment segment : segments) {
					if (!segment.isMuted()) {
						continue;
					}
					final String segmentTime = segment.toString();
					System.out.println("Starting ffmpeg to cut out unmuted parts from " + vodFileName);
					cutVod(vodFileName, segmentTime, clipID, formattedDate);
					clipID++;
				}
			}
		}
		System.out.println("Everything is done!");
	}

	private String getVodDuration(String vodFileName) {
		final String command = "ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 -sexagesimal \"" + vodFileName + "\"";
		final String result = execCmd(command);
		if (result != null) {
			return result.trim();
		}
		return "null";
	}

	private void cutVod(String vodFileName, String segmentTime, int clipID, String vodDate) {
		final String[] split = segmentTime.split("-");
		if (split.length != 2) {
			throw new RuntimeException("Something went horribly wrong! Segment time format is invalid! " + segmentTime);
		}
		final String segmentStartTime = split[0];
		final String segmentDurationTime = split[1];
		final String clipFileName = config.getOutputFolder() + config.getChannelName() + "_clip" + clipID + "_" + segmentStartTime.replace(":", "-") + "_" + vodDate;
		final String command = "ffmpeg -hide_banner -ss " + segmentStartTime +
				" -accurate_seek -i \"" + config.getVodsFolder() + vodFileName + "\" -t " + segmentDurationTime +
				" -reset_timestamps 1 -async 1 -c:v copy -movflags faststart -c:a copy \"" + clipFileName + ".mp4\"";
		// result of this call is always null for some reason
		execCmd(command);
	}

	private String getVodFileNameWithDate(String formattedDate) {
		for (String vodName : localVods) {
			if (vodName.startsWith(config.getChannelName() + "_" + formattedDate + "_") && (vodName.endsWith(".mp4") || vodName.endsWith(".mkv"))) {
				return vodName;
			}
		}
		return null;
	}

	private String execCmd(String cmd) {
		String result = null;
		try (InputStream inputStream = Runtime.getRuntime().exec(cmd).getInputStream();
		     Scanner s = new Scanner(inputStream).useDelimiter("\\A")) {
			result = s.hasNext() ? s.next() : null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	private List<String> listFilesUsingJavaIO(String dir) {
		return Stream.of(new File(dir).listFiles())
				.filter(file -> !file.isDirectory())
				.map(File::getName)
				.collect(Collectors.toList());
	}
}

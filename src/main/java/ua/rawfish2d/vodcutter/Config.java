package ua.rawfish2d.vodcutter;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;

public class Config {
	@Getter
	@Setter
	@Parameter(names = "-i", description = "input folder with VODs", required = true)
	private String vodsFolder;
	@Getter
	@Setter
	@Parameter(names = "-o", description = "output folder for clips")
	private String outputFolder = "output_clips";
	@Getter
	@Parameter(names = "-channel", description = "channel name", required = true)
	private String channelName;
}

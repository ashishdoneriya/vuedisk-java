package com.csetutorials.vuedisk.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilesListObj {

	private String name;

	@JsonProperty("isDir")
	private boolean isDir;

	private String size;

	@JsonProperty("isText")
	private boolean isText;

	@JsonProperty("isImage")
	private boolean isImage;

	@JsonProperty("isAudio")
	private boolean isAudio;

	@JsonProperty("isVideo")
	private boolean isVideo;

	private long sizeInBytes;

}

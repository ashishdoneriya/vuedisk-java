package com.csetutorials.fileserver.beans;

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

	private String mime;

	@JsonProperty("isTextFile")
	private boolean isTextFile;

}

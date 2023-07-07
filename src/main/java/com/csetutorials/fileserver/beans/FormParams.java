package com.csetutorials.fileserver.beans;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FormParams {

	private String sourceDir;

	private String destinationDir;

	private List<String> files;

	private String newName;

	private String oldName;

	private String name;

	private String url;

	private String content;

}

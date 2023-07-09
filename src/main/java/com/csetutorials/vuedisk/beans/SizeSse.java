package com.csetutorials.vuedisk.beans;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SizeSse {

	private String message;

	private long sizeInBytes;

	private long items;

	private boolean stopped;

	private boolean finished;

}

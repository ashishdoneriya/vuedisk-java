package com.csetutorials.fileserver.beans;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ErrorMessage {

	private HttpStatus httpCode;
	private String message;

}

package com.csetutorials.vuedisk.controllers;

import com.csetutorials.vuedisk.beans.ErrorMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
@Log4j2
public class ExceptionHandler {

	@org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorMessage> handleException(HttpServletRequest request, Exception e) {
		log.error("Problem while fetching response of url '" + request.getRequestURI() + "'", e);
		ErrorMessage apiError = new ErrorMessage();
		apiError.setHttpCode(HttpStatus.INTERNAL_SERVER_ERROR);
		apiError.setMessage(e.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
	}

}

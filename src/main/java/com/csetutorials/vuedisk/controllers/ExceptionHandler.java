package com.csetutorials.vuedisk.controllers;

import com.csetutorials.vuedisk.beans.ErrorMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestControllerAdvice
@Log4j2
public class ExceptionHandler {

	@org.springframework.web.bind.annotation.ExceptionHandler({AsyncRequestNotUsableException.class})
	public void handleClientAbort(HttpServletRequest request, HttpServletResponse response, Exception e) {
		if (response.isCommitted()) {
			log.debug("Ignoring client disconnect while writing response of url '{}'", request.getRequestURI());
			return;
		}
		log.debug("Ignoring client disconnect while fetching response of url '{}'", request.getRequestURI(), e);
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Void> handleMissingResource(HttpServletRequest request, NoResourceFoundException e) {
		log.debug("Missing static resource requested at '{}'", request.getRequestURI());
		return ResponseEntity.notFound().build();
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<Void> handleMissingHandler(HttpServletRequest request, NoHandlerFoundException e) {
		log.debug("Missing handler for {} {}", request.getMethod(), request.getRequestURI());
		return ResponseEntity.notFound().build();
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorMessage> handleException(HttpServletRequest request, HttpServletResponse response, Exception e) {
		if (response.isCommitted()) {
			log.debug("Response already committed for url '{}', skipping error body", request.getRequestURI(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
		log.error("Problem while fetching response of url '{}'", request.getRequestURI(), e);
		ErrorMessage apiError = new ErrorMessage();
		apiError.setHttpCode(HttpStatus.INTERNAL_SERVER_ERROR);
		apiError.setMessage(e.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
	}

}

package com.auruspay.exceptionhandler;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionhandler {

	@ExceptionHandler(NullPointerException.class)
	public ResponseEntity<ErrorResponse> handleTransactionException(NullPointerException ex) {
		ErrorResponse error = new ErrorResponse();

		error.setMessage(ex.getMessage());
		error.setStatus(HttpStatus.NOT_FOUND);
		error.setTimestamp(LocalDateTime.now());

		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(NodeNotFountException.class)
	public ResponseEntity<String> handleNodeException(NodeNotFountException ex){
		String error = ex.getMessage();
		
		return new ResponseEntity<String>(error,HttpStatus.NOT_FOUND);
		
	}
}

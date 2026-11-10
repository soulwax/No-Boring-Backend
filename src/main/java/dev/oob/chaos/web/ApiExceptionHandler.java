package dev.oob.chaos.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import dev.oob.chaos.board.ThingNotFoundException;

/** Every error leaves as an RFC 9457 {@code application/problem+json} body, worded in the request's language. */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler
	ProblemDetail handleNotFound(ThingNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				message("problem.not-found.detail", String.valueOf(ex.id())));
		problem.setTitle(message("problem.not-found.title"));
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
			.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message("problem.invalid.detail"));
		problem.setTitle(message("problem.invalid.title"));
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	private String message(String code, Object... args) {
		return getMessageSource().getMessage(code, args, LocaleContextHolder.getLocale());
	}

}

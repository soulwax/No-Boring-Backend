package dev.oob.chaos.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Rejects submissions that are already fun: this board is for boring things only. */
@Documented
@Constraint(validatedBy = ActuallyBoringValidator.class)
@Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ActuallyBoring {

	String message() default "That already sounds fun. Bring us something truly boring.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}

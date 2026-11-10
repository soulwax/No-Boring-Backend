package dev.oob.chaos.web;

import java.util.Locale;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ActuallyBoringValidator implements ConstraintValidator<ActuallyBoring, String> {

	static final Set<String> TOO_FUN = Set.of("party", "rollercoaster", "fireworks", "skydiving", "concert",
			"festival", "dragon", "laser", "volcano");

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		String lower = value.toLowerCase(Locale.ROOT);
		return !lower.contains("!!") && TOO_FUN.stream().noneMatch(lower::contains);
	}

}

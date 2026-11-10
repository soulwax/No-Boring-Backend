package dev.oob.chaos.web;

import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import dev.oob.chaos.twist.Genre;

/** {@code genre} is optional: leave it out for a surprise. */
public record SubmitRequest(
		@NotBlank(message = "{thing.original.blank}")
		@Size(max = 140) @ActuallyBoring String original,
		@Size(max = 40) @Nullable String author,
		@Nullable Genre genre) {
}

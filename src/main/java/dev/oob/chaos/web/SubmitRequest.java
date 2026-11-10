package dev.oob.chaos.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitRequest(
		@NotBlank(message = "Even nothing is more interesting than this. Type something.")
		@Size(max = 140) @ActuallyBoring String original,
		@Size(max = 40) String author) {
}

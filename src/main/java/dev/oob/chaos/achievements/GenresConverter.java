package dev.oob.chaos.achievements;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import dev.oob.chaos.twist.Genre;

/** A player's genres as one comma-separated column, e.g. {@code NOIR,HEIST}. */
@Converter
class GenresConverter implements AttributeConverter<Set<Genre>, String> {

	@Override
	public String convertToDatabaseColumn(Set<Genre> genres) {
		return genres == null ? "" : genres.stream().map(Genre::name).sorted().collect(Collectors.joining(","));
	}

	@Override
	public Set<Genre> convertToEntityAttribute(String column) {
		Set<Genre> genres = EnumSet.noneOf(Genre.class);
		if (column != null && !column.isBlank()) {
			Arrays.stream(column.split(",")).map(Genre::valueOf).forEach(genres::add);
		}
		return genres;
	}

}

package dev.oob.chaos.board;

import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores a short list of lines in one text column, so lore needs no join table. */
@Converter
class LinesConverter implements AttributeConverter<List<String>, String> {

	@Override
	public String convertToDatabaseColumn(List<String> lines) {
		return (lines == null || lines.isEmpty()) ? "" : String.join("\n", lines);
	}

	@Override
	public List<String> convertToEntityAttribute(String column) {
		return (column == null || column.isEmpty()) ? List.of() : List.of(column.split("\n"));
	}

}

package eu.cybershu.pocketstats.reader;

import eu.cybershu.pocketstats.db.Item;
import eu.cybershu.pocketstats.reader.api.ReaderItem;
import eu.cybershu.pocketstats.reader.api.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ReaderItemToDbItemMapper {
    ReaderItemToDbItemMapper INSTANCE = Mappers.getMapper(ReaderItemToDbItemMapper.class);

    static List<String> mapTags(Map<String, Tag> tags) {
        if (tags == null || tags.isEmpty()) return Collections.emptyList();
        else return tags.keySet().stream().toList();
    }

    @Mapping(target = "category", constant = "article")
    @Mapping(target = "source", constant = "READER")
    Item apiToEntity(ReaderItem readerItem);
}
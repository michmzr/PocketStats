package eu.cybershu.pocketstats.db;

import eu.cybershu.pocketstats.pocket.api.ListItem;
import eu.cybershu.pocketstats.pocket.api.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface PocketItemToDbItemMapper {
    PocketItemToDbItemMapper INSTANCE = Mappers.getMapper(PocketItemToDbItemMapper.class);

    static List<String> mapTags(Map<String, Tag> tags) {
        if (tags == null || tags.isEmpty()) return Collections.emptyList();
        else return tags.keySet().stream().toList();
    }

    //@Mapping(target = "category", constant = "article")
    @Mapping(target = "source", constant = "POCKET")
    Item apiToEntity(ListItem item);
}

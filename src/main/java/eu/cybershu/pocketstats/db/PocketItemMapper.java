package eu.cybershu.pocketstats.db;

import eu.cybershu.pocketstats.model.api.ListItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PocketItemMapper {
    PocketItemMapper INSTANCE = Mappers.getMapper(PocketItemMapper.class);

    PocketItem apiModelToDb(ListItem item);
}

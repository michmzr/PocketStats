package eu.cybershu.pocketstats.reader;

import eu.cybershu.pocketstats.db.Item;
import eu.cybershu.pocketstats.db.ItemStatus;
import eu.cybershu.pocketstats.reader.api.Location;
import eu.cybershu.pocketstats.reader.api.ReaderItem;
import eu.cybershu.pocketstats.reader.api.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
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

    // Custom mapping logic for the `status` field
    @Named("readerStatusToItemStatus")
    static ItemStatus readerStatusToItemStatus(ReaderItem readerItem) {
        if(readerItem.location() == null) {
            return ItemStatus.TO_READ;
        }

        switch (readerItem.location()) {
            case NEW, LATER, FEED, SHORTLIST -> {
                return ItemStatus.TO_READ;
            }
            case ARCHIVE -> {
                return ItemStatus.ARCHIVED;
            }
            default -> {
                return ItemStatus.TO_READ;
            }
        }
    }

    // Custom mapping logic for the `timeRead` field
    @Named("readerLocationToTimeRead")
    static Instant readerLocationToTimeRead(ReaderItem readerItem) {
        if (readerItem.location() == Location.ARCHIVE) {
            return readerItem.last_moved_at();
        }
        return null;
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "url", source = "url")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "favorite", constant = "false")
    @Mapping(target = "status", source = ".", qualifiedByName = "readerStatusToItemStatus") // Use custom logic
    @Mapping(target = "timeAdded", source = "created_at")
    @Mapping(target = "timeUpdated", source = "updated_at")
    @Mapping(target = "timeRead", source = ".", qualifiedByName = "readerLocationToTimeRead") // Use custom logic
    @Mapping(target = "excerpt", source = "summary")
    @Mapping(target = "wordCount", source = "wordCount")
    @Mapping(target = "category", constant = "article")
    @Mapping(target = "source", constant = "READER")
    @Mapping(target = "lang", constant = "null")
    Item apiToEntity(ReaderItem readerItem);
}

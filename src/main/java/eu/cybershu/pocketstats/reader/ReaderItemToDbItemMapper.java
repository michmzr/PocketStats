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

    //custom function for field 'status' for Item from ReaderItem
    // maps: location==new -> ItemStatus.UNREAD
    // maps: location==later -> ItemStatus.UNREAD
    // maps: location==feed -> ItemStatus.UNREAD
    // maps: location==shortlist -> ItemStatus.UNREAD
    // maps: location==archive -> ItemStatus.ARCHIVED


    @Mapping(target = "id", source = "id")
    @Mapping(target = "url", source = "url")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "favorite", constant = "false")
    @Mapping(target = "status", qualifiedByName = "readerStatusToItemStatus")
    @Mapping(target = "timeAdded", source = "created_at")
    @Mapping(target = "timeUpdated", source = "updated_at")
    @Mapping(target = "timeRead", qualifiedByName = "readerLocationToTimeRead")
    @Mapping(target = "timeFavorited", constant = "null")
    @Mapping(target = "resolvedTitle", source = "title")
    @Mapping(target = "resolvedUrl", source = "sourceUrl")
    @Mapping(target = "excerpt", source = "summary")
    @Mapping(target = "wordCount", source = "wordCount")
    @Mapping(target = "category", constant = "article")
    @Mapping(target = "source", constant = "READER")
    Item apiToEntity(ReaderItem readerItem);

    @Named("readerStatusToItemStatus")
    public static ItemStatus readerStatusToItemStatus(ReaderItem readerItem) {
        switch (readerItem.location()) {
            case NEW -> {
                return ItemStatus.TO_READ;
            }
            case LATER -> {
                return ItemStatus.TO_READ;
            }
            case FEED -> {
                return ItemStatus.TO_READ;
            }
            case SHORTLIST -> {
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

    @Named("readerLocationToTimeRead")
    public static Instant readerLocationToTimeRead(ReaderItem readerItem) {
        switch (readerItem.location()) {
            case ARCHIVE -> {
                return readerItem.last_moved_at();
            }
            default -> {
                return null;
            }
        }
    }
}
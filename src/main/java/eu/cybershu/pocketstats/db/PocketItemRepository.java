package eu.cybershu.pocketstats.db;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface PocketItemRepository extends MongoRepository<PocketItem, String> {
    Optional<PocketItem> findById(String id);
}

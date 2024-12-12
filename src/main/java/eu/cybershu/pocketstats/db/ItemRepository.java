package eu.cybershu.pocketstats.db;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface ItemRepository extends MongoRepository<Item, String> {
    Optional<Item> findById(String id);
}


package eu.cybershu.pocketstats.db;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

@Service
public interface MigrationStatusRepository extends MongoRepository<MigrationStatus, String> {
    MigrationStatus findTopByOrderByDateDesc();
}

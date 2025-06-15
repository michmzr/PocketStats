package eu.cybershu.pocketstats.db;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public interface MigrationStatusRepository extends MongoRepository<MigrationStatus, String> {
    Optional<MigrationStatus> findFirstBySourceOrderByDateDesc(Source source);
}

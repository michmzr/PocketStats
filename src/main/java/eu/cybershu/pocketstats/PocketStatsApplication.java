package eu.cybershu.pocketstats;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.TimeZone;

@EnableMongoRepositories
@SpringBootApplication
public class PocketStatsApplication {
	public static void main(String[] args) {
		SpringApplication.run(PocketStatsApplication.class, args);
	}

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}
}

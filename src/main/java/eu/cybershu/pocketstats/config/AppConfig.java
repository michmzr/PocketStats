package eu.cybershu.pocketstats.config;

import eu.cybershu.pocketstats.shell.InputReader;
import org.jline.reader.LineReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class AppConfig {
    @Bean
    public InputReader inputReader(@Lazy LineReader lineReader) {
        return new InputReader(lineReader);
    }
}

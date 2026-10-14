package eu.cybershu.pocketstats.reader

import org.springframework.boot.test.context.runner.ApplicationContextRunner
import spock.lang.Specification
import spock.lang.Unroll

class ReaderSyncConfigurationTest extends Specification {
    @Unroll
    def "Readwise availability for token #token is #available"() {
        expect:
        new ReaderSyncConfiguration(token).isAvailable() == available

        where:
        token       | available
        null        | false
        ''          | false
        '  \t\n'   | false
        'test-key'  | true
    }

    def "configuration initializes without a Pocket key or Readwise token"() {
        expect:
        new ApplicationContextRunner()
                .withUserConfiguration(ReaderSyncConfiguration)
                .withPropertyValues('READER_ACCESS_TOKEN=')
                .run { context ->
                    assert context.startupFailure == null
                    assert !context.getBean(ReaderSyncConfiguration).isAvailable()
                }
        true
    }
}

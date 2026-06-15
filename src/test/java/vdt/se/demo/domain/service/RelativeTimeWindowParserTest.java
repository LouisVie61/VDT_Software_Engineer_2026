package vdt.se.demo.domain.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RelativeTimeWindowParserTest {

    private final RelativeTimeWindowParser parser = new RelativeTimeWindowParser();

    @Test
    void parsesHourAndDayRelativeWindows() {
        assertThat(parser.parse("Failed login in last 24h"))
                .hasValueSatisfying(window -> assertThat(window.duration()).isEqualTo(Duration.ofHours(24)));
        assertThat(parser.parse("failed auth in past 12 hours"))
                .hasValueSatisfying(window -> assertThat(window.duration()).isEqualTo(Duration.ofHours(12)));
        assertThat(parser.parse("failed auth in previous 7d"))
                .hasValueSatisfying(window -> assertThat(window.duration()).isEqualTo(Duration.ofDays(7)));
        assertThat(parser.parse("failed auth in last 7 days"))
                .hasValueSatisfying(window -> assertThat(window.duration()).isEqualTo(Duration.ofDays(7)));
    }

    @Test
    void returnsEmptyWhenNoRelativeWindowExists() {
        assertThat(parser.parse("show failed logins")).isEmpty();
    }
}

package vdt.se.demo.domain.valueObjects;

import java.time.Duration;

public record RelativeTimeWindow(Duration duration) {

    public RelativeTimeWindow {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            duration = Duration.ofDays(1);
        }
    }

    public static RelativeTimeWindow ofHours(int hours) {
        return new RelativeTimeWindow(Duration.ofHours(Math.max(1, hours)));
    }

    public static RelativeTimeWindow ofDays(int days) {
        return new RelativeTimeWindow(Duration.ofDays(Math.max(1, days)));
    }
}

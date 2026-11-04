package br.com.financeiro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financeiro.backend.security.LoginLimitProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {

    private static final String EMAIL = "ana@exemplo.com";
    private static final Duration BLOCK = Duration.ofMinutes(15);

    private final MovableClock clock = new MovableClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(new LoginLimitProperties(3, BLOCK), clock);

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(EMAIL);
        }
    }

    @Test
    void belowTheLimitIsNotBlocked() {
        fail(2);

        assertThat(limiter.remainingBlock(EMAIL)).isEmpty();
    }

    @Test
    void reachingTheLimitBlocksForTheWholeDuration() {
        fail(3);

        assertThat(limiter.remainingBlock(EMAIL)).contains(BLOCK);
    }

    @Test
    void blockEndsExactlyWhenTheDurationPasses() {
        fail(3);

        clock.advance(BLOCK.minusSeconds(1));
        assertThat(limiter.remainingBlock(EMAIL)).contains(Duration.ofSeconds(1));

        clock.advance(Duration.ofSeconds(1));
        assertThat(limiter.remainingBlock(EMAIL)).isEmpty();
    }

    @Test
    void countStartsOverAfterTheBlockEnds() {
        fail(3);
        clock.advance(BLOCK);

        fail(2);

        assertThat(limiter.remainingBlock(EMAIL)).isEmpty();
    }

    @Test
    void failuresFarApartDoNotAddUp() {
        fail(2);
        clock.advance(BLOCK);

        fail(2);

        assertThat(limiter.remainingBlock(EMAIL)).isEmpty();
    }

    @Test
    void successClearsTheCount() {
        fail(2);

        limiter.recordSuccess(EMAIL);
        fail(2);

        assertThat(limiter.remainingBlock(EMAIL)).isEmpty();
    }

    @Test
    void capitalLettersAndSpacesAreTheSameAccount() {
        limiter.recordFailure("ANA@exemplo.com");
        limiter.recordFailure(" ana@EXEMPLO.com ");
        limiter.recordFailure(EMAIL);

        assertThat(limiter.remainingBlock("Ana@Exemplo.com")).isPresent();
    }

    @Test
    void eachEmailHasItsOwnCount() {
        fail(3);

        assertThat(limiter.remainingBlock("bia@exemplo.com")).isEmpty();
    }

    @Test
    void textLongerThanAnyEmailIsNotKeptInMemory() {
        String huge = "a".repeat(255);

        limiter.recordFailure(huge);
        limiter.recordFailure(huge);
        limiter.recordFailure(huge);

        assertThat(limiter.remainingBlock(huge)).isEmpty();
    }

    @Test
    void invalidConfigurationIsRefused() {
        assertThatThrownBy(() -> new LoginLimitProperties(0, BLOCK)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new LoginLimitProperties(3, Duration.ZERO)).isInstanceOf(IllegalStateException.class);
    }

    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-11-04T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}

package org.pmiops.workbench;

import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Test configuration for fake Entity DateTimes in Spring JPA, implemented via JPA auditing. This
 * should be imported by most tests that use @DataJpaTest. The desired fake time can be manipulated
 * by injecting the {@code FakeClock} and setting the time.
 *
 * <p>This configuration affects Entity properties which are annotated with:
 *
 * <ul>
 *   <li>{@code @CreatedDate}
 *   <li>{@code @LastModifiedDate}
 * </ul>
 *
 * Please note that the entity model must also be annotated with {@code @EntityListeners} for
 * automated time setting to take effect.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "fakeDateTimeProvider")
public class JpaFakeDateTimeConfiguration {
  @Bean(name = "fakeDateTimeProvider")
  public DateTimeProvider dateTimeProvider(Clock clock) {
    return () -> Optional.of(clock.instant());
  }
}

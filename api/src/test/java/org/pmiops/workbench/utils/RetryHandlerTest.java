package org.pmiops.workbench.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.google.GoogleRetryHandler;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class RetryHandlerTest {

  private GoogleRetryHandler googleRetryHandler =
      new GoogleRetryHandler(new ExponentialRandomBackOffPolicy());

  @Test
  public void testThrowErrorWhenExhausted() {
    System.out.println("~~~~~~~~~");
    assertThrows(
        UnauthorizedException.class,
        () ->
            googleRetryHandler.run(
                (context) -> {
                  throw new UnauthorizedException("Test");
                }));
  }
}

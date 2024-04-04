package org.pmiops.workbench.opentelemetry;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import javax.annotation.Nonnull;

public class NoopSpanExporter implements SpanExporter {
  /**
   * Noop implementation for exporting spans.
   *
   * @param spans The {@link Collection} of {@link SpanData} that need to be exported.
   * @return a success result code indicated via {@link CompletableResultCode#ofSuccess()}.
   */
  @Override
  public CompletableResultCode export(@Nonnull Collection<SpanData> spans) {
    return CompletableResultCode.ofSuccess();
  }

  /**
   * Noop implementation for flushing current spans.
   *
   * @return a success result code indicated via {@link CompletableResultCode#ofSuccess()}.
   */
  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  /**
   * Noop implementation for shutting down the current exporter.
   *
   * @return a success result code indicated via {@link CompletableResultCode#ofSuccess()}.
   */
  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }
}

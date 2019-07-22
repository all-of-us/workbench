package org.pmiops.workbench.notebooks;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Response;
import io.opencensus.common.Scope;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.logging.Logger;

/**
 * Created by brubenst on 7/22/19.
 */
public class NotebooksApiClientTracer extends ApiClient {
  private static final Tracer tracer = Tracing.getTracer();
  private static final Logger log = Logger.getLogger(NotebooksApiClientTracer.class.getName());

  @Override
  public <T> ApiResponse<T> execute(Call call, Type returnType) throws ApiException {
    Response response;
    T data;
    try (Scope ss =
             tracer
                 .spanBuilderWithExplicitParent("NotebooksApiCall", tracer.getCurrentSpan())
                 .startScopedSpan()) {
      response = call.execute();
      data = handleResponseWithTracing(response, returnType);
    } catch (IOException e) {
      throw new ApiException(e);
    }
    return new ApiResponse<>(response.code(), response.headers().toMultimap(), data);
  }

  private <T> T handleResponseWithTracing(Response response, Type returnType) throws ApiException {
    String targetUrl = response.request().httpUrl().encodedPath();
    Scope urlSpan =
        tracer.spanBuilderWithExplicitParent(targetUrl, tracer.getCurrentSpan()).startScopedSpan();
    urlSpan.close();
    return super.handleResponse(response, returnType);
  }
}

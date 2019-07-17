package org.pmiops.workbench.firecloud;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Response;
import org.pmiops.workbench.api.ClusterController;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by brubenst on 7/16/19.
 */
public class FirecloudApiClientTracer extends ApiClient {
  private static final Logger log = Logger.getLogger(FirecloudApiClientTracer.class.getName());

  @Override
  public <T> ApiResponse<T> execute(Call call, Type returnType) throws ApiException {
    long start = Instant.now().toEpochMilli();

    log.log(Level.INFO, "" + start);
    ApiResponse<T> resp = super.execute(call, returnType);
    log.log(Level.INFO, resp.getHeaders().keySet().stream().collect(Collectors.joining(",")));
    long end = Instant.now().toEpochMilli();
    log.log(Level.INFO, "" + end);
    return resp;
  }

  @Override
  public <T> T handleResponse(Response response, Type returnType) throws ApiException {
    log.log(Level.INFO, response.request().httpUrl().fragment());
    return super.handleResponse(response, returnType);
  }
}

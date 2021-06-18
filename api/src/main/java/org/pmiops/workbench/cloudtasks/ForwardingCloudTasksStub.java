package org.pmiops.workbench.cloudtasks;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CreateTaskRequest;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.cloud.tasks.v2.stub.CloudTasksStub;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless Cloud Tasks stub which immediately forwards all incoming tasks for execution. Tasks are
 * dispatched asynchronously, and are not throttled or queued. This stub is intended to be used for
 * testing purposes only.
 *
 * <p>Limitations:
 * <li>Only Google App Engine cloud tasks are supported
 * <li>Only task creation is supported
 * <li>Queue configuration (queue name, project name) are not validated
 */
public class ForwardingCloudTasksStub extends CloudTasksStub {
  private static final Logger log = Logger.getLogger(ForwardingCloudTasksStub.class.getName());

  private final String baseUrl;

  public ForwardingCloudTasksStub(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  public UnaryCallable<CreateTaskRequest, Task> createTaskCallable() {
    return new UnaryCallable<CreateTaskRequest, Task>() {
      @Override
      public ApiFuture<Task> futureCall(CreateTaskRequest request, ApiCallContext context) {
        final QueueName queueName = QueueName.parse(request.getParent());
        final AppEngineHttpRequest gaeReq = request.getTask().getAppEngineHttpRequest();
        final Request apiReq =
            new Request.Builder()
                .url(baseUrl + gaeReq.getRelativeUri())
                .addHeader("X-AppEngine-QueueName", queueName.getQueue())
                .post(
                    RequestBody.create(
                        MediaType.parse("application/json; charset=utf-8"),
                        gaeReq.getBody().toStringUtf8()))
                .build();

        log.info(
            String.format(
                "asynchronously forwarding task request for queue '%s', to handler '%s'",
                queueName.getQueue(), apiReq.url()));
        new OkHttpClient()
            .newCall(apiReq)
            .enqueue(
                new Callback() {
                  @Override
                  public void onFailure(Request request, IOException e) {
                    log.log(Level.SEVERE, "task execution failed", e);
                  }

                  @Override
                  public void onResponse(Response response) {}
                });
        return ApiFutures.immediateFuture(request.getTask());
      }
    };
  }

  @Override
  public void close() {}

  @Override
  public void shutdown() {}

  @Override
  public boolean isShutdown() {
    return true;
  }

  @Override
  public boolean isTerminated() {
    return true;
  }

  @Override
  public void shutdownNow() {}

  @Override
  public boolean awaitTermination(long duration, TimeUnit unit) {
    return true;
  }
}

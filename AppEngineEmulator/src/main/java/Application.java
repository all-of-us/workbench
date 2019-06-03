import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Application {

    private static String BASE_URL = "http://localhost:8081";

    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient();

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            System.out.println("Simluating cron!!!");

            Request request = new Request.Builder()
                        .addHeader("X-AppEngine-Cron", "true")
                        .url(BASE_URL + "/v1/cron/syncBillingProjectStatus")
                        .build();

            Call call = client.newCall(request);
            try {
                System.out.println(call.execute());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }, 0, 5, TimeUnit.SECONDS);
    }

}

package org.broad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Application {

  private static String CURRENT_PID = pid();
  private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  private static String BASE_URL = "http://localhost:8081";

  /*
   * These two fields are used to determine if the API server has been killed so the cron
   * emulator can kill itself. It tracks the number of consecutive connection failures and
   * assumes that the server has exited if there are a sufficient number of connection failures.
   * The hasServerStarted check is needed to ignore the first round of requests which will
   * almost always fail due to the lag between this process being started and the server being
   * ready to handle requests.
   */
  private static boolean hasServerStarted = false;
  private static int consecutiveConnectionFailures = 0;
  private static int MAX_CONNECTION_FAILURES_BEFORE_EXIT = 2;

  public static void main(String[] args) throws Exception {
    OkHttpClient client = new OkHttpClient();
    ScheduledExecutorService scheduledExecutorService = Executors
        .newSingleThreadScheduledExecutor();

    String cronYamlPath = args[0];
    String xml = new String(Files.readAllBytes(Paths.get(cronYamlPath)));
    CronEntries cronEntries = new ObjectMapper(new YAMLFactory()).readValue(xml, CronEntries.class);

    for (Cron cron : cronEntries.cron) {
      Schedule schedule = parseCronSchedule(cron.schedule);

      scheduledExecutorService.scheduleAtFixedRate(() -> {
        Request request = new Request.Builder()
            .addHeader("X-Appengine-Cron", "true")
            .url(BASE_URL + cron.url)
            .build();

        Call call = client.newCall(request);
        try {
          Response response = call.execute();
          log(response.toString());
          hasServerStarted = true;
          consecutiveConnectionFailures = 0;
        } catch (ConnectException e) {
          log(e.getMessage());
          consecutiveConnectionFailures++;
        } catch (IOException e) {
          log(e.getMessage());
        }

        if (hasServerStarted
            && consecutiveConnectionFailures >= MAX_CONNECTION_FAILURES_BEFORE_EXIT) {
          System.exit(0);
        }

      }, 0, schedule.period, schedule.timeUnit);
    }
  }

  private static void log(String s) {
    System.out.println("PID (" + CURRENT_PID + "): " + sdf.format(new Date()) + ": " + s);
  }

  private static String pid() {
    String vmName = ManagementFactory.getRuntimeMXBean().getName();
    int p = vmName.indexOf("@");
    return vmName.substring(0, p);
  }

  /*
   * Currently only parses the `every X hours` and `every X minutes` case since that is what
   * is used in the All of Us project. The plan is to expand the functionality of this parser
   * as needed.
   */
  private static Schedule parseCronSchedule(String scheduleString) {
    Matcher hoursMatcher = Pattern.compile("every (\\d+) hours").matcher(scheduleString);
    Matcher minutesMatcher = Pattern.compile("every (\\d+) minutes").matcher(scheduleString);
    if (hoursMatcher.matches()) {
      return new Schedule(TimeUnit.HOURS, Integer.parseInt(hoursMatcher.group(1)));
    } else if (minutesMatcher.matches()) {
      return new Schedule(TimeUnit.MINUTES, Integer.parseInt(minutesMatcher.group(1)));
    } else {
      throw new RuntimeException("Could not parse cron schedule: " + scheduleString);
    }
  }

}
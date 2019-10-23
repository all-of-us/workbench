package org.broad;

/*
 * POJOs to represent the org.broad.Cron config as defined in
 * https://cloud.google.com/appengine/docs/standard/java/config/cronref#example
 *
 * This class will be fed into jackson xml mapper to parse `cron.xml` files
 */
public class Cron {

  public String url;

  public String schedule;

  public String target;

  public String description;

  public String timezone;
}

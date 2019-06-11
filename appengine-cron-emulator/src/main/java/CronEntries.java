import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.ArrayList;
import java.util.List;

public class CronEntries {

  @JacksonXmlElementWrapper(useWrapping = false)
  public List<Cron> cron = new ArrayList<>();

}

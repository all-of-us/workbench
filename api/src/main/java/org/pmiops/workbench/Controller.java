package org.pmiops.workbench;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// XXX: trigger CI - revert.
@RestController
public class Controller {
  @RequestMapping("/")
  public String index() {
    return "AllOfUs Workbench API";
  }
}

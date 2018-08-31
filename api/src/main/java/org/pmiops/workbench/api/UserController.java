package org.pmiops.workbench.api;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserApiDelegate {

  private static final Logger log = Logger.getLogger(UserController.class.getName());
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_PAGE_SIZE = 10;
  private static final String DEFAULT_SORT_FIELD = "email";
  private static final Function<User, org.pmiops.workbench.model.User> TO_USER_RESPONSE_USER = user -> {
    org.pmiops.workbench.model.User modelUser = new org.pmiops.workbench.model.User();
    modelUser.setEmail(user.getEmail());
    modelUser.setGivenName(user.getGivenName());
    modelUser.setFamilyName(user.getFamilyName());
    return modelUser;
  };

  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Override
  public ResponseEntity<UserResponse> user(String term, String pageToken, Integer size, String sortOrder) {
    UserResponse response = new UserResponse();
    // fail fast
    if (null == term || term.isEmpty()) {
      response.setUsers(Collections.emptyList());
      response.setNextPageToken("");
      return ResponseEntity.ok(response);
    }

    Sort.Direction direction = Optional
        .ofNullable(Sort.Direction.fromStringOrNull(sortOrder))
        .orElse(Sort.Direction.ASC);
    Sort sort = new Sort(new Sort.Order(direction, DEFAULT_SORT_FIELD));
    List<User> users = userService.findUsersBySearchString(term, sort);
    int pageSize = Optional.ofNullable(size).orElse(DEFAULT_PAGE_SIZE);
    List<List<User>> pagedUsers = Lists.partition(users, pageSize);

    int page = getPageToken(pageToken);
    boolean hasNext = page < pagedUsers.size() - 1;
    String nextPageToken = hasNext ? Integer.toString(page + 1) : "";
    response.setNextPageToken(nextPageToken);

    if (page < pagedUsers.size()) {
      List<org.pmiops.workbench.model.User> modelUsers = pagedUsers
          .get(page)
          .stream()
          .map(TO_USER_RESPONSE_USER)
          .collect(Collectors.toList());
      response.setUsers(modelUsers);
    } else {
      log.warning(String.format("User attempted autocomplete for a paged result that doesn't exist. Term: %s. Page: %d", term, page));
      response.setUsers(Collections.emptyList());
      return ResponseEntity.badRequest().body(response);
    }
    return ResponseEntity.ok(response);
  }

  private int getPageToken(String pageToken) {
    if (pageToken == null) {
      return DEFAULT_PAGE;
    }
    try {
      return Integer.valueOf(pageToken);
    } catch (NumberFormatException nfe) {
      log.info(String.format("Unable to parse invalid page token: %s", pageToken));
    }
    return DEFAULT_PAGE;
  }

}

package org.pmiops.workbench.api;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;

@RestController
public class UserController implements UserApiDelegate {

  private static final Integer DEFAULT_PAGE = 0;
  private static final Integer DEFAULT_SIZE = 10;
  private static final String DEFAULT_SORT_FIELD = "email";
  private static final Function<User, UserResponse> TO_USER_RESPONSE = user -> {
    UserResponse userResponse = new UserResponse();
    userResponse.setEmail(user.getEmail());
    userResponse.setGivenName(user.getGivenName());
    userResponse.setFamilyName(user.getFamilyName());
    return userResponse;
  };

  private final UserDao userDao;
  private final WorkbenchConfig workbenchConfig;

  @Autowired
  public UserController(UserDao userDao, Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.userDao = userDao;
    this.workbenchConfig = workbenchConfigProvider.get();
  }

  @Override
  public ResponseEntity<List<UserResponse>> user(String term, Integer page, Integer size, String sort) {
    if (null == term || term.isEmpty()) {
      return ResponseEntity.ok(Collections.emptyList());
    }
    Sort.Direction direction = Optional
        .ofNullable(Sort.Direction.fromStringOrNull(sort))
        .orElse(Sort.Direction.ASC);
    Pageable pageable = new PageRequest(
        Optional.ofNullable(page).orElse(DEFAULT_PAGE),
        Optional.ofNullable(size).orElse(DEFAULT_SIZE),
        new Sort(direction, DEFAULT_SORT_FIELD));
    List<UserResponse> responses = userDao
        .findUsersBySearchString(term, pageable)
        .stream()
        .filter(u -> !workbenchConfig.firecloud.enforceRegistered ||
            u.getDataAccessLevelEnum().equals(DataAccessLevel.REGISTERED))
        .map(TO_USER_RESPONSE)
        .collect(Collectors.toList());
    return ResponseEntity.ok(responses);
  }

}

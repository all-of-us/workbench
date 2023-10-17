package org.pmiops.workbench.absorb;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.stream.Collectors.toList;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.pmiops.workbench.absorb.api.AuthenticateApi;
import org.pmiops.workbench.absorb.api.EnrollmentsApi;
import org.pmiops.workbench.absorb.api.UsersApi;
import org.pmiops.workbench.absorb.model.AuthenticationRequest;
import org.pmiops.workbench.absorb.model.UserCourseEnrollmentResource;
import org.pmiops.workbench.google.CloudStorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AbsorbServiceImpl implements AbsorbService {
  private final CloudStorageClient cloudStorageClient;

  @Autowired
  public AbsorbServiceImpl(CloudStorageClient cloudStorageClient) {
    this.cloudStorageClient = cloudStorageClient;
  }

  // This function returns false if the user has an account with absorb, even if they have never
  // logged into Absorb. However, in our case, a users' account is created the first time they log
  // in, and only then. We may be able to create accounts prior to a user logging in by using the
  // API but have no plans to do so. Until we do that, the concepts of "logged in before" and "has
  // account" should be equivalent. We should rename the function if that stops being true. This
  // method name is clearer to use in other services since it describes the users' actions so far
  // (domain logic) rather than relying on Absorb-specific state (implementation details).
  @Override
  public Boolean userHasLoggedIntoAbsorb(Credentials credentials) {
    return credentials.userId != null;
  }

  @Override
  public List<Enrollment> getActiveEnrollmentsForUser(Credentials credentials) throws ApiException {
    if (credentials.userId == null) {
      throw new ApiException(404, "User not found");
    }

    // Fetch enrollments for the user
    var enrollmentsResponse =
        new EnrollmentsApi()
            .enrollmentsGetUserEnrollments(
                credentials.apiKey, credentials.accessToken, credentials.userId, "isActive")
            .getEnrollments();

    // Convert enrollments to our simplified and serialized model
    return enrollmentsResponse.stream().map(this::convertToEnrollment).collect(toList());
  }

  private Enrollment convertToEnrollment(UserCourseEnrollmentResource e) {
    if (e.getDateCompleted() == null) {
      return new Enrollment(e.getCourseId(), null);
    } else {
      Instant dateCompleted =
          LocalDateTime.parse(e.getDateCompleted(), ISO_DATE_TIME).toInstant(ZoneOffset.UTC);
      return new Enrollment(e.getCourseId(), dateCompleted);
    }
  }

  // Obtains a valid API key, access token, and user id for the given email address.
  // Tokens are valid for four hours, which is well under the expected lifetime of clients of this
  // service.
  @Override
  public Credentials fetchCredentials(String email) throws ApiException {
    var config = cloudStorageClient.getAbsorbCredentials();
    var apiKey = config.getString("apiKey");

    // Obtain an access token
    var accessToken =
        new AuthenticateApi()
            .restAuthenticationAuthenticate(
                new AuthenticationRequest()
                    .password(config.getString("password"))
                    .username(config.getString("username"))
                    .privateKey(apiKey),
                apiKey);

    // Find the user id
    var users = new UsersApi().usersGetUsers(apiKey, accessToken, "username eq '" + email + "'");

    if (users.getUsers().size() > 1) {
      throw new ApiException(500, "Multiple users found");
    }

    // Size == 0 means that no user was found. This is a valid state. It indicates
    // the user has not logged in to Absorb yet.
    var userId = users.getUsers().size() == 1 ? users.getUsers().get(0).getId() : null;

    return new Credentials(apiKey, accessToken, userId);
  }
}

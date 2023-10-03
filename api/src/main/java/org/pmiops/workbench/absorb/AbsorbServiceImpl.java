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

  @Override
  public List<Enrollment> getActiveEnrollmentsForUser(String email) throws ApiException {
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

    if (users.getUsers().size() == 0) {
      throw new ApiException(404, "User not found");
    }

    // Fetch enrollments for the user
    var enrollmentsResponse =
        new EnrollmentsApi()
            .enrollmentsGetUserEnrollments(
                apiKey, accessToken, users.getUsers().get(0).getId(), "isActive")
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
}

package org.pmiops.workbench.zendesk;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.zendesk.client.v2.model.Comment;
import org.zendesk.client.v2.model.Request;
import org.zendesk.client.v2.model.Ticket.Requester;
import org.zendesk.client.v2.model.Type;

public class ZendeskRequests {
  private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

  @VisibleForTesting
  static final String RESEARCH_PURPOSE_RAW_JSON_HEADER = "[Raw research purpose JSON]";

  private ZendeskRequests() {}

  private static String buildZendeskTicketBody(DbUser user, Workspace workspace) {
    ResearchPurpose rp = workspace.getResearchPurpose();
    String primaryPurpose =
        ImmutableMap.<String, Boolean>builder()
            .put(
                "Disease focused research: "
                    + Optional.ofNullable(rp.getDiseaseOfFocus()).orElse(""),
                rp.getDiseaseFocusedResearch())
            .put("Methods development/validation study", rp.getMethodsDevelopment())
            .put("Research Control", rp.getControlSet()).put("Genetic Research", rp.getAncestry())
            .put("Social/Behavioral Research", rp.getSocialBehavioral())
            .put("Population Health/Public Health Research", rp.getPopulationHealth())
            .put("Drug/Therapeutics Development Research", rp.getDrugDevelopment())
            .put("For-Profit Purpose", rp.getCommercialPurpose())
            .put("Educational Purpose", rp.getEducational())
            .put("Other Purpose: " + rp.getOtherPurposeDetails(), rp.getOtherPurpose()).build()
            .entrySet().stream()
            .filter(Entry::getValue)
            .map(e -> " - " + e.getKey())
            .collect(Collectors.joining("\n"));
    return Lists.newArrayList(
            "[Workspace name]",
            workspace.getName(),
            "",
            "[Workspace ID]",
            String.format("%s/%s", workspace.getNamespace(), workspace.getId()),
            "",
            "[Review requester]",
            String.format(
                "%s %s (%s, %s)",
                user.getGivenName(),
                user.getFamilyName(),
                user.getUsername(),
                user.getContactEmail()),
            "",
            "[Primary purpose of project]",
            primaryPurpose,
            "",
            "[Provide the reason for choosing All of Us data for your investigation]",
            rp.getReasonForAllOfUs(),
            "",
            "[What are the specific scientific question(s) you intend to study]",
            rp.getIntendedStudy(),
            "",
            "[What are your anticipated findings from this study]",
            rp.getAnticipatedFindings(),
            "",
            "[Population area(s) of focus]",
            Joiner.on(", ")
                .join(Optional.ofNullable(rp.getPopulationDetails()).orElse(ImmutableList.of())),
            "",
            Optional.ofNullable(rp.getOtherPopulationDetails())
                .filter(s -> !s.isEmpty())
                .map(s -> "[Other population]\n" + s)
                .orElse(""),
            "",
            RESEARCH_PURPOSE_RAW_JSON_HEADER,
            PRETTY_GSON.toJson(rp))
        .stream()
        .filter(Predicates.notNull())
        .collect(Collectors.joining("\n"));
  }

  /**
   * Produces a Zendesk {@link Request} for a review of the given the given workspace, requested by
   * the given user.
   */
  public static Request workspaceToReviewRequest(DbUser user, Workspace workspace) {
    Request zdReq = new Request();
    zdReq.setType(Type.TASK);
    zdReq.setSubject("Workspace Review request for '" + workspace.getName() + "'");
    Requester requester = new Requester();
    requester.setName(user.getGivenName() + " " + user.getFamilyName());
    requester.setEmail(user.getUsername());
    zdReq.setRequester(requester);
    Comment comment = new Comment();
    comment.setBody(buildZendeskTicketBody(user, workspace));
    zdReq.setComment(comment);
    return zdReq;
  }
}

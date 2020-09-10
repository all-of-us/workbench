package org.pmiops.workbench.cohortbuilder.util;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.exceptions.BadRequestException;

public final class MySqlSearchUtil {

  private static final ImmutableList<String> SEARCH_CHARACTERS =
      ImmutableList.of("\"", "+", "-", "(", ")");

  public static String modifySearchTerm(String term) {
    if (StringUtils.isEmpty(term)) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Please provide a valid search term: \"%s\" is not valid.", term));
    }
    if (!SEARCH_CHARACTERS.stream().filter(term::contains).collect(Collectors.toList()).isEmpty()) {
      return term;
    }
    String[] keywords = term.split("\\W+");
    if (keywords.length == 1 && keywords[0].length() <= 3) {
      return "+\"" + keywords[0];
    }

    return IntStream.range(0, keywords.length)
        .filter(i -> keywords[i].length() > 2)
        .mapToObj(
            i -> {
              if ((i + 1) != keywords.length) {
                return "+\"" + keywords[i] + "\"";
              } else if (keywords[i].contains("-")) {
                return keywords[i];
              }
              return "+" + keywords[i] + "*";
            })
        .collect(Collectors.joining());
  }

  public static String modifyMultipleMatchKeyword(String query) {
    // This function modifies the keyword to match all the words if multiple words are present(by
    // adding + before each word to indicate match that matching each word is essential)
    if (query == null || query.trim().isEmpty()) {
      return null;
    }
    String[] keywords = query.split("[,+\\s+]");
    List<String> temp = new ArrayList<>();
    for (String key : keywords) {
      String tempKey;
      // This is to exact match concept codes like 100.0, 507.01. Without this mysql was matching
      // 100*, 507*.
      if (key.contains(".")) {
        tempKey = "\"" + key + "\"";
      } else {
        tempKey = key;
      }
      if (!tempKey.isEmpty()) {
        String toAdd = "+" + tempKey;
        if (tempKey.contains("-") && !temp.contains(tempKey)) {
          temp.add(tempKey);
        } else if (tempKey.contains("*") && tempKey.length() > 1) {
          temp.add(toAdd);
        } else {
          if (key.length() < 3) {
            temp.add(key);
          } else {
            temp.add(toAdd);
          }
        }
      }
    }

    StringBuilder query2 = new StringBuilder();
    for (String key : temp) {
      query2.append(key);
    }

    return query2.toString();
  }
}

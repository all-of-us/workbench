package org.pmiops.workbench.cohortbuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SearchTerm {

  private static final int MIN_TERM_LENGTH_NO_SPECIAL_CHAR = 2;
  private final String term;
  private String modifiedTerm;
  private List<String> endsWithTerms;
  private List<String> stopWords;

  public SearchTerm(String term, List<String> stopWords) {
    this.term = term;
    this.stopWords = stopWords;
    parseTermForSearch();
  }

  public String getTerm() {
    return term;
  }

  public String getModifiedTerm() {
    return modifiedTerm;
  }

  public List<String> getEndsWithTerms() {
    return endsWithTerms;
  }

  public boolean hasEndsWithOnly() {
    return !endsWithTerms.isEmpty() && modifiedTerm.isEmpty();
  }

  public boolean hasModifiedTermOnly() {
    return endsWithTerms.isEmpty() && !modifiedTerm.isEmpty();
  }

  public boolean hasEndsWithTermsAndModifiedTerm() {
    return !endsWithTerms.isEmpty() && !modifiedTerm.isEmpty();
  }

  public boolean hasNoTerms() {
    return endsWithTerms.isEmpty() && modifiedTerm.isEmpty();
  }

  private void parseTermForSearch() {
    List<String> parsedTerms = new ArrayList<>();
    // add quoted pattern to the list of modifiedTerms
    final String quotedPattern = "([+-]?\\\"[^\\\"]*\\\")";
    Pattern pattern = Pattern.compile(quotedPattern);
    Matcher matcher = pattern.matcher(term);
    while (matcher.find()) {
      parsedTerms.add(matcher.group().matches("^[+-].*") ? matcher.group() : "+" + matcher.group());
    }
    // remove the quoted phrase/pattern
    List<String> words =
        new ArrayList<>(Arrays.asList(term.replaceAll(quotedPattern, "").split(" ")));
    // remove words that start with multiple special chars
    words =
        words.stream()
            .filter(
                word ->
                    !(word.startsWith("*+")
                        || word.startsWith("*-")
                        || word.startsWith("+*")
                        || word.startsWith("-*")))
            .filter(
                word ->
                    !(word.endsWith("*+")
                        || word.endsWith("*-")
                        || word.endsWith("+*")
                        || word.endsWith("-*")))
            .filter(word -> !(word.startsWith("*") && word.endsWith("*")))
            .collect(Collectors.toList());

    List<String> endsWith =
        words.stream()
            // .filter(word -> word.toLowerCase().matches(endsWithPattern))
            .filter(word -> word.startsWith("*"))
            .map(word -> word.replaceAll("\\*", "%"))
            .map(word -> word.replaceAll("([\\.\\?])", "\\\\$1"))
            .collect(Collectors.toList());

    // now process non-endsWith words
    words = words.stream().filter(word -> !word.startsWith("*")).collect(Collectors.toList());

    words.stream()
        .forEach(
            word -> {
              if (word.matches("-\\w+(-\\w+)+")) {
                // -covid-19 -type-2-diabetes => -"covid-19" -"type-2-diabetes"
                parsedTerms.add("-\"" + word.substring(1) + "\"");
              } else if (word.matches("\\+\\w+(-\\w+)+")) {
                // +covid-19 +type-2-diabetes => +"covid-19" +"type-2-diabetes"
                parsedTerms.add("+\"" + word.substring(1) + "\"");
              } else if (word.matches("\\w+(-\\w+)+")) {
                // covid-19 type-2-diabetes => +"covid-19" +"type-2-diabetes"
                parsedTerms.add("+\"" + word + "\"");
              } else if (word.startsWith("-")) {
                // -diabet => -diabet
                parsedTerms.add(word);
              } else if (word.startsWith("+")) {
                // +diabet => +diabet*
                parsedTerms.add(word + "*");
              } else {
                // diabet => +diabet*
                parsedTerms.add("+" + word + "*");
              }
            });

    // filter: any parsed words that are stop words criteria
    final String stopWordPattern = "^(" + String.join(")$|^(", stopWords) + ")$";
    // filter: any parsed words that fail MIN_TERM_LENGTH_NO_SPECIAL_CHAR criteria
    this.endsWithTerms =
        endsWith.stream()
            .filter(w -> !w.replaceAll("[+-]", "").toLowerCase().matches(stopWordPattern))
            .filter(w -> w.replaceAll("[+%-]", "").length() >= MIN_TERM_LENGTH_NO_SPECIAL_CHAR)
            .collect(Collectors.toList());

    // fix multiple occurrence of +/-/*
    // filter: any parsed words that are stop words criteria
    // filter: any parsed words that fail MIN_TERM_LENGTH_NO_SPECIAL_CHAR criteria

    this.modifiedTerm =
        parsedTerms.stream()
            .map(x -> x.replaceAll("\\++", "\\+"))
            .map(x -> x.replaceAll("-+", "\\-"))
            .map(x -> x.replaceAll("\\*+", "\\*"))
            .filter(w -> !w.replaceAll("[+*\"-]", "").toLowerCase().matches(stopWordPattern))
            .filter(w -> w.replaceAll("[+*\"-]", "").length() >= MIN_TERM_LENGTH_NO_SPECIAL_CHAR)
            .collect(Collectors.joining(""));
  }
}

package org.pmiops.workbench.cohortbuilder;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SearchTermTest {

  @Test
  void getModifiedTerm() {
    SearchTerm searchTerm = new SearchTerm("+statin *from", getStopWords());
    assertThat(searchTerm.getModifiedTerm()).isEqualTo("+statin*");
  }

  @Test
  void getEndsWithTerms() {
    SearchTerm searchTerm = new SearchTerm("*statin +from", getStopWords());
    assertThat(searchTerm.getEndsWithTerms()).containsExactly("%statin");
  }

  @Test
  void hasEndsWithOnly() {
    SearchTerm searchTerm = new SearchTerm("*itsok +from", getStopWords());
    assertThat(searchTerm.hasModifiedTermOnly()).isFalse();
    assertThat(searchTerm.hasEndsWithOnly()).isTrue();
    assertThat(searchTerm.hasEndsWithTermsAndModifiedTerm()).isFalse();
    assertThat(searchTerm.hasNoTerms()).isFalse();
  }

  @Test
  void hasModifiedTermOnly() {
    SearchTerm searchTerm = new SearchTerm("*it +pita", getStopWords());
    assertThat(searchTerm.hasModifiedTermOnly()).isTrue();
    assertThat(searchTerm.hasEndsWithOnly()).isFalse();
    assertThat(searchTerm.hasEndsWithTermsAndModifiedTerm()).isFalse();
    assertThat(searchTerm.hasNoTerms()).isFalse();
  }

  @Test
  void hasEndsWithTermsAndModifiedTerm() {
    SearchTerm searchTerm = new SearchTerm("*statin +pita", getStopWords());
    assertThat(searchTerm.hasModifiedTermOnly()).isFalse();
    assertThat(searchTerm.hasEndsWithOnly()).isFalse();
    assertThat(searchTerm.hasEndsWithTermsAndModifiedTerm()).isTrue();
    assertThat(searchTerm.hasNoTerms()).isFalse();
  }

  @Test
  void hasNoTerms() {
    SearchTerm searchTerm = new SearchTerm("*it +is +is* about", getStopWords());
    assertThat(searchTerm.hasModifiedTermOnly()).isFalse();
    assertThat(searchTerm.hasEndsWithOnly()).isFalse();
    assertThat(searchTerm.hasEndsWithTermsAndModifiedTerm()).isFalse();
    assertThat(searchTerm.hasNoTerms()).isTrue();
  }

  @ParameterizedTest(name = "checkEndsWithTerms: {0} {1}=>{2}")
  @MethodSource("getParametersTermAndCheckEndTerms")
  void checkEndsWithTerms(String testInput, String term, List<String> expected) {
    SearchTerm actual = new SearchTerm(term, getStopWords());

    assertWithMessage(testInput).that(actual.getModifiedTerm()).isEmpty();

    assertWithMessage(testInput)
        .that(actual.getEndsWithTerms())
        .containsExactlyElementsIn(expected.toArray());
  }

  private static Stream<Arguments> getParametersTermAndCheckEndTerms() {

    return Stream.of(
        Arguments.of("Search term: ", "*lung", Arrays.asList("%lung")),
        Arguments.of("Search term: ", "*lung:", Arrays.asList("%lung:")),
        Arguments.of("Search term: ", "*lung%", Arrays.asList("%lung%")),
        Arguments.of("Search term: ", "*lung?", Arrays.asList("%lung\\?")),
        Arguments.of("Search term: ", "*lung.", Arrays.asList("%lung\\.")),
        Arguments.of("Search term: ", "*+lung", Arrays.asList()),
        Arguments.of("Search term: ", "+*lung", Arrays.asList()),
        Arguments.of("Search term: ", "*-lung", Arrays.asList()),
        Arguments.of("Search term: ", "-*lung", Arrays.asList()),
        Arguments.of("Search term: ", "*lung*", Arrays.asList()),
        Arguments.of("Search term: ", "*lung+*", Arrays.asList()),
        Arguments.of("Search term: ", "*lung-*", Arrays.asList())
        // Arguments.of("Search term: ", "*lung*+", Arrays.asList("%lung")), // not allowed by ui
        // Arguments.of("Search term: ", "*lung*-", Arrays.asList("%lung")), // not allowed by ui
        );
  }

  @ParameterizedTest(name = "checkModifiedTerm: {0} {1}=>{2}")
  @MethodSource("getParametersTermAndCheckModifiedTerm")
  void checkModifiedTerm(String testInput, String term, String expected) {
    SearchTerm actual = new SearchTerm(term, getStopWords());
    assertWithMessage(testInput).that(actual.getModifiedTerm()).isEqualTo(expected);
  }

  private static Stream<Arguments> getParametersTermAndCheckModifiedTerm() {

    return Stream.of(
        Arguments.of("Search term: ", "lung", "+lung*"),
        Arguments.of("Search term: ", "+lung", "+lung*"),
        Arguments.of("Search term: ", "lung cancer", "+lung*+cancer*"),
        Arguments.of("Search term: ", "lung cancer", "+lung*+cancer*"),
        Arguments.of("Search term: ", "lung* cancer", "+lung*+cancer*"),
        Arguments.of("Search term: ", "lung cancer*", "+lung*+cancer*"),
        Arguments.of("Search term: ", "lung* cancer*", "+lung*+cancer*"),
        Arguments.of("Search term: ", "+lung cancer", "+lung*+cancer*"),
        Arguments.of("Search term: ", "lung +cancer", "+lung*+cancer*"),
        Arguments.of("Search term: ", "+lung +cancer", "+lung*+cancer*"),
        Arguments.of("Search term: ", "lung -cancer", "+lung*-cancer"),
        Arguments.of("Search term: ", "+lung -cancer", "+lung*-cancer"),
        Arguments.of("Search term: ", "lung* -cancer", "+lung*-cancer"),
        Arguments.of("Search term: ", "\"lung cancer\"", "+\"lung cancer\""),
        Arguments.of("Search term: ", "covid-19", "+\"covid-19\""),
        Arguments.of("Search term: ", "type-2-diabetes", "+\"type-2-diabetes\""),
        Arguments.of("Search term: ", "*statin pita", "+pita*"),
        Arguments.of("Search term: ", "*statin +pita", "+pita*"),
        Arguments.of("Search term: ", "-pita brea", "-pita+brea*"),
        Arguments.of("Search term: ", "*statin -pita", "-pita"),
        Arguments.of("Search term: ", "*statin *pita", ""),
        Arguments.of("Search term: ", "*statin other *pita", "+other*"),
        Arguments.of("Search term: ", "*statin other *pita -minus", "+other*-minus"),
        Arguments.of("Search term: ", "-\"my first phrase\"", "-\"my first phrase\""),
        Arguments.of("Search term: ", "+\"my second phrase\"", "+\"my second phrase\""),
        Arguments.of("Search term: ", "\"my second phrase\"", "+\"my second phrase\""),
        Arguments.of(
            "Search term: ", "-covid-19 -type-2-diabetes", "-\"covid-19\"-\"type-2-diabetes\""),
        Arguments.of(
            "Search term: ", "+covid-19 +type-2-diabetes", "+\"covid-19\"+\"type-2-diabetes\""),
        Arguments.of(
            "Search term: ", "covid-19 type-2-diabetes", "+\"covid-19\"+\"type-2-diabetes\""),
        Arguments.of("Search term: ", "-diabet", "-diabet"),
        Arguments.of("Search term: ", "+diabet", "+diabet*"),
        Arguments.of("Search term: ", "diabet", "+diabet*"),
        Arguments.of("Search term: ", "+++diabet", "+diabet*"),
        Arguments.of("Search term: ", "---diabet", "-diabet"),
        Arguments.of("Search term: ", "+diabet is from", "+diabet*"),
        Arguments.of("Search term: ", "+\"diabet is from\"", "+\"diabet is from\""),
        Arguments.of("Search term: ", "-\"diabet is from\"", "-\"diabet is from\""),
        Arguments.of("Search term: ", "how* is *it diabet ", "+diabet*"),
        Arguments.of("Search term: ", "how* is *it +di* ", ""),
        Arguments.of("Search term: ", "how* is *it dia ", "+dia*"));
  }

  private static List<String> getStopWords() {
    // SELECT regexp_replace(group_concat(value),',','|') FROM
    // INFORMATION_SCHEMA.INNODB_FT_DEFAULT_STOPWORD;
    String stopwords =
        "a|about|an|are|as|at|be|by|com|de|en|for|from|how|i|in|is|it|la|of|on|or|that|the|this|to|was|what|when|where|who|will|with|und|the|www";
    return Arrays.asList(stopwords.split("\\|"));
  }
}

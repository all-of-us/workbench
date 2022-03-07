package org.pmiops.workbench.cohortbuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.cache.MySQLStopWords;
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.CBDataFilterDao;
import org.pmiops.workbench.cdr.dao.CriteriaMenuDao;
import org.pmiops.workbench.cdr.dao.DomainCardDao;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapperImpl;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CohortBuilderServiceImplTest {

  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @Mock private BigQueryService bigQueryService;
  @Mock private CohortQueryBuilder cohortQueryBuilder;
  @Autowired private CBCriteriaAttributeDao cbCriteriaAttributeDao;
  @Autowired private CBCriteriaDao cbCriteriaDao;
  @Autowired private CriteriaMenuDao criteriaMenuDao;
  @Autowired private CBDataFilterDao cbDataFilterDao;
  @Autowired private DomainCardDao domainCardDao;
  @Autowired private PersonDao personDao;
  @Autowired private SurveyModuleDao surveyModuleDao;
  @Autowired private CohortBuilderMapper cohortBuilderMapper;
  @Mock private Provider<MySQLStopWords> mySQLStopWordsProvider;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, CohortBuilderMapperImpl.class})
  @MockBean({WorkspaceAuthService.class})
  static class Configuration {}

  private CohortBuilderServiceImpl cohortBuilderService;

  @BeforeEach
  public void setUp() {

    cohortBuilderService =
        new CohortBuilderServiceImpl(
            bigQueryService,
            cohortQueryBuilder,
            cbCriteriaAttributeDao,
            cbCriteriaDao,
            criteriaMenuDao,
            cbDataFilterDao,
            domainCardDao,
            personDao,
            surveyModuleDao,
            cohortBuilderMapper,
            mySQLStopWordsProvider);

    MySQLStopWords mySQLStopWords = new MySQLStopWords(getStopWords());
    when(mySQLStopWordsProvider.get()).thenReturn(mySQLStopWords);
  }
private static List<String> testCases = new ArrayList<>();

  @ParameterizedTest
  @MethodSource("getModifyTermMatchParameters")
  void modifyTermMatch(String term, String expected) {

    String actual = cohortBuilderService.modifyTermMatch(term);
    // simplified
    String[] terms = term.split("\\W+");
    String simplified = "+" + String.join("+", terms) + "*";

    testCases.add(String.format("|%s|%s|",term,actual));
    System.out.println("|term|modifiedTerm|");
    System.out.println("|-|-|");
    testCases.forEach(System.out::println);

    // assertThat(actual).isEqualTo(expected);
  }

  private static Stream<Arguments> getModifyTermMatchParameters() {

    return Stream.of(
        Arguments.of("lun", "+lun*"),
        Arguments.of("lung can", "+\"lung\"+can*"),
        Arguments.of("001", "Like '001%'"),
        Arguments.of("001.1", "Like '001.1%'"),
        Arguments.of("heart attack rate", "+\"heart\"+\"attack\"+rate*"),
        Arguments.of("lun* can*", ""),
        Arguments.of("lun* -cancer", ""),
        Arguments.of("lung -can", ""),
        Arguments.of("+lung +cancer", ""),
        Arguments.of("\"lung cancer\"", ""),
        Arguments.of("type * diabetes", "+\"type * diabetes\""),
        Arguments.of("type 2 diabetes", "+\"type\"+diabetes*"),
        Arguments.of(
            "type 2 diabetes and heart attack ", "+\"type\"+\"diabetes\"+\"heart\"+attack*"),
        Arguments.of("disorder of the eye", "+\"disorder\"+eye*"),
        Arguments.of("disorder    of    the   eye  ", "+\"disorder\"+eye*"));
  }

  private static List<String> getStopWords() {
    // SELECT * FROM INFORMATION_SCHEMA.INNODB_FT_DEFAULT_STOPWORD;
    String stopwords =
        "a\n" + "about\n" + "an\n" + "are\n" + "as\n" + "at\n" + "be\n" + "by\n" + "com\n" + "de\n"
            + "en\n" + "for\n" + "from\n" + "how\n" + "i\n" + "in\n" + "is\n" + "it\n" + "la\n"
            + "of\n" + "on\n" + "or\n" + "that\n" + "the\n" + "this\n" + "to\n" + "was\n" + "what\n"
            + "when\n" + "where\n" + "who\n" + "will\n" + "with\n" + "und\n" + "the\n" + "www\n";
    return Arrays.asList(stopwords.split("\n"));
  }
}

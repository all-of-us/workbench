package org.pmiops.workbench.cdr.dao;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.IntStream;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.model.DbCardCount;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class CustomCBCriteriaDaoImpl implements CustomCBCriteriaDao {

  public static class QueryAndParameters {

    private final String query;
    private final MapSqlParameterSource parameters;

    QueryAndParameters(String query, MapSqlParameterSource parameters) {
      this.query = query;
      this.parameters = parameters;
    }

    public String getQuery() {
      return query;
    }

    public MapSqlParameterSource getParameters() {
      return parameters;
    }
  }

  private static final String SQL_DB_CDR_NAME = "DB_CDR_NAME";
  private static final String SQL_ENDS_WITH = "SQL_ENDS_WITH";

  // SQL bind parameter variables
  private static final String BIND_VAR_DOMAIN = "domain";
  private static final String BIND_VAR_STANDARD = "standard";
  private static final String BIND_VAR_HIERARCHIES = "hierarchies";
  private static final String BIND_VAR_TERM = "term";
  private static final String BIND_VAR_TYPE = "type";
  private static final String BIND_VAR_TYPES = "types";
  private static final String BIND_VAR_ID = "id";

  // SQL regexp replace variable (for IN clause)
  private static final String VAR_IN_DOMAINS = "domains";

  // SQL snips
  // essentially to match a non permitted ending character for name
  private static final String DYNAMIC_SQL_REGEXP = "upper(name) regexp upper(%s)";

  private static final String LIMIT_OFFSET = "limit %s offset %s\n";

  private static final String OR = "\nor\n";

  private static final String MATCH_FULL_TEXT_DOMAIN =
      " and match(full_text) against(concat('+[', :"
          + BIND_VAR_DOMAIN
          + ", '_rank1]') in boolean mode)\n";

  private static final String MATCH_FULL_TEXT_DOMAIN_TERM =
      " and match(full_text) against(concat(:"
          + BIND_VAR_TERM
          + ", '+[', :"
          + BIND_VAR_DOMAIN
          + ", '_rank1]') in boolean mode)\n";

  private static final String DOMAIN_ID_IN_DOMAINS =
      " and domain_id in (:" + VAR_IN_DOMAINS + ")\n";

  private static final String MATCH_FULL_TEXT_SURVEY =
      " and match(full_text) against('+[survey_rank1]' in boolean mode)\n";

  private static final String MATCH_FULL_TEXT_SURVEY_TERM =
      " and match(full_text) against(concat(:"
          + BIND_VAR_TERM
          + ", '+[survey_rank1]') in boolean mode)\n";

  private static final String MATCH_ID_PATH =
      "and match(path) against(:" + BIND_VAR_ID + " in boolean mode) \n";

  private static final String CRITERIA_BY_DOMAIN_ENDS_WITH =
      "select *\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where is_standard = :"
          + BIND_VAR_STANDARD
          + "\n"
          + MATCH_FULL_TEXT_DOMAIN
          + "and type != :"
          + BIND_VAR_TYPE
          + "\n"
          + "and ("
          + SQL_ENDS_WITH
          + ")\n"
          + "order by est_count desc, name asc\n";

  private static final String CRITERIA_BY_DOMAIN_TERM_ENDS_WITH =
      "select *\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where is_standard = :"
          + BIND_VAR_STANDARD
          + "\n"
          + MATCH_FULL_TEXT_DOMAIN_TERM
          + "and type != :"
          + BIND_VAR_TYPE
          + "\n"
          + "and ("
          + SQL_ENDS_WITH
          + ")\n"
          + "order by est_count desc, name asc\n";

  private static final String AUTO_COMPLETE_ENDS_WITH =
      "select *\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where type in (:"
          + BIND_VAR_TYPES
          + ")\n"
          + "and is_standard = :"
          + BIND_VAR_STANDARD
          + "\n"
          + "and has_hierarchy in (:"
          + BIND_VAR_HIERARCHIES
          + ")\n"
          + MATCH_FULL_TEXT_DOMAIN
          + "and ("
          + SQL_ENDS_WITH
          + ")\n"
          + "order by est_count desc, name asc\n";

  private static final String AUTO_COMPLETE_TERM_ENDS_WITH =
      "select *\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where type in (:"
          + BIND_VAR_TYPES
          + ")\n"
          + "and is_standard = :"
          + BIND_VAR_STANDARD
          + "\n"
          + "and has_hierarchy in (:"
          + BIND_VAR_HIERARCHIES
          + ")\n"
          + MATCH_FULL_TEXT_DOMAIN_TERM
          + "and ("
          + SQL_ENDS_WITH
          + ")\n"
          + "order by est_count desc, name asc\n";

  private static final String DOMAIN_COUNTS_TERM_ENDS_WITH =
      "select \n"
          + "upper(substring_index(substring_index(full_text, '[', -1), '_rank1', 1)) as domainId\n"
          + ", upper(substring_index(substring_index(full_text, '[', -1), '_rank1', 1)) as name\n"
          + ", count(*) as count\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where match(full_text) against(:"
          + BIND_VAR_TERM
          + " in boolean mode)\n"
          + "and full_text like '%_rank1%'\n"
          + "and is_standard = :"
          + BIND_VAR_STANDARD
          + "\n"
          + DOMAIN_ID_IN_DOMAINS
          + "and ("
          + SQL_ENDS_WITH
          + ") \n"
          + "group by 1 "
          + "order by count desc";

  private static final String DOMAIN_COUNTS_ENDS_WITH =
      "select \n"
          + "upper(substring_index(substring_index(full_text, '[', -1), '_rank1', 1)) as domainId\n"
          + ", upper(substring_index(substring_index(full_text, '[', -1), '_rank1', 1)) as name\n"
          + ", count(*) as count\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where full_text like '%_rank1%'\n"
          + "and is_standard = :"
          + BIND_VAR_STANDARD
          + "\n"
          + DOMAIN_ID_IN_DOMAINS
          + "and ("
          + SQL_ENDS_WITH
          + ") \n"
          + "group by 1 "
          + "order by count desc";

  private static final String SURVEY_COUNTS_TERM_ENDS_WITH =
      "select 'SURVEY' as domainId"
          + ", name\n"
          + ", count\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria c \n"
          + "join(\n"
          + "select substring_index(path,'.',1) as survey_version_concept_id, count(*) as count\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where domain_id = 'SURVEY'\n"
          + "and subtype = 'QUESTION'\n"
          + "and concept_id in (select concept_id\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where domain_id = 'SURVEY' "
          + MATCH_FULL_TEXT_SURVEY_TERM
          + "and ("
          + SQL_ENDS_WITH
          + "))\n"
          + "group by survey_version_concept_id"
          + ") a on c.id = a.survey_version_concept_id \n"
          + "order by count desc, name asc";

  private static final String SURVEY_COUNTS_ENDS_WITH =
      "select 'SURVEY' as domainId"
          + ", name\n"
          + ", count\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria c \n"
          + "join(\n"
          + "select substring_index(path,'.',1) as survey_version_concept_id, count(*) as count\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where domain_id = 'SURVEY'\n"
          + "and subtype = 'QUESTION'\n"
          + "and concept_id in (select concept_id\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where domain_id = 'SURVEY' "
          + MATCH_FULL_TEXT_SURVEY
          + "and ("
          + SQL_ENDS_WITH
          + "))\n"
          + "group by survey_version_concept_id"
          + ") a on c.id = a.survey_version_concept_id "
          + " order by count desc, name asc";

  private static final String SURVEY_QUESTION_TERM_ENDS_WITH =
      "select *\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria c1 \n"
          + "where c1.domain_id = 'SURVEY' \n"
          + "and c1.subtype = 'QUESTION' \n"
          + "and c1.concept_id in (select concept_id from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria \n"
          + "where domain_id = 'SURVEY'\n"
          + MATCH_FULL_TEXT_SURVEY_TERM
          + "and ("
          + SQL_ENDS_WITH
          + "))\n"
          + "order by c1.est_count desc, name asc\n";

  private static final String SURVEY_QUESTION_ENDS_WITH =
      "select *\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria c1 \n"
          + "where c1.domain_id = 'SURVEY' \n"
          + "and c1.subtype = 'QUESTION' \n"
          + "and c1.concept_id in (select concept_id from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria \n"
          + "where domain_id = 'SURVEY'\n"
          + MATCH_FULL_TEXT_SURVEY
          + "and ("
          + SQL_ENDS_WITH
          + ")) \n"
          + "order by c1.est_count desc, name asc\n";

  private static final String SURVEY_QUESTION_BY_PATH_TERM_ENDS_WITH =
      "select *\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria c1 \n"
          + "where c1.domain_id = 'SURVEY' \n"
          + "and c1.subtype = 'QUESTION' \n"
          + "and c1.full_text like '%[survey_rank1]%'\n"
          + "and c1.concept_id in (select concept_id from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria \n"
          + "where domain_id = 'SURVEY' \n"
          + MATCH_FULL_TEXT_SURVEY_TERM
          + MATCH_ID_PATH
          + "and ("
          + SQL_ENDS_WITH
          + ")) \n"
          + "order by c1.est_count desc, name asc\n";

  private static final String SURVEY_QUESTION_BY_PATH_ENDS_WITH =
      "select *\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria c1 \n"
          + "where c1.domain_id = 'SURVEY' \n"
          + "and c1.subtype = 'QUESTION' \n"
          + "and c1.full_text like '%[survey_rank1]%'\n"
          + "and c1.concept_id in (select concept_id from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria \n"
          + "where domain_id = 'SURVEY' \n"
          + MATCH_ID_PATH
          + "and ("
          + SQL_ENDS_WITH
          + ")) \n"
          + "order by c1.est_count desc, name asc\n";

  @Autowired private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Override
  public Page<DbCriteria> findCriteriaByDomainAndNameEndsWithAndStandardAndNotType(
      String domain, List<String> endsWithList, Boolean standard, String type, Pageable page) {
    Object[][] params = {
      {BIND_VAR_DOMAIN, domain},
      {BIND_VAR_STANDARD, standard},
      {BIND_VAR_TYPE, type}
    };
    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(CRITERIA_BY_DOMAIN_ENDS_WITH, params, endsWithList);
    return new PageImpl<>(
        queryForPaginatedList(page, queryAndParameters),
        page,
        Objects.requireNonNull(count(queryAndParameters)));
  }

  @Override
  public Page<DbCriteria> findCriteriaByDomainAndNameEndsWithAndTermAndStandardAndNotType(
      String domain,
      String term,
      List<String> endsWithList,
      Boolean standard,
      String type,
      Pageable page) {
    Object[][] params = {
      {BIND_VAR_DOMAIN, domain},
      {BIND_VAR_STANDARD, standard},
      {BIND_VAR_TERM, term},
      {BIND_VAR_TYPE, type}
    };
    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(CRITERIA_BY_DOMAIN_TERM_ENDS_WITH, params, endsWithList);
    return new PageImpl<>(
        queryForPaginatedList(page, queryAndParameters),
        page,
        Objects.requireNonNull(count(queryAndParameters)));
  }

  @Override
  public List<DbCriteria> findCriteriaByDomainAndTypeAndStandardAndNameEndsWith(
      String domain,
      List<String> types,
      Boolean standard,
      List<Boolean> hierarchies,
      List<String> endsWithList,
      Pageable page) {
    Object[][] params = {
      {BIND_VAR_DOMAIN, domain},
      {BIND_VAR_TYPES, types},
      {BIND_VAR_STANDARD, standard},
      {BIND_VAR_HIERARCHIES, hierarchies}
    };
    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(AUTO_COMPLETE_ENDS_WITH, params, endsWithList);
    return new PageImpl<>(
            queryForPaginatedList(page, queryAndParameters),
            page,
            Objects.requireNonNull(count(queryAndParameters)))
        .getContent();
  }

  @Override
  public List<DbCriteria> findCriteriaByDomainAndTypeAndStandardAndTermAndNameEndsWith(
      String domain,
      List<String> types,
      Boolean standard,
      List<Boolean> hierarchies,
      String term,
      List<String> endsWithList,
      Pageable page) {
    Object[][] params = {
      {BIND_VAR_DOMAIN, domain},
      {BIND_VAR_TYPES, types},
      {BIND_VAR_STANDARD, standard},
      {BIND_VAR_HIERARCHIES, hierarchies},
      {BIND_VAR_TERM, term}
    };
    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(AUTO_COMPLETE_TERM_ENDS_WITH, params, endsWithList);
    return new PageImpl<>(
            queryForPaginatedList(page, queryAndParameters),
            page,
            Objects.requireNonNull(count(queryAndParameters)))
        .getContent();
  }

  @Override
  public List<DbCardCount> findDomainCountsByNameEndsWithAndStandardAndDomains(
      List<String> endsWithList, Boolean standard, List<String> domains) {
    Object[][] params = {{BIND_VAR_STANDARD, standard}, {VAR_IN_DOMAINS, domains}};
    return queryForDbCardCountList(
        generateQueryAndParameters(DOMAIN_COUNTS_ENDS_WITH, params, endsWithList));
  }

  @Override
  public List<DbCardCount> findDomainCountsByTermAndNameEndsWithAndStandardAndDomains(
      String term, List<String> endsWithList, Boolean standard, List<String> domains) {
    Object[][] params = {
      {BIND_VAR_STANDARD, standard},
      {BIND_VAR_TERM, term},
      {VAR_IN_DOMAINS, domains}
    };
    return queryForDbCardCountList(
        generateQueryAndParameters(DOMAIN_COUNTS_TERM_ENDS_WITH, params, endsWithList));
  }

  @Override
  public List<DbCardCount> findSurveyCountsByNameEndsWith(List<String> endsWithList) {
    return queryForDbCardCountList(
        generateQueryAndParameters(SURVEY_COUNTS_ENDS_WITH, null, endsWithList));
  }

  @Override
  public List<DbCardCount> findSurveyCountsByTermAndNameEndsWith(
      String term, List<String> endsWithList) {
    Object[][] params = {{BIND_VAR_TERM, term}};
    return queryForDbCardCountList(
        generateQueryAndParameters(SURVEY_COUNTS_TERM_ENDS_WITH, params, endsWithList));
  }

  @Override
  public Page<DbCriteria> findSurveyQuestionByNameEndsWith(
      List<String> endsWithList, Pageable page) {

    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(SURVEY_QUESTION_ENDS_WITH, null, endsWithList);

    return new PageImpl<>(
        queryForPaginatedList(page, queryAndParameters),
        page,
        Objects.requireNonNull(count(queryAndParameters)));
  }

  @Override
  public Page<DbCriteria> findSurveyQuestionByTermAndNameEndsWith(
      String term, List<String> endsWithList, Pageable page) {
    Object[][] params = {{BIND_VAR_TERM, term}};

    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(SURVEY_QUESTION_TERM_ENDS_WITH, params, endsWithList);

    return new PageImpl<>(
        queryForPaginatedList(page, queryAndParameters),
        page,
        Objects.requireNonNull(count(queryAndParameters)));
  }

  @Override
  public Page<DbCriteria> findSurveyQuestionByPathAndNameEndsWith(
      Long id, List<String> endsWithList, Pageable page) {
    Object[][] params = {{BIND_VAR_ID, id}};

    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(SURVEY_QUESTION_BY_PATH_ENDS_WITH, params, endsWithList);

    return new PageImpl<>(
        queryForPaginatedList(page, queryAndParameters),
        page,
        Objects.requireNonNull(count(queryAndParameters)));
  }

  @Override
  public Page<DbCriteria> findSurveyQuestionByPathAndTermAndNameEndsWith(
      Long id, String term, List<String> endsWithList, Pageable page) {
    Object[][] params = {{BIND_VAR_ID, id}, {BIND_VAR_TERM, term}};

    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(SURVEY_QUESTION_BY_PATH_TERM_ENDS_WITH, params, endsWithList);

    return new PageImpl<>(
        queryForPaginatedList(page, queryAndParameters),
        page,
        Objects.requireNonNull(count(queryAndParameters)));
  }

  protected QueryAndParameters generateQueryAndParameters(
      String sql, Object[][] params, List<String> endsWithList) {

    long cdrVersionId = CdrVersionContext.getCdrVersion().getCdrVersionId();
    Optional<DbCdrVersion> cdrVersionOptional = cdrVersionDao.findById(cdrVersionId);
    DbCdrVersion dbCdrVersion =
        cdrVersionOptional.orElseThrow(
            () ->
                new BadRequestException(
                    String.format("CDR version with ID %s not found", cdrVersionId)));

    MapSqlParameterSource parameters = new MapSqlParameterSource();
    if (params != null) {
      Arrays.stream(params).forEach(param -> parameters.addValue(param[0].toString(), param[1]));
    }

    StringJoiner joiner = new StringJoiner(OR);
    IntStream.range(0, endsWithList.size())
        .forEach(
            idx -> {
              String parameterName = "endsWith" + idx;
              parameters.addValue(
                  parameterName, endsWithList.get(idx).substring(1) + "[^a-z0-9]?$");
              joiner.add(String.format(DYNAMIC_SQL_REGEXP, ":" + parameterName));
            });

    return new QueryAndParameters(
        sql.replaceAll(SQL_DB_CDR_NAME, dbCdrVersion.getCdrDbName())
            .replaceAll(SQL_ENDS_WITH, joiner.toString()),
        parameters);
  }

  @NotNull
  private List<DbCriteria> queryForPaginatedList(
      Pageable page, QueryAndParameters queryAndParameters) {
    return namedParameterJdbcTemplate.query(
        queryAndParameters.getQuery()
            + String.format(LIMIT_OFFSET, page.getPageSize(), page.getOffset()),
        queryAndParameters.getParameters(),
        new DBCriteriaRowMapper());
  }

  @NotNull
  private List<DbCardCount> queryForDbCardCountList(QueryAndParameters queryAndParameters) {
    return namedParameterJdbcTemplate.query(
        queryAndParameters.getQuery(),
        queryAndParameters.getParameters(),
        new DbCardCountRowMapper());
  }

  @Nullable
  private Long count(QueryAndParameters queryAndParameters) {
    return namedParameterJdbcTemplate.queryForObject(
        queryAndParameters.getQuery().replace("*", "count(*)"),
        queryAndParameters.getParameters(),
        Long.class);
  }

  private static class DBCriteriaRowMapper implements RowMapper<DbCriteria> {
    @Override
    public DbCriteria mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
      return DbCriteria.builder()
          .addId(rs.getLong("id"))
          .addParentId(rs.getLong("parent_id"))
          .addDomainId(rs.getString("domain_id"))
          .addStandard(rs.getBoolean("is_standard"))
          .addType(rs.getString("type"))
          .addSubtype(rs.getString("subtype"))
          .addConceptId(rs.getString("concept_id"))
          .addCode(rs.getString("code"))
          .addName(rs.getString("name"))
          .addValue(rs.getString("value"))
          .addCount(rs.getLong("est_count"))
          .addGroup(rs.getBoolean("is_group"))
          .addSelectable(rs.getBoolean("is_selectable"))
          .addAttribute(rs.getBoolean("has_attribute"))
          .addHierarchy(rs.getBoolean("has_hierarchy"))
          .addAncestorData(rs.getBoolean("has_ancestor_data"))
          .addPath(rs.getString("path"))
          .addParentCount(rs.getLong("rollup_count"))
          .addChildCount(rs.getLong("item_count"))
          .addSynonyms(rs.getString("display_synonyms"))
          .build();
    }
  }

  private static class DbCardCountRowMapper implements RowMapper<DbCardCount> {
    @Override
    public DbCardCount mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new DbCardCountImpl(
          rs.getString("domainId"), rs.getString("name"), rs.getLong("count"));
    }
  }

  public static class DbCardCountImpl implements DbCardCount {

    private final String domainId;
    private final String name;
    private final long count;

    public DbCardCountImpl(String domainId, String name, long count) {
      this.domainId = domainId;
      this.name = name;
      this.count = count;
    }

    @Override
    public String getDomainId() {
      return domainId;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Long getCount() {
      return count;
    }
  }
}

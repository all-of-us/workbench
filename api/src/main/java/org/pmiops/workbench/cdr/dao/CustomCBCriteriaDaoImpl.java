package org.pmiops.workbench.cdr.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.model.DbCardCount;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    @Override
    public String toString() {
      return "QueryAndParameters{" + "query='" + query + '\'' + ", parameters=" + parameters + '}';
    }
  }

  private static final String SQL_DB_CDR_NAME = "DB_CDR_NAME";
  private static final String SQL_ENDS_WITH = "SQL_ENDS_WITH";
  private static final String BIND_VAR_DOMAIN = "domain";
  private static final String BIND_VAR_STANDARD = "standard";
  private static final String BIND_VAR_TERM = "term";
  private static final String BIND_VAR_TYPE = "type";
  private static final String BIND_VAR_IN_DOMAIN_LIST = "domains";

  // SQL snips
  private static final String DYNAMIC_SQL = "upper(name) like upper(%s)";
  private static final String LIMIT_OFFSET = "limit %s offset %s\n";
  private static final String OR = "\nor\n";

  private static final String CRITERIA_BY_DOMAIN_ENDS_WITH_NO_TERM =
      "select *\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where is_standard = :"
          + BIND_VAR_STANDARD
          + "\n"
          + "and match(full_text) against(concat('+[', :"
          + BIND_VAR_DOMAIN
          + ", '_rank1]') in boolean mode)\n"
          + "and ("
          + SQL_ENDS_WITH
          + ")\n"
          + "order by est_count desc, name asc\n";

  private static final String CRITERIA_BY_DOMAIN_ENDS_WITH_AND_TERM =
      "select *\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where is_standard = :"
          + BIND_VAR_STANDARD
          + "\n"
          + "and match(full_text) against(concat(:"
          + BIND_VAR_TERM
          + ", '+[', :"
          + BIND_VAR_DOMAIN
          + ", '_rank1]') in boolean mode)\n"
          + "and ("
          + SQL_ENDS_WITH
          + ")\n"
          + "order by est_count desc, name asc\n";

  private static final String AUTO_COMPLETE_ENDS_WITH_NO_TERM =
      "select *\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where type = :"
          + BIND_VAR_TYPE
          + "\n"
          + "and is_standard = :"
          + BIND_VAR_STANDARD
          + "\n"
          + "and has_hierarchy = 1\n"
          + "and match(full_text) against(concat('+[', :"
          + BIND_VAR_DOMAIN
          + ", '_rank1]') in boolean mode)\n"
          + "and ("
          + SQL_ENDS_WITH
          + ")\n"
          + "order by est_count desc, name asc\n";

  private static final String AUTO_COMPLETE_ENDS_WITH_AND_TERM =
      "select *\n"
          + "from \n"
          + SQL_DB_CDR_NAME
          + ".cb_criteria\n"
          + "where type = :"
          + BIND_VAR_TYPE
          + "\n"
          + "and is_standard = :"
          + BIND_VAR_STANDARD
          + "\n"
          + "and has_hierarchy = 1\n"
          + "and match(full_text) against(concat(:"
          + BIND_VAR_TERM
          + ", '+[', :"
          + BIND_VAR_DOMAIN
          + ", '_rank1]') in boolean mode)\n"
          + "and ("
          + SQL_ENDS_WITH
          + ")\n"
          + "order by est_count desc, name asc\n";

  private static final String DOMAIN_COUNTS_ENDS_WITH_AND_TERM =
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
          + "and domain_id in (:"
          + BIND_VAR_IN_DOMAIN_LIST
          + ")\n"
          + "and ("
          + SQL_ENDS_WITH
          + ")\n"
          + "group by 1 "
          + "order by count desc";

  private static final String DOMAIN_COUNTS_ENDS_WITH_NO_TERM =
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
          + "and domain_id in (:"
          + BIND_VAR_IN_DOMAIN_LIST
          + ")\n"
          + "and ("
          + SQL_ENDS_WITH
          + ")\n"
          + "group by 1 "
          + "order by count desc";

  private static final String SURVEY_COUNTS_ENDS_WITH_AND_TERM =
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
          + "and match(full_text) against(concat(:"
          + BIND_VAR_TERM
          + ", '+[survey_rank1]') in boolean mode)\n"
          + "and ("
          + SQL_ENDS_WITH
          + "))\n"
          + "group by survey_version_concept_id"
          + ") a on c.id = a.survey_version_concept_id "
          + "order by count desc";

  private static final String SURVEY_COUNTS_ENDS_WITH_NO_TERM =
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
          + "and match(full_text) against('+[survey_rank1]' in boolean mode)\n"
          + "and ("
          + SQL_ENDS_WITH
          + "))\n"
          + "group by survey_version_concept_id"
          + ") a on c.id = a.survey_version_concept_id "
          + "order by count desc";

  @Autowired private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Override
  public Page<DbCriteria> findCriteriaByDomainAndStandardAndNameEndsWith(
      String domain, Boolean standard, List<String> endsWithList, Pageable page) {
    Object[][] params = {
      {BIND_VAR_DOMAIN, domain},
      {BIND_VAR_STANDARD, standard}
    };
    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(CRITERIA_BY_DOMAIN_ENDS_WITH_NO_TERM, params, endsWithList);
    return new PageImpl<>(
        queryForPaginatedList(page, queryAndParameters),
        page,
        Objects.requireNonNull(count(queryAndParameters)));
  }

  @Override
  public Page<DbCriteria> findCriteriaByDomainAndStandardAndTermAndNameEndsWith(
      String domain, Boolean standard, String term, List<String> endsWithList, Pageable page) {
    Object[][] params = {
      {BIND_VAR_DOMAIN, domain},
      {BIND_VAR_STANDARD, standard},
      {BIND_VAR_TERM, term}
    };
    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(CRITERIA_BY_DOMAIN_ENDS_WITH_AND_TERM, params, endsWithList);
    return new PageImpl<>(
        queryForPaginatedList(page, queryAndParameters),
        page,
        Objects.requireNonNull(count(queryAndParameters)));
  }

  @Override
  public List<DbCriteria> findCriteriaByDomainAndTypeAndStandardAndNameEndsWith(
      String domain, String type, Boolean standard, List<String> endsWithList, PageRequest page) {
    Object[][] params = {
      {BIND_VAR_DOMAIN, domain}, {BIND_VAR_TYPE, type}, {BIND_VAR_STANDARD, standard}
    };
    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(AUTO_COMPLETE_ENDS_WITH_NO_TERM, params, endsWithList);
    return new PageImpl<>(
            queryForPaginatedList(page, queryAndParameters),
            page,
            Objects.requireNonNull(count(queryAndParameters)))
        .getContent();
  }

  @Override
  public List<DbCriteria> findCriteriaByDomainAndTypeAndStandardAndTermAndNameEndsWith(
      String domain,
      String type,
      Boolean standard,
      String term,
      List<String> endsWithList,
      PageRequest page) {
    Object[][] params = {
      {BIND_VAR_DOMAIN, domain},
      {BIND_VAR_TYPE, type},
      {BIND_VAR_STANDARD, standard},
      {BIND_VAR_TERM, term}
    };
    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(AUTO_COMPLETE_ENDS_WITH_AND_TERM, params, endsWithList);
    return new PageImpl<>(
            queryForPaginatedList(page, queryAndParameters),
            page,
            Objects.requireNonNull(count(queryAndParameters)))
        .getContent();
  }

  @Override
  public List<DbCardCount> findDomainCountsByDomainsAndStandardAndNameEndsWith(
      List<String> domains, Boolean standard, List<String> endsWithList) {
    Object[][] params = {{BIND_VAR_STANDARD, standard}, {BIND_VAR_IN_DOMAIN_LIST, domains}};
    return queryForDbCardCountList(
        generateQueryAndParameters(DOMAIN_COUNTS_ENDS_WITH_NO_TERM, params, endsWithList));
  }

  @Override
  public List<DbCardCount> findDomainCountsByDomainsAndStandardAndTermAndNameEndsWith(
      List<String> domains, Boolean standard, String term, List<String> endsWithList) {
    Object[][] params = {
      {BIND_VAR_STANDARD, standard},
      {BIND_VAR_TERM, term},
      {BIND_VAR_IN_DOMAIN_LIST, domains}
    };
    return queryForDbCardCountList(
        generateQueryAndParameters(DOMAIN_COUNTS_ENDS_WITH_AND_TERM, params, endsWithList));
  }

  @Override
  public List<DbCardCount> findSurveyCountsAndNameEndsWith(List<String> endsWithList) {
    return queryForDbCardCountList(
        generateQueryAndParameters(SURVEY_COUNTS_ENDS_WITH_NO_TERM, null, endsWithList));
  }

  @Override
  public List<DbCardCount> findSurveyCountsAndTermAndNameEndsWith(
      String term, List<String> endsWithList) {
    Object[][] params = {{BIND_VAR_TERM, term}};
    return queryForDbCardCountList(
        generateQueryAndParameters(SURVEY_COUNTS_ENDS_WITH_AND_TERM, params, endsWithList));
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
              parameters.addValue(parameterName, endsWithList.get(idx));
              joiner.add(String.format(DYNAMIC_SQL, ":" + parameterName));
            });

    String tmpSql =
        sql.replaceAll(SQL_DB_CDR_NAME, dbCdrVersion.getCdrDbName())
            .replaceAll(SQL_ENDS_WITH, joiner.toString());

    return new QueryAndParameters(tmpSql, parameters);
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
    final List<DbCardCount> dbCardCountList =
        namedParameterJdbcTemplate.query(
            queryAndParameters.getQuery(),
            queryAndParameters.getParameters(),
            new DbCardCountRowMapper());
    return dbCardCountList;
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

    private String domainId;
    private String name;
    private long count;

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

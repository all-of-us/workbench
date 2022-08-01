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
  }

  private static final String ENDS_WITH_WITHOUT_TERM =
      "select *\n"
          + "from %s.cb_criteria\n"
          + "where is_standard = :standard\n"
          + "and match(full_text) against(concat('+[', :domain, '_rank1]') in boolean mode)\n"
          + "and (%s)\n"
          + "order by est_count desc, name asc\n";

  private static final String ENDS_WITH_WITH_TERM =
      "select *\n"
          + "from %s.cb_criteria\n"
          + "where is_standard = :standard\n"
          + "and match(full_text) against(concat(:term, '+[', :domain, '_rank1]') in boolean mode)\n"
          + "and (%s)\n"
          + "order by est_count desc, name asc\n";

  private static final String AUTO_COMPLETE_ENDS_WITH_WITHOUT_TERM =
      "select *\n"
          + "from %s.cb_criteria\n"
          + "where type = :type\n"
          + "and standard = :standard\n"
          + "and hierarchy = 1\n"
          + "and match(full_text) against(concat('+[', :domain, '_rank1]') in boolean mode)\n"
          + "and (%s)\n"
          + "order by est_count desc, name asc\n";

  private static final String AUTO_COMPLETE_ENDS_WITH_WITH_TERM =
      "select *\n"
          + "from %s.cb_criteria\n"
          + "where type = :type\n"
          + "and standard = :standard\n"
          + "and hierarchy = 1\n"
          + "and match(full_text) against(concat(:term, '+[', :domain, '_rank1]') in boolean mode)\n"
          + "and (%s)\n"
          + "order by est_count desc, name asc\n";

  private static final String DYNAMIC_SQL = "upper(name) like upper(%s)";

  private static final String LIMIT_OFFSET = "limit %s offset %s\n";

  private static final String OR = "\nor\n";

  private static final String BIND_VAR_DOMAIN = "domain";
  private static final String BIND_VAR_STANDARD = "standard";
  private static final String BIND_VAR_TERM = "term";
  private static final String BIND_VAR_TYPE = "type";

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
        generateQueryAndParameters(ENDS_WITH_WITHOUT_TERM, params, endsWithList);
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
        generateQueryAndParameters(ENDS_WITH_WITH_TERM, params, endsWithList);
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
        generateQueryAndParameters(AUTO_COMPLETE_ENDS_WITH_WITHOUT_TERM, params, endsWithList);
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
        generateQueryAndParameters(AUTO_COMPLETE_ENDS_WITH_WITH_TERM, params, endsWithList);
    return new PageImpl<>(
            queryForPaginatedList(page, queryAndParameters),
            page,
            Objects.requireNonNull(count(queryAndParameters)))
        .getContent();
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
    Arrays.stream(params)
        .forEach(
            param -> {
              parameters.addValue(param[0].toString(), param[1]);
            });

    StringJoiner joiner = new StringJoiner(OR);
    IntStream.range(0, endsWithList.size())
        .forEach(
            idx -> {
              String endsWith = endsWithList.get(idx).replace("*", "%");
              String parameterName = "endsWith" + idx;
              parameters.addValue(parameterName, endsWith);
              joiner.add(String.format(DYNAMIC_SQL, ":" + parameterName));
            });

    return new QueryAndParameters(
        String.format(sql, dbCdrVersion.getCdrDbName(), joiner), parameters);
  }

  @NotNull
  private MapSqlParameterSource getMapSqlParameterSource(
      String domain, Boolean standard, String term) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("domain", domain).addValue("standard", standard);
    if (term != null) {
      parameters.addValue("term", term);
    }
    return parameters;
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
}

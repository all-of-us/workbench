package org.pmiops.workbench.elasticsearch

import com.google.common.truth.Truth.assertThat

import com.google.common.collect.ImmutableList
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Arrays
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.cdr.dao.CBCriteriaDao
import org.pmiops.workbench.cdr.model.CBCriteria
import org.pmiops.workbench.model.AttrName
import org.pmiops.workbench.model.Attribute
import org.pmiops.workbench.model.CriteriaSubType
import org.pmiops.workbench.model.CriteriaType
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.Modifier
import org.pmiops.workbench.model.ModifierType
import org.pmiops.workbench.model.Operator
import org.pmiops.workbench.model.SearchGroup
import org.pmiops.workbench.model.SearchGroupItem
import org.pmiops.workbench.model.SearchParameter
import org.pmiops.workbench.model.SearchRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ElasticFiltersTest {

    @Autowired
    private val cbCriteriaDao: CBCriteriaDao? = null
    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null

    private var leafParam2: SearchParameter? = null

    private fun icd9Criteria(): CBCriteria {
        return CBCriteria()
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .attribute(java.lang.Boolean.FALSE)
                .standard(false)
                .synonyms("[CONDITION_rank1]")
    }

    private fun drugCriteria(): CBCriteria {
        return CBCriteria()
                .domainId(DomainType.DRUG.toString())
                .type(CriteriaType.ATC.toString())
                .attribute(java.lang.Boolean.FALSE)
                .standard(true)
    }

    private fun basicsCriteria(): CBCriteria {
        return CBCriteria()
                .domainId(DomainType.SURVEY.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.SURVEY.toString())
                .attribute(java.lang.Boolean.FALSE)
                .standard(false)
                .synonyms("+[SURVEY_rank1]")
    }

    @Before
    fun setUp() {
        // Generate a simple test criteria tree
        // 1
        // | - 2
        // | - 3
        val icd9Parent = icd9Criteria().code("001").conceptId("771").group(true).selectable(false).parentId(0)
        saveCriteriaWithPath("", icd9Parent)

        val icd9Child1 = icd9Criteria()
                .code("001.002")
                .conceptId("772")
                .group(false)
                .selectable(true)
                .parentId(icd9Parent.id)
        saveCriteriaWithPath(icd9Parent.path, icd9Child1)
        val icd9Child2 = icd9Criteria()
                .code("001.003")
                .conceptId("773")
                .group(true)
                .selectable(true)
                .parentId(icd9Parent.id)
        saveCriteriaWithPath(icd9Parent.path, icd9Child2)

        // Singleton SNOMED code.
        cbCriteriaDao!!.save(
                CBCriteria()
                        .code("005")
                        .conceptId("775")
                        .domainId(DomainType.CONDITION.toString())
                        .type(CriteriaType.SNOMED.toString())
                        .attribute(java.lang.Boolean.FALSE)
                        .group(false)
                        .selectable(true)
                        .parentId(0)
                        .path(""))

        // Four node PPI tree, survey, question, 1 answer.
        val survey = basicsCriteria().code("006").group(true).selectable(true).parentId(0).conceptId("77")
        saveCriteriaWithPath("", survey)

        val question = basicsCriteria()
                .code("007")
                .conceptId("777")
                .group(true)
                .selectable(true)
                .parentId(survey.id)
        saveCriteriaWithPath(survey.path, question)

        val answer = basicsCriteria()
                .code("008")
                // Concept ID matches the question.
                .conceptId("7771")
                .group(false)
                .selectable(true)
                .parentId(question.id)
        saveCriteriaWithPath(question.path, answer)

        // drug tree
        // Use jdbcTemplate to create/insert data into the ancestor table
        // The codebase currently doesn't have a need to implement a DAO for this table
        jdbcTemplate!!.execute(
                "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)")
        jdbcTemplate.execute(
                "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (19069022, 21600009)")
        val drugParent = drugCriteria().code("A").conceptId("21600001").group(true).selectable(false).parentId(0)
        saveCriteriaWithPath("", drugParent)
        val drug1 = drugCriteria()
                .code("A01")
                .conceptId("21600002")
                .group(true)
                .selectable(true)
                .parentId(drugParent.id)
        saveCriteriaWithPath(drugParent.path, drug1)
        val drug2 = drugCriteria()
                .code("A01A")
                .conceptId("21600003")
                .group(true)
                .selectable(true)
                .parentId(drug1.id)
        saveCriteriaWithPath(drug1.path, drug2)
        val drug3 = drugCriteria()
                .code("A01AA")
                .conceptId("21600004")
                .group(true)
                .selectable(true)
                .parentId(drug2.id)
        saveCriteriaWithPath(drug2.path, drug3)
        val drug4 = drugCriteria()
                .code("9873")
                .conceptId("19069022")
                .group(false)
                .selectable(true)
                .type(CriteriaType.RXNORM.toString())
                .parentId(drug3.id)
        saveCriteriaWithPath(drug3.path, drug4)

        leafParam2 = SearchParameter()
                .conceptId(772L)
                .domain(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .ancestorData(false)
                .standard(false)
                .group(false)
    }

    @After
    fun tearDown() {
        // jdbcTemplate is used to create/insert data into the criteria ancestor table. The codebase
        // currently doesn't have a need to implement a DAO for this table. The @DirtiesContext
        // annotation seems to only recognized Hibernate persisted entities and their related tables.
        // So the following tear down method needs to drop to table to keep from colliding with
        // other test classes that also use this approach to create the cb_criteria_ancestor table. Not
        // implementing this drop causes subsequent test suite runs to fail with a
        // BadSqlGrammarException: StatementCallback; bad SQL grammar [create table
        // cb_criteria_ancestor(ancestor_id integer, descendant_id integer)]; nested exception is
        // org.h2.jdbc.JdbcSQLException: Table "CB_CRITERIA_ANCESTOR" already exists
        jdbcTemplate!!.execute("drop table cb_criteria_ancestor")
    }

    private fun singleNestedQuery(vararg inners: QueryBuilder): QueryBuilder {
        return singleNestedQueryOccurrences(1, *inners)
    }

    private fun singleNestedQueryOccurrences(n: Int, vararg inners: QueryBuilder): QueryBuilder {
        val b = QueryBuilders.boolQuery()
        for (`in` in inners) {
            b.filter(`in`)
        }
        return QueryBuilders.boolQuery()
                .filter(
                        QueryBuilders.boolQuery()
                                .should(
                                        QueryBuilders.functionScoreQuery(
                                                QueryBuilders.nestedQuery(
                                                        "events", QueryBuilders.constantScoreQuery(b), ScoreMode.Total))
                                                .setMinScore(n.toFloat())))
    }

    private fun nonNestedQuery(vararg inners: QueryBuilder): QueryBuilder {
        val innerBuilder = QueryBuilders.boolQuery()
        for (`in` in inners) {
            if (`in`.toString().contains("is_deceased") && `in`.toString().contains("birth_datetime")) {
                innerBuilder.should(`in`)
            } else {
                val b = QueryBuilders.boolQuery().filter(`in`)
                innerBuilder.should(b)
            }
        }
        return QueryBuilders.boolQuery().filter(innerBuilder)
    }

    private fun nonNestedMustNotQuery(vararg inners: QueryBuilder): QueryBuilder {
        val innerBuilder = QueryBuilders.boolQuery()
        for (`in` in inners) {
            val b = QueryBuilders.boolQuery().filter(`in`)
            innerBuilder.should(b)
        }
        return QueryBuilders.boolQuery().filter(innerBuilder).mustNot(innerBuilder)
    }

    @Test
    fun testLeafQuery() {
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(leafParam2))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772"))))
    }

    @Test
    fun testParentConceptQuery() {
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem()
                                                        .addSearchParametersItem(
                                                                SearchParameter()
                                                                        .conceptId(21600002L)
                                                                        .domain(DomainType.DRUG.toString())
                                                                        .type(CriteriaType.ATC.toString())
                                                                        .ancestorData(true)
                                                                        .standard(true)
                                                                        .group(true)))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery(
                                        "events.concept_id", ImmutableList.of("21600002", "21600009"))))
    }

    @Test
    fun testICD9Query() {
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem()
                                                        .addSearchParametersItem(
                                                                SearchParameter()
                                                                        .value("001")
                                                                        .conceptId(771L)
                                                                        .domain(DomainType.CONDITION.toString())
                                                                        .type(CriteriaType.ICD9CM.toString())
                                                                        .group(true)
                                                                        .ancestorData(false)
                                                                        .standard(false)))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery(
                                        "events.source_concept_id", ImmutableList.of("771", "772", "773"))))
    }

    @Test
    fun testICD9AndSnomedQuery() {
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem()
                                                        .addSearchParametersItem(
                                                                SearchParameter()
                                                                        .value("001")
                                                                        .conceptId(771L)
                                                                        .domain(DomainType.CONDITION.toString())
                                                                        .type(CriteriaType.ICD9CM.toString())
                                                                        .group(false)
                                                                        .ancestorData(false)
                                                                        .standard(false))
                                                        .addSearchParametersItem(
                                                                SearchParameter()
                                                                        .conceptId(477L)
                                                                        .domain(DomainType.CONDITION.toString())
                                                                        .type(CriteriaType.SNOMED.toString())
                                                                        .group(false)
                                                                        .ancestorData(false)
                                                                        .standard(true)))))

        val source = QueryBuilders.boolQuery()
        source.filter(QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("771")))
        val standard = QueryBuilders.boolQuery()
        standard.filter(QueryBuilders.termsQuery("events.concept_id", ImmutableList.of("477")))
        val expected = QueryBuilders.boolQuery()
                .filter(
                        QueryBuilders.boolQuery()
                                .should(
                                        QueryBuilders.functionScoreQuery(
                                                QueryBuilders.nestedQuery(
                                                        "events",
                                                        QueryBuilders.constantScoreQuery(source),
                                                        ScoreMode.Total))
                                                .setMinScore(1f))
                                .should(
                                        QueryBuilders.functionScoreQuery(
                                                QueryBuilders.nestedQuery(
                                                        "events",
                                                        QueryBuilders.constantScoreQuery(standard),
                                                        ScoreMode.Total))
                                                .setMinScore(1f)))
        assertThat(resp).isEqualTo(expected)
    }

    @Test
    fun testPPISurveyQuery() {
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem()
                                                        .addSearchParametersItem(
                                                                SearchParameter()
                                                                        .domain(DomainType.SURVEY.toString())
                                                                        .type(CriteriaType.PPI.toString())
                                                                        .subtype(CriteriaSubType.SURVEY.toString())
                                                                        .ancestorData(false)
                                                                        .standard(false)
                                                                        .group(true)
                                                                        .conceptId(77L)))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery(
                                        "events.source_concept_id", ImmutableList.of("77", "7771", "777"))))
    }

    @Test
    fun testPPIQuestionQuery() {
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem()
                                                        .addSearchParametersItem(
                                                                SearchParameter()
                                                                        .domain(DomainType.SURVEY.toString())
                                                                        .type(CriteriaType.PPI.toString())
                                                                        .subtype(CriteriaSubType.QUESTION.toString())
                                                                        .conceptId(777L)
                                                                        .ancestorData(false)
                                                                        .standard(false)
                                                                        .group(true)))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery(
                                        "events.source_concept_id", ImmutableList.of("7771", "777"))))
    }

    @Test
    fun testPPIAnswerQuery() {
        val attr = Attribute().name(AttrName.NUM).operator(Operator.EQUAL).addOperandsItem("1")
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem()
                                                        .addSearchParametersItem(
                                                                SearchParameter()
                                                                        .domain(DomainType.SURVEY.toString())
                                                                        .type(CriteriaType.PPI.toString())
                                                                        .subtype(CriteriaSubType.ANSWER.toString())
                                                                        .conceptId(7771L)
                                                                        .group(false)
                                                                        .ancestorData(false)
                                                                        .standard(false)
                                                                        .addAttributesItem(attr)))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("7771")),
                                QueryBuilders.rangeQuery("events.value_as_number").gte(1.0f).lte(1.0f)))
    }

    @Test
    fun testPPIAnswerQueryCat() {
        val attr = Attribute().name(AttrName.CAT).operator(Operator.IN).addOperandsItem("1")
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem()
                                                        .addSearchParametersItem(
                                                                SearchParameter()
                                                                        .domain(DomainType.SURVEY.toString())
                                                                        .type(CriteriaType.PPI.toString())
                                                                        .subtype(CriteriaSubType.ANSWER.toString())
                                                                        .conceptId(777L)
                                                                        .group(false)
                                                                        .ancestorData(false)
                                                                        .standard(false)
                                                                        .addAttributesItem(attr)))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("777")),
                                QueryBuilders.termsQuery(
                                        "events.value_as_source_concept_id", ImmutableList.of("1"))))
    }

    @Test
    fun testAgeAtEventModifierQuery() {
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem()
                                                        .addSearchParametersItem(leafParam2)
                                                        .addModifiersItem(
                                                                Modifier()
                                                                        .name(ModifierType.AGE_AT_EVENT)
                                                                        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                                                                        .addOperandsItem("18")))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772")),
                                QueryBuilders.rangeQuery("events.age_at_start").gte(18)))
    }

    @Test
    fun testDateModifierQuery() {
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem()
                                                        .addSearchParametersItem(leafParam2)
                                                        .addModifiersItem(
                                                                Modifier()
                                                                        .name(ModifierType.EVENT_DATE)
                                                                        .operator(Operator.BETWEEN)
                                                                        .addOperandsItem("12/25/1988")
                                                                        .addOperandsItem("12/27/1988")))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772")),
                                QueryBuilders.rangeQuery("events.start_date").gte("12/25/1988").lte("12/27/1988")))
    }

    @Test
    fun testVisitsModifierQuery() {
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem()
                                                        .addSearchParametersItem(leafParam2)
                                                        .addModifiersItem(
                                                                Modifier()
                                                                        .name(ModifierType.ENCOUNTERS)
                                                                        .operator(Operator.IN)
                                                                        .addOperandsItem("123")))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772")),
                                QueryBuilders.termsQuery("events.visit_concept_id", ImmutableList.of("123"))))
    }

    @Test
    fun testNumOfOccurrencesModifierQuery() {
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem()
                                                        .addSearchParametersItem(leafParam2)
                                                        .addModifiersItem(
                                                                Modifier()
                                                                        .name(ModifierType.NUM_OF_OCCURRENCES)
                                                                        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                                                                        .addOperandsItem("13")))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQueryOccurrences(
                                13, QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of("772"))))
    }

    @Test
    fun testHeightAnyQuery() {
        val conceptId = "903133"
        val heightAnyParam = SearchParameter()
                .conceptId(java.lang.Long.parseLong(conceptId))
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .standard(false)
                .ancestorData(false)
                .group(false)
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem().addSearchParametersItem(heightAnyParam))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of(conceptId))))
    }

    @Test
    fun testHeightEqualQuery() {
        val attr = Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList<T>("1"))
        val left = java.lang.Float.parseFloat(attr.getOperands().get(0))
        val right = java.lang.Float.parseFloat(attr.getOperands().get(0))
        val conceptId = "903133"
        val heightParam = SearchParameter()
                .conceptId(java.lang.Long.parseLong(conceptId))
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .group(false)
                .ancestorData(false)
                .standard(false)
                .addAttributesItem(attr)
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(heightParam))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of(conceptId)),
                                QueryBuilders.rangeQuery("events.value_as_number").gte(left).lte(right)))
    }

    @Test
    fun testWeightBetweenQuery() {
        val attr = Attribute()
                .name(AttrName.NUM)
                .operator(Operator.BETWEEN)
                .operands(Arrays.asList<T>("1", "2"))
        val left = java.lang.Float.parseFloat(attr.getOperands().get(0))
        val right = java.lang.Float.parseFloat(attr.getOperands().get(1))
        val conceptId = "903121"
        val weightParam = SearchParameter()
                .conceptId(java.lang.Long.parseLong(conceptId))
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .standard(false)
                .ancestorData(false)
                .group(false)
                .addAttributesItem(attr)
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(weightParam))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of(conceptId)),
                                QueryBuilders.rangeQuery("events.value_as_number").gte(left).lte(right)))
    }

    @Test
    fun testGenderQuery() {
        val conceptId = "8507"
        val genderParam = SearchParameter()
                .conceptId(java.lang.Long.parseLong(conceptId))
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.GENDER.toString())
                .standard(true)
                .ancestorData(false)
                .group(false)
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(genderParam))))
        assertThat(resp)
                .isEqualTo(
                        nonNestedQuery(
                                QueryBuilders.termsQuery("gender_concept_id", ImmutableList.of(conceptId))))
    }

    @Test
    fun testGenderExcludeQuery() {
        val conceptId = "8507"
        val genderParam = SearchParameter()
                .conceptId(java.lang.Long.parseLong(conceptId))
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.GENDER.toString())
                .group(false)
                .ancestorData(false)
                .standard(true)
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addExcludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(genderParam))))
        assertThat(resp)
                .isEqualTo(
                        nonNestedQuery(
                                QueryBuilders.termsQuery("gender_concept_id", ImmutableList.of(conceptId))))
    }

    @Test
    fun testGenderIncludeAndExcludeQuery() {
        val conceptId = "8507"
        val genderParam = SearchParameter()
                .conceptId(java.lang.Long.parseLong(conceptId))
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.GENDER.toString())
                .group(false)
                .ancestorData(false)
                .standard(true)
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(genderParam)))
                        .addExcludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(genderParam))))
        assertThat(resp)
                .isEqualTo(
                        nonNestedMustNotQuery(
                                QueryBuilders.termsQuery("gender_concept_id", ImmutableList.of(conceptId))))
    }

    @Test
    fun testRaceQuery() {
        val conceptId = "8515"
        val raceParam = SearchParameter()
                .conceptId(java.lang.Long.parseLong(conceptId))
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.RACE.toString())
                .group(false)
                .ancestorData(false)
                .standard(true)
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(raceParam))))
        assertThat(resp)
                .isEqualTo(
                        nonNestedQuery(
                                QueryBuilders.termsQuery("race_concept_id", ImmutableList.of(conceptId))))
    }

    @Test
    fun testEthnicityQuery() {
        val conceptId = "38003563"
        val ethParam = SearchParameter()
                .conceptId(java.lang.Long.parseLong(conceptId))
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.ETHNICITY.toString())
                .group(false)
                .ancestorData(false)
                .standard(true)
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(ethParam))))
        assertThat(resp)
                .isEqualTo(
                        nonNestedQuery(
                                QueryBuilders.termsQuery("ethnicity_concept_id", ImmutableList.of(conceptId))))
    }

    @Test
    fun testDeceasedQuery() {
        val deceasedParam = SearchParameter()
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.DECEASED.toString())
                .standard(true)
                .ancestorData(false)
                .group(false)
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem().addSearchParametersItem(deceasedParam))))
        assertThat(resp).isEqualTo(nonNestedQuery(QueryBuilders.termQuery("is_deceased", true)))
    }

    @Test
    fun testPregnancyQuery() {
        val conceptId = "903120"
        val operand = "12345"
        val attr = Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList<T>(operand))
        val pregParam = SearchParameter()
                .conceptId(java.lang.Long.parseLong(conceptId))
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .group(false)
                .ancestorData(false)
                .standard(false)
                .attributes(Arrays.asList<T>(attr))
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(pregParam))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery("events.source_concept_id", ImmutableList.of(conceptId)),
                                QueryBuilders.termsQuery("events.value_as_concept_id", ImmutableList.of(operand))))
    }

    @Test
    fun testMeasurementCategoricalQuery() {
        val conceptId = "3015813"
        val operand1 = "12345"
        val operand2 = "12346"
        val attr = Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(Arrays.asList<T>(operand1, operand2))
        val measParam = SearchParameter()
                .conceptId(java.lang.Long.parseLong(conceptId))
                .domain(DomainType.MEASUREMENT.toString())
                .type(CriteriaType.LOINC.toString())
                .group(false)
                .ancestorData(false)
                .standard(true)
                .attributes(Arrays.asList<T>(attr))
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(measParam))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery("events.concept_id", ImmutableList.of(conceptId)),
                                QueryBuilders.termsQuery(
                                        "events.value_as_concept_id", ImmutableList.of(operand1, operand2))))
    }

    @Test
    fun testVisitQuery() {
        val conceptId = "9202"
        val visitParam = SearchParameter()
                .conceptId(java.lang.Long.parseLong(conceptId))
                .domain(DomainType.VISIT.toString())
                .type(CriteriaType.VISIT.toString())
                .ancestorData(false)
                .standard(true)
                .group(false)
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(visitParam))))
        assertThat(resp)
                .isEqualTo(
                        singleNestedQuery(
                                QueryBuilders.termsQuery("events.concept_id", ImmutableList.of(conceptId))))
    }

    @Test
    fun testAgeQuery() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val left = now.minusYears(34).minusYears(1).toLocalDate()
        val right = now.minusYears(20).toLocalDate()
        val ethParam = SearchParameter()
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.AGE.toString())
                .group(false)
                .ancestorData(false)
                .standard(true)
                .addAttributesItem(
                        Attribute()
                                .name(AttrName.AGE)
                                .operator(Operator.BETWEEN)
                                .operands(Arrays.asList<T>("20", "34")))
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(SearchGroupItem().addSearchParametersItem(ethParam))))
        val ageBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("is_deceased", false))
                .filter(
                        QueryBuilders.rangeQuery("birth_datetime")
                                .gt(left)
                                .lte(right)
                                .format("yyyy-MM-dd"))
        assertThat(resp).isEqualTo(nonNestedQuery(ageBuilder))
    }

    @Test
    fun testAgeAndEthnicityQuery() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val left = now.minusYears(34).minusYears(1).toLocalDate()
        val right = now.minusYears(20).toLocalDate()
        val ageParam = SearchParameter()
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.AGE.toString())
                .group(false)
                .ancestorData(false)
                .standard(true)
                .addAttributesItem(
                        Attribute()
                                .name(AttrName.AGE)
                                .operator(Operator.BETWEEN)
                                .operands(Arrays.asList<T>("20", "34")))
        val conceptId = "38003563"
        val ethParam = SearchParameter()
                .conceptId(java.lang.Long.parseLong(conceptId))
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.ETHNICITY.toString())
                .group(false)
                .ancestorData(false)
                .standard(true)
                .conceptId(java.lang.Long.parseLong(conceptId))
        val resp = ElasticFilters.fromCohortSearch(
                cbCriteriaDao,
                SearchRequest()
                        .addIncludesItem(
                                SearchGroup()
                                        .addItemsItem(
                                                SearchGroupItem()
                                                        .searchParameters(Arrays.asList<T>(ageParam, ethParam)))))

        val ageBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("is_deceased", false))
                .filter(
                        QueryBuilders.rangeQuery("birth_datetime")
                                .gt(left)
                                .lte(right)
                                .format("yyyy-MM-dd"))
        assertThat(resp)
                .isEqualTo(
                        nonNestedQuery(
                                ageBuilder,
                                QueryBuilders.termsQuery("ethnicity_concept_id", ImmutableList.of(conceptId))))
    }

    private fun saveCriteriaWithPath(path: String, criteria: CBCriteria) {
        cbCriteriaDao!!.save(criteria)
        val pathEnd = criteria.id.toString()
        criteria.path(if (path.isEmpty()) pathEnd else "$path.$pathEnd")
        cbCriteriaDao.save(criteria)
    }
}

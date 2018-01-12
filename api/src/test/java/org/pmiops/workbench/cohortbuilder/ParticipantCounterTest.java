package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@Import({ParticipantCounter.class, QueryBuilderFactory.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class ParticipantCounterTest {

    @Autowired
    ParticipantCounter participantCounter;

    @Test
    public void buildParticipantCounterQuery_BothIncludesAndExcludesEmpty() throws Exception {

        try {
            participantCounter.buildParticipantCounterQuery(new SearchRequest());
        } catch (BadRequestException e) {
            assertEquals("Invalid SearchRequest: includes[] and excludes[] cannot both be empty", e.getMessage());
        }
    }

    @Test
    public void buildParticipantCounterQuery_ExcludesWithoutIncludes() throws Exception {

        String genderNamedParameter = "";
        SearchParameter parameter1 = new SearchParameter()
                .subtype("GEN")
                .conceptId(8507L);

        SearchGroupItem searchGroupItem1 = new SearchGroupItem()
                .type("DEMO")
                .addSearchParametersItem(parameter1);

        SearchGroup searchGroup1 = new SearchGroup()
                .addItemsItem(searchGroupItem1);

        SearchRequest request = new SearchRequest()
                .addExcludesItem(searchGroup1);

        QueryJobConfiguration actualRequest = participantCounter.buildParticipantCounterQuery(request);

        for (String key : actualRequest.getNamedParameters().keySet()) {
            if (key.startsWith("gen")) {
                genderNamedParameter = key;
            }
        }

        final String expectedSql = "select count(distinct person_id) as count\n" +
                "from `${projectId}.${dataSetId}.person` person\n" +
                "where\n" +
                "person.person_id in (select distinct person_id\n" +
                "from `${projectId}.${dataSetId}.person` p\n" +
                "where p.gender_concept_id = @" + genderNamedParameter + "\n" +
                ")\n";

        assertEquals(expectedSql, actualRequest.getQuery());

        assertEquals("8507",
                actualRequest
                        .getNamedParameters()
                        .get(genderNamedParameter)
                        .getValue());
    }

    @Test
    public void buildParticipantCounterQuery() throws Exception {

        String genderNamedParameter = "";
        String conditionNamedParameter = "";
        String procedureNamedParameter = "";
        String cmICD9NamedParameter = "";
        String procICD9NamedParameter = "";
        String cmCPTNamedParameter = "";
        String procCPTNamedParameter = "";

        SearchParameter parameter1 = new SearchParameter()
                .domain("Condition")
                .value("001.1");
        SearchParameter parameter2 = new SearchParameter()
                .subtype("GEN")
                .conceptId(8507L);
        SearchParameter parameter3 = new SearchParameter()
                .domain("Procedure")
                .value("001.2");

        SearchGroupItem searchGroupItem1 = new SearchGroupItem()
                .type("ICD9")
                .addSearchParametersItem(parameter1);
        SearchGroupItem searchGroupItem2 = new SearchGroupItem()
                .type("DEMO")
                .addSearchParametersItem(parameter2);
        SearchGroupItem searchGroupItem3 = new SearchGroupItem()
                .type("CPT")
                .addSearchParametersItem(parameter3);

        SearchGroup searchGroup1 = new SearchGroup()
                .addItemsItem(searchGroupItem1);
        SearchGroup searchGroup2 = new SearchGroup()
                .addItemsItem(searchGroupItem2);
        SearchGroup searchGroup3 = new SearchGroup()
                .addItemsItem(searchGroupItem3);

        SearchRequest request = new SearchRequest()
                .addIncludesItem(searchGroup1)
                .addIncludesItem(searchGroup2)
                .addExcludesItem(searchGroup3);

        QueryJobConfiguration actualRequest = participantCounter.buildParticipantCounterQuery(request);

        for (String key : actualRequest.getNamedParameters().keySet()) {
            if (key.startsWith("gen")) {
                genderNamedParameter = key;
            } else if (key.startsWith("Condition")){
                conditionNamedParameter = key;
            } else if (key.startsWith("Procedure")) {
                procedureNamedParameter = key;
            } else if (key.startsWith("cmICD9")) {
                cmICD9NamedParameter = key;
            } else if (key.startsWith("procICD9")) {
                procICD9NamedParameter = key;
            } else if (key.startsWith("cmCPT")) {
                cmCPTNamedParameter = key;
            } else if (key.startsWith("procCPT")) {
                procCPTNamedParameter = key;
            }
        }

        final String expectedSql = "select count(distinct person_id) as count\n" +
                "from `${projectId}.${dataSetId}.person` person\n" +
                "where\n" +
                "person.person_id in (select person_id\n" +
                "from `${projectId}.${dataSetId}.person` p\n" +
                "where person_id in (select distinct person_id\n" +
                "from `${projectId}.${dataSetId}.condition_occurrence` a, `${projectId}.${dataSetId}.concept` b\n" +
                "where a.condition_source_concept_id = b.concept_id\n" +
                "and b.vocabulary_id in (@" + cmICD9NamedParameter + ",@" + procICD9NamedParameter + ")\n" +
                "and b.concept_code in unnest(@" + conditionNamedParameter + ")\n" +
                ")\n" +
                ")\n" +
                "and person.person_id in (select distinct person_id\n" +
                "from `${projectId}.${dataSetId}.person` p\n" +
                "where p.gender_concept_id = @" + genderNamedParameter + "\n" +
                ")\n" +
                "and not exists\n" +
                "(select 'x' from\n" +
                "(select person_id\n" +
                "from `${projectId}.${dataSetId}.person` p\n" +
                "where person_id in (select distinct person_id\n" +
                "from `${projectId}.${dataSetId}.procedure_occurrence` a, `${projectId}.${dataSetId}.concept` b\n" +
                "where a.procedure_source_concept_id = b.concept_id\n" +
                "and b.vocabulary_id in (@" + cmCPTNamedParameter + ",@" + procCPTNamedParameter + ")\n" +
                "and b.concept_code in unnest(@" + procedureNamedParameter + ")\n" +
                ")\n" +
                ")\n" +
                "x where x.person_id = person.person_id)\n";

        assertEquals(expectedSql, actualRequest.getQuery());

        assertEquals("8507",
                actualRequest
                        .getNamedParameters()
                        .get(genderNamedParameter)
                        .getValue());

        assertEquals(parameter1.getValue(),
                actualRequest
                        .getNamedParameters()
                        .get(conditionNamedParameter)
                        .getArrayValues().get(0).getValue());
    }

    @Test
    public void buildParticipantIdQuery() throws Exception {

        String genderNamedParameter = "";
        String conditionNamedParameter = "";
        String procedureNamedParameter = "";
        String cmICD9NamedParameter = "";
        String procICD9NamedParameter = "";
        String cmCPTNamedParameter = "";
        String procCPTNamedParameter = "";

        SearchParameter parameter1 = new SearchParameter()
                .domain("Condition")
                .value("001.1");
        SearchParameter parameter2 = new SearchParameter()
                .subtype("GEN")
                .conceptId(8507L);
        SearchParameter parameter3 = new SearchParameter()
                .domain("Procedure")
                .value("001.2");

        SearchGroupItem searchGroupItem1 = new SearchGroupItem()
                .type("ICD9")
                .addSearchParametersItem(parameter1);
        SearchGroupItem searchGroupItem2 = new SearchGroupItem()
                .type("DEMO")
                .addSearchParametersItem(parameter2);
        SearchGroupItem searchGroupItem3 = new SearchGroupItem()
                .type("CPT")
                .addSearchParametersItem(parameter3);

        SearchGroup searchGroup1 = new SearchGroup()
                .addItemsItem(searchGroupItem1);
        SearchGroup searchGroup2 = new SearchGroup()
                .addItemsItem(searchGroupItem2);
        SearchGroup searchGroup3 = new SearchGroup()
                .addItemsItem(searchGroupItem3);

        SearchRequest request = new SearchRequest()
                .addIncludesItem(searchGroup1)
                .addIncludesItem(searchGroup2)
                .addExcludesItem(searchGroup3);

        QueryJobConfiguration actualRequest = participantCounter.buildParticipantIdQuery(request,
            200, 0);

        for (String key : actualRequest.getNamedParameters().keySet()) {
            if (key.startsWith("gen")) {
                genderNamedParameter = key;
            } else if (key.startsWith("Condition")){
                conditionNamedParameter = key;
            } else if (key.startsWith("Procedure")) {
                procedureNamedParameter = key;
            } else if (key.startsWith("cmICD9")) {
                cmICD9NamedParameter = key;
            } else if (key.startsWith("procICD9")) {
                procICD9NamedParameter = key;
            } else if (key.startsWith("cmCPT")) {
                cmCPTNamedParameter = key;
            } else if (key.startsWith("procCPT")) {
                procCPTNamedParameter = key;
            }
        }

        final String expectedSql = "select distinct person_id, race_concept_id, gender_concept_id, ethnicity_concept_id, birth_datetime\n" +
                "from `${projectId}.${dataSetId}.person` person\n" +
                "where\n" +
                "person.person_id in (select person_id\n" +
                "from `${projectId}.${dataSetId}.person` p\n" +
                "where person_id in (select distinct person_id\n" +
                "from `${projectId}.${dataSetId}.condition_occurrence` a, `${projectId}.${dataSetId}.concept` b\n" +
                "where a.condition_source_concept_id = b.concept_id\n" +
                "and b.vocabulary_id in (@" + cmICD9NamedParameter + ",@" + procICD9NamedParameter + ")\n" +
                "and b.concept_code in unnest(@" + conditionNamedParameter + ")\n" +
                ")\n" +
                ")\n" +
                "and person.person_id in (select distinct person_id\n" +
                "from `${projectId}.${dataSetId}.person` p\n" +
                "where p.gender_concept_id = @" + genderNamedParameter + "\n" +
                ")\n" +
                "and not exists\n" +
                "(select 'x' from\n" +
                "(select person_id\n" +
                "from `${projectId}.${dataSetId}.person` p\n" +
                "where person_id in (select distinct person_id\n" +
                "from `${projectId}.${dataSetId}.procedure_occurrence` a, `${projectId}.${dataSetId}.concept` b\n" +
                "where a.procedure_source_concept_id = b.concept_id\n" +
                "and b.vocabulary_id in (@" + cmCPTNamedParameter + ",@" + procCPTNamedParameter + ")\n" +
                "and b.concept_code in unnest(@" + procedureNamedParameter + ")\n" +
                ")\n" +
                ")\n" +
                "x where x.person_id = person.person_id)\n" +
                "order by person_id\n" +
                "limit 200";

        assertEquals(expectedSql, actualRequest.getQuery());

        assertEquals("8507",
                actualRequest
                        .getNamedParameters()
                        .get(genderNamedParameter)
                        .getValue());

        assertEquals(parameter1.getValue(),
                actualRequest
                        .getNamedParameters()
                        .get(conditionNamedParameter)
                        .getArrayValues().get(0).getValue());
    }

    @Test
    public void buildChartInfoCounterQuery() throws Exception {

        String genderNamedParameter = "";
        String conditionNamedParameter = "";
        String procedureNamedParameter = "";
        String cmICD9NamedParameter = "";
        String procICD9NamedParameter = "";
        String cmCPTNamedParameter = "";
        String procCPTNamedParameter = "";

        SearchParameter parameter1 = new SearchParameter()
                .domain("Condition")
                .value("001.1");
        SearchParameter parameter2 = new SearchParameter()
                .subtype("GEN")
                .conceptId(8507L);
        SearchParameter parameter3 = new SearchParameter()
                .domain("Procedure")
                .value("001.2");

        SearchGroupItem searchGroupItem1 = new SearchGroupItem()
                .type("ICD9")
                .addSearchParametersItem(parameter1);
        SearchGroupItem searchGroupItem2 = new SearchGroupItem()
                .type("DEMO")
                .addSearchParametersItem(parameter2);
        SearchGroupItem searchGroupItem3 = new SearchGroupItem()
                .type("CPT")
                .addSearchParametersItem(parameter3);

        SearchGroup searchGroup1 = new SearchGroup()
                .addItemsItem(searchGroupItem1);
        SearchGroup searchGroup2 = new SearchGroup()
                .addItemsItem(searchGroupItem2);
        SearchGroup searchGroup3 = new SearchGroup()
                .addItemsItem(searchGroupItem3);

        SearchRequest request = new SearchRequest()
                .addIncludesItem(searchGroup1)
                .addIncludesItem(searchGroup2)
                .addExcludesItem(searchGroup3);

        QueryJobConfiguration actualRequest = participantCounter.buildChartInfoCounterQuery(request);

        for (String key : actualRequest.getNamedParameters().keySet()) {
            if (key.startsWith("gen")) {
                genderNamedParameter = key;
            } else if (key.startsWith("Condition")){
                conditionNamedParameter = key;
            } else if (key.startsWith("Procedure")) {
                procedureNamedParameter = key;
            } else if (key.startsWith("cmICD9")) {
                cmICD9NamedParameter = key;
            } else if (key.startsWith("procICD9")) {
                procICD9NamedParameter = key;
            } else if (key.startsWith("cmCPT")) {
                cmCPTNamedParameter = key;
            } else if (key.startsWith("procCPT")) {
                procCPTNamedParameter = key;
            }
        }

        final String expectedSql = "select concept1.concept_code as gender, \n" +
                "case when concept2.concept_name is null then 'Unknown' else concept2.concept_name end as race, \n" +
                "case when CAST(DATE_DIFF(CURRENT_DATE, DATE(person.year_of_birth, person.month_of_birth, person.day_of_birth), MONTH)/12 as INT64) >= 0 and CAST(DATE_DIFF(CURRENT_DATE, DATE(person.year_of_birth, person.month_of_birth, person.day_of_birth), MONTH)/12 as INT64) <= 18 then '0-18'\n" +
                "when CAST(DATE_DIFF(CURRENT_DATE, DATE(person.year_of_birth, person.month_of_birth, person.day_of_birth), MONTH)/12 as INT64) >= 19 and CAST(DATE_DIFF(CURRENT_DATE, DATE(person.year_of_birth, person.month_of_birth, person.day_of_birth), MONTH)/12 as INT64) <= 44 then '19-44'\n" +
                "when CAST(DATE_DIFF(CURRENT_DATE, DATE(person.year_of_birth, person.month_of_birth, person.day_of_birth), MONTH)/12 as INT64) >= 45 and CAST(DATE_DIFF(CURRENT_DATE, DATE(person.year_of_birth, person.month_of_birth, person.day_of_birth), MONTH)/12 as INT64) <= 64 then '45-64'\n" +
                "else '> 65'\n" +
                "end as ageRange,\n" +
                "count(*) as count\n" +
                "from `${projectId}.${dataSetId}.person` person\n" +
                "left join `${projectId}.${dataSetId}.concept` concept1 on (person.gender_concept_id = concept1.concept_id and concept1.vocabulary_id = 'Gender')\n" +
                "left join `${projectId}.${dataSetId}.concept` concept2 on (person.race_concept_id = concept2.concept_id and concept2.vocabulary_id = 'Race')\n" +
                "where\n" +
                "person.person_id in (select person_id\n" +
                "from `${projectId}.${dataSetId}.person` p\n" +
                "where person_id in (select distinct person_id\n" +
                "from `${projectId}.${dataSetId}.condition_occurrence` a, `${projectId}.${dataSetId}.concept` b\n" +
                "where a.condition_source_concept_id = b.concept_id\n" +
                "and b.vocabulary_id in (@" + cmICD9NamedParameter + ",@" + procICD9NamedParameter + ")\n" +
                "and b.concept_code in unnest(@" + conditionNamedParameter + ")\n" +
                ")\n" +
                ")\n" +
                "and person.person_id in (select distinct person_id\n" +
                "from `${projectId}.${dataSetId}.person` p\n" +
                "where p.gender_concept_id = @" + genderNamedParameter + "\n" +
                ")\n" +
                "and not exists\n" +
                "(select 'x' from\n" +
                "(select person_id\n" +
                "from `${projectId}.${dataSetId}.person` p\n" +
                "where person_id in (select distinct person_id\n" +
                "from `${projectId}.${dataSetId}.procedure_occurrence` a, `${projectId}.${dataSetId}.concept` b\n" +
                "where a.procedure_source_concept_id = b.concept_id\n" +
                "and b.vocabulary_id in (@" + cmCPTNamedParameter + ",@" + procCPTNamedParameter + ")\n" +
                "and b.concept_code in unnest(@" + procedureNamedParameter + ")\n" +
                ")\n" +
                ")\n" +
                "x where x.person_id = person.person_id)\n" +
                "group by gender, race, ageRange\n" +
                "order by gender, race, ageRange\n";

        assertEquals(expectedSql, actualRequest.getQuery());

        assertEquals("8507",
                actualRequest
                        .getNamedParameters()
                        .get(genderNamedParameter)
                        .getValue());

        assertEquals(parameter1.getValue(),
                actualRequest
                        .getNamedParameters()
                        .get(conditionNamedParameter)
                        .getArrayValues().get(0).getValue());
    }

}

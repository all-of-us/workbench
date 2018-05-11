package org.pmiops.workbench.cdr.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbDomain;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.AchillesResult;
import org.pmiops.workbench.cdr.model.AchillesAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class DbDomainDaoTest {

    @Autowired
    private DbDomainDao dao;

    @Autowired
    private ConceptDao conceptDao;

    @Autowired
    private AchillesResultDao achillesResultDao;

    @Autowired
    private AchillesAnalysisDao achillesAnalysisDao;

    private DbDomain dbDomain1;

    @Before
    public void setUp() {


        DbDomain dbDomain2;
        DbDomain dbDomain3;
        DbDomain dbDomain4;

        Concept concept1;
        Concept concept2;
        Concept concept3;
        Concept concept4;

        AchillesResult achillesResult1;
        AchillesResult achillesResult2;
        AchillesResult achillesResult3;

        AchillesAnalysis achillesAnalysis1;
        AchillesAnalysis achillesAnalysis2;
        AchillesAnalysis achillesAnalysis3;

        dbDomain1 = createDbDomain("Condition","Diagnoses","Conditions are records of a Person suggesting the presence of a disease or medical condition stated as a diagnosis, a sign or a symptom, which is either observed by a Provider or reported by the patient.","domain_filter","condition",19L,0L);
        dao.save(dbDomain1);

        dbDomain2=createDbDomain("Drug","Medications","Drugs biochemical substance formulated in such a way that when administered to a Person it will exert a certain physiological or biochemical effect. The drug exposure domain concepts capture records about the utilization of a Drug when ingested or otherwise introduced into the body.","domain_filter","drug",13L,0L);
        dao.save(dbDomain2);

        dbDomain3=createDbDomain("Lifestyle","Lifestyle","The Lifestyle module provides information on smoking, alcohol and recreational drug use","survey","ppi",1585855L,568120L);
        dao.save(dbDomain3);

        dbDomain4=createDbDomain("TheBasics","The Basics","The Basics module provides demographics and economic information for participants","survey","ppi",1586134L,567437L);
        dao.save(dbDomain3);

        concept1=createConcept(4296023L,"Failure","S","76797004","Clinical Finding","SNOMED","Condition",10L,0.0f);
        concept2=createConcept(1585826L,"Kidney failure","S","OrganTransplantDescription_Kidney","Clinical Finding","SNOMED","Condition",15L,0.0f);
        concept3=createConcept(1000000L,"What is the reason of your failure?","","Question","Question","PPI","Observation",2480L,0.57f);
        concept4=createConcept(2000000L,"Are you a fitness geek?","","Question","Question","PPI","Observation",2476L,0.51f);

        conceptDao.save(concept1);
        conceptDao.save(concept2);
        conceptDao.save(concept3);
        conceptDao.save(concept4);

        achillesAnalysis1=createAchillesAnalysis(3110L,"Survey Question Answer Count","survey_concept_id","question_concept_id","answer_concept_id","answer_value_string",null,"column","counts");
        achillesAnalysis2=createAchillesAnalysis(3111L,"Gender","survey_concept_id","question_concept_id","answer_concept_id","answer_value_string","gender_concept_id","column","charts");
        achillesAnalysis2=createAchillesAnalysis(3112L,"Age","survey_concept_id","question_concept_id","answer_concept_id","answer_value_string","age_decile","column","charts");

        achillesAnalysisDao.save(achillesAnalysis1);
        achillesAnalysisDao.save(achillesAnalysis2);
        achillesAnalysisDao.save(achillesAnalysis3);

        achillesResult1=createAchillesResult(2397L,3110L,"1586134","1000000","","Smoking",null,260L);
        achillesResult2=createAchillesResult(2380L,3111L,"1585855","2000000","","Drinking is the cause of failure",null,2345L);
        achillesResult3=createAchillesResult(2345L,3112L,"1586134","1000000","","Donot know",null,789L);

        achillesResultDao.save(achillesResult1);
        achillesResultDao.save(achillesResult2);
        achillesResultDao.save(achillesResult3);
    }

    @Test
    public void findAllDbDomains() throws Exception {
        /* Todo write more tests */
        final List<DbDomain> list = dao.findAll();
        Assert.assertEquals(dbDomain1.getDomainId(),"Domain1");
        Assert.assertEquals(list.get(0).getDomainId(),dbDomain1.getDomainId());
    }

    @Test
    public void findDbDomainsByDbType() throws Exception {
        /* Todo write more tests */
        final List<DbDomain> list = dao.findByDbType("Sample Domain");
        Assert.assertEquals(dbDomain1.getDomainId(),"Domain1");
        Assert.assertEquals(list.get(0).getDomainId(),dbDomain1.getDomainId());
    }

    @Test
    public void findDbDomainsByDbTypeAndConceptId() throws Exception {
        /* Todo write more tests */
        final List<DbDomain> list = dao.findByDbTypeAndAndConceptIdNotNull("Sample Domain");
        Assert.assertEquals(dbDomain1.getDomainId(),"Domain1");
        Assert.assertEquals(list.get(0).getDomainId(),dbDomain1.getDomainId());
    }

    @Test
    public void findDomainMatchResults() throws Exception{
        final List<DbDomain> list=dao.findDomainSearchResults("hypertension");
        Assert.assertNotEquals(list,null);
    }

    private DbDomain createDbDomain(String domainId, String domainDisp, String domainDesc,String dbType,String domainRoute,Long conceptId,Long count) {
        return new DbDomain()
                .domainId(domainId)
                .domainDisplay(domainDisp)
                .domainDesc(domainDesc)
                .dbType(dbType)
                .domainRoute(domainRoute)
                .conceptId(conceptId)
                .countValue(count);
    }

    private Concept createConcept(Long conceptId,String conceptName,String standardConcept,String conceptCode,String conceptClassId,String vocabularyId,String domainId,Long count,float prevalence){
        return new Concept()
                .conceptId(conceptId)
                .conceptName(conceptName)
                .standardConcept(standardConcept)
                .conceptCode(conceptCode)
                .conceptClassId(conceptClassId)
                .vocabularyId(vocabularyId)
                .domainId(domainId)
                .count(count)
                .prevalence(prevalence);
    }

    private AchillesResult createAchillesResult(Long id,Long analysisId,String stratum_1,String stratum_2,String stratum_3,String stratum_4,String stratum_5,Long count){
        return new AchillesResult()
                .id(id)
                .analysisId(analysisId)
                .stratum1(stratum_1)
                .stratum2(stratum_2)
                .stratum3(stratum_3)
                .stratum4(stratum_4)
                .stratum5(stratum_5)
                .countValue(count);
    }

    private AchillesAnalysis createAchillesAnalysis(Long analysisId,String analysisName,String st1,String st2,String st3,String st4,String st5,String chartType,String dataType){
        return new AchillesAnalysis()
                .analysisId(analysisId)
                .analysisName(analysisName)
                .stratum1Name(st1)
                .stratum2Name(st2)
                .stratum3Name(st3)
                .stratum4Name(st4)
                .stratum5Name(st5)
                .chartType(chartType)
                .dataType(dataType);
    }


}

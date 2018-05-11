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

        Concept concept1;
        Concept concept2;

        AchillesResult achillesResult1;
        AchillesResult achillesResult2;
        AchillesResult achillesResult3;

        AchillesAnalysis achillesAnalysis1;
        AchillesAnalysis achillesAnalysis2;
        AchillesAnalysis achillesAnalysis3;

        dbDomain1 = createDbDomain("Domain1","Sample Domain");
        dao.save(dbDomain1);

        dbDomain2=createDbDomain("Condition","domain_filter");
        dao.save(dbDomain2);

        dbDomain3=createDbDomain("Lifestyle","survey");
        dao.save(dbDomain3);

        concept1=createConcept(1L,"Condition","Sample hypertension");
        concept2=createConcept(2L,"Lifestyle","Do you have hypertension or any of the symptoms related to hypertension");

        conceptDao.save(concept1);
        conceptDao.save(concept2);

        achillesAnalysis1=createAchillesAnalysis(Long.valueOf(3110),"Response distribution");
        achillesAnalysis2=createAchillesAnalysis(Long.valueOf(3111),"Gender");
        achillesAnalysis3=createAchillesAnalysis(Long.valueOf(3112),"Age");

        achillesAnalysisDao.save(achillesAnalysis1);
        achillesAnalysisDao.save(achillesAnalysis2);
        achillesAnalysisDao.save(achillesAnalysis3);

        achillesResult1=createAchillesResult(1L,Long.valueOf(3110),"1586134",1L);
        achillesResult2=createAchillesResult(2L,Long.valueOf(3111),"1585855",2L);
        achillesResult3=createAchillesResult(3L,Long.valueOf(3112),"1585710",1L);

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

    private DbDomain createDbDomain(String domainId, String dbType) {
        return new DbDomain()
                .domainId(domainId)
                .domainDisplay("Domain description for display")
                .domainDesc("Domain description")
                .dbType(dbType)
                .domainRoute("Domain Route")
                .conceptId(Long.valueOf(0))
                .countValue(Long.valueOf(0));
    }

    private Concept createConcept(Long conceptId,String domainId,String conceptName){
        return new Concept()
                .conceptId(conceptId)
                .conceptName(conceptName)
                .standardConcept("S")
                .conceptCode("Sample concept code")
                .conceptClassId("Sample concept class Id")
                .vocabularyId("Sample vocab")
                .domainId(domainId)
                .count(2L)
                .prevalence(0.0f);
    }

    private AchillesResult createAchillesResult(Long id,Long analysisId,String stratum_1,Long stratum_2){
        return new AchillesResult()
                .id(id)
                .analysisId(analysisId)
                .stratum1(stratum_1)
                .stratum2(String.valueOf(stratum_2))
                .stratum3("0")
                .stratum4("hypertension")
                .stratum5(null)
                .countValue(2L);
    }

    private AchillesAnalysis createAchillesAnalysis(Long analysisId,String analysisName){
        return new AchillesAnalysis()
                .analysisId(analysisId)
                .analysisName(analysisName)
                .stratum1Name("stratum 1")
                .stratum2Name("stratum 2")
                .stratum3Name("stratum 3")
                .stratum4Name("stratum 4")
                .stratum5Name("stratum 5")
                .chartType("column")
                .dataType("counts");
    }


}

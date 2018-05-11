package org.pmiops.workbench.cdr.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbDomain;
import org.pmiops.workbench.cdr.model.Concept;
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
    private QuestionConceptDao conceptDao;

    private DbDomain dbDomain1;



    @Before
    public void setUp() {

        Concept concept1;
        Concept concept2;
        Concept concept3;

        dbDomain1 = createDbDomain("Domain1","Sample Domain");
        concept1 = createConcept("Sample Hypertension","Condition");
        concept2 = createConcept("Sample Diabetes","Condition");
        concept3 = createConcept("Cancer Treatment","Procedure");
        dao.save(dbDomain1);
        conceptDao.save(concept1);
        conceptDao.save(concept2);
        conceptDao.save(concept3);
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

    private Concept createConcept(String conceptName,String domainId) {
        return new Concept()
                .conceptId(Long.valueOf(0))
                .conceptName(conceptName)
                .domainId(domainId)
                .vocabularyId("Sample Vocabulary")
                .conceptClassId("Sample Class")
                .standardConcept("S")
                .conceptCode(conceptCode)
                .valid_start_date("1970-01-01")
                .valid_end_date("2099-12-31")
                .invalid_reason(null)
                .count_value(2)
                .prevalence(0.00);
    }


}

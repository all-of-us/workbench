package org.pmiops.workbench.cdr.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbDomain;
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

    private DbDomain dbDomain1;

    @Before
    public void setUp() {

        dbDomain1 = createDbDomain("Domain1","Sample Domain");

        dao.save(dbDomain1);

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

}

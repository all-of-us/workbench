package org.pmiops.workbench.cdr.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.QuestionConcept;
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
public class QuestionConceptDaoTest {

    @Autowired
    private QuestionConceptDao questionConceptDao;


    @Test
    public void findSurveyQuestions() throws Exception{
        final List<QuestionConcept> list=questionConceptDao.findSurveyQuestions(1586134L);
        Assert.assertNotEquals(list,null);
    }


}

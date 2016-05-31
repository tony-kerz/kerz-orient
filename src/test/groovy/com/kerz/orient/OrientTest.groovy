package com.kerz.orient

import static org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.impls.orient.OrientGraph

@RunWith(SpringJUnit4ClassRunner)
@SpringApplicationConfiguration(classes = [
  OrientRepositoryConfiguration
  ]
)
class OrientTest {
  Logger log = LoggerFactory.getLogger(OrientTest)
  
  @Autowired
  OrientGraph g

  @Before
  void setUp() {
  }

  @After
  void tearDown() {
  }
  
  @Test
  void shouldEmbed() {
    g.command(new OCommandSQL('drop class Person unsafe')).execute()
    g.command(new OCommandSQL('create class Person extends V')).execute()
    g.command(new OCommandSQL('create property Person.point embedded OPoint')).execute()
    
    def doc = new ODocument('OPoint')
    doc.field('coordinates', [1.1f, 2.2f])
    
    def person = g.addVertex('class:Person')
    def point = person.setProperty('point', doc)
    
    g.commit()
  }
}

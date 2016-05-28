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

import com.kerz.orient.dao.PersonRepository
import com.kerz.orient.domain.Person

@RunWith(SpringJUnit4ClassRunner)
@SpringApplicationConfiguration(classes = [
  OrientRepositoryConfiguration, 
  PersonRepository
  ]
)
class OrientRepositoryTest {
  Logger log = LoggerFactory.getLogger(OrientRepositoryTest)
  
  //def vertexTypes = [Widget, Whatsit, Person]
  def vertexTypes = [Person]

//  @Autowired
//  OrientRepository orientRepository
  
  @Autowired
  PersonRepository personRepository

  @Before
  void setUp() {
    vertexTypes.each {
      personRepository.createVertexType(it.simpleName)
    }
  }

  @After
  void tearDown() {
    vertexTypes.each {
      personRepository.dropVertexType(it.simpleName)
    }
  }

//  @Test
//  void shouldWork() {
//    def w = new Widget(name: 'w123', price: 10.99, whatsit: new Whatsit(name: 'w456'))
//    orientRepository.saveElement(w)
//    def w2 = orientRepository.g.v(w.id)
//    assertNotNull('entity required', w2)
//    assertEquals('name should be same', w.name, w2.name)
//    assertEquals('price should be same', w.price, w2.price, 0.0f)
//    assertNull(w2?.whatsit)
//  }
  
  @Test
  void crudRepo() {
    assertNotNull(personRepository)
    def person = personRepository.save(new Person(firstName: 'fred', lastName: 'smith', address: '151 Farmington Ave, Hartford CT, 06108'))
    def person2 = personRepository.findOne(person.id)
    assertEquals(person, person2)
  }
}

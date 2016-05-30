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
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import com.kerz.orient.dao.PersonRepository
import com.kerz.orient.domain.OPoint
import com.kerz.orient.domain.Person
import com.tinkerpop.blueprints.impls.orient.OrientGraph

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
  OrientGraph g
  
  @Autowired
  PersonRepository personRepository

  @Before
  void setUp() {
    vertexTypes.each {
      personRepository.createVertexType(it.simpleName)
    }
    personRepository.submitSql('create property person.location embedded OPoint')
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
  void shouldSort() {
    def peep1 = personRepository.save(new Person(firstName: 'abe', lastName: 'beck'))
    def peep2 = personRepository.save(new Person(firstName: 'andy', lastName: 'smith'))
    def peep3 = personRepository.save(new Person(firstName: 'fred', lastName: 'smith'))
    def peep4 = personRepository.save(new Person(firstName: 'jon', lastName: 'jones'))
    def peep5 = personRepository.save(new Person(firstName: 'jon', lastName: 'zephyr'))
    
    def sort = new Sort(Sort.Direction.ASC, 'firstName')
    def people = personRepository.findAll(sort)
    assertEquals(5, people.size)
    assertEquals(peep1, people[0])
    assertEquals(peep3, people[2])
    
    sort = new Sort(Sort.Direction.DESC, 'firstName')
    people = personRepository.findAll(sort)
    assertEquals(peep2, people[3])
    assertEquals(peep1, people[4])
    
    sort = new Sort(Sort.Direction.ASC, 'lastName')
    people = personRepository.findAll(sort)
    assertEquals(peep1, people[0])
    assertEquals(peep4, people[1])
    assertEquals(peep5, people[4])
    
    sort = new Sort(Sort.Direction.ASC, 'lastName').and(new Sort(Sort.Direction.DESC, 'firstName'))
    people = personRepository.findAll(sort)
    assertEquals(peep3, people[2])
    assertEquals(peep2, people[3])
    
    personRepository.g.commit()
  }
  
  @Test
  void shouldPage() {
    def peep1 = personRepository.save(new Person(firstName: 'abe', lastName: 'beck'))
    def peep2 = personRepository.save(new Person(firstName: 'andy', lastName: 'smith'))
    def peep3 = personRepository.save(new Person(firstName: 'fred', lastName: 'smith'))
    def peep4 = personRepository.save(new Person(firstName: 'jon', lastName: 'jones'))
    def peep5 = personRepository.save(new Person(firstName: 'jon', lastName: 'zephyr'))
    
    Pageable pageable = new PageRequest(0, 2, Sort.Direction.ASC, 'firstName')
    Page<Person> page = personRepository.findAll(pageable)

    assertEquals(5, page.totalElements)
    assertEquals(2, page.size)
    assertEquals(3, page.totalPages)
    assertEquals(2, page.content.size())
    assertEquals(peep1, page.content[0])
    assertEquals(peep2, page.content[1])
    
    page = personRepository.findAll(page.nextPageable())
    
    assertEquals(2, page.content.size())
    assertEquals(peep3, page.content[0])
    assertEquals(peep4, page.content[1])
    
    page = personRepository.findAll(page.nextPageable())
    
    assertEquals(1, page.content.size())
    assertEquals(peep5, page.content[0])
    
    assertNull(page.nextPageable())
  }
  
  @Test
  void shouldCrud() {
    def person = personRepository.save(new Person(firstName: 'fred', lastName: 'smith', address: '151 Farmington Ave, Hartford CT, 06108'))
    assertNotNull(person.id)
    def person2 = personRepository.findOne(person.id)
    assertEquals(person, person2)
    assertEquals(1, personRepository.count())
    person2.firstName = 'frank'
    personRepository.save(person2)
    person2 = personRepository.findOne(person.id)
    assertEquals('frank', person2.firstName)
    personRepository.delete(person)
    assertEquals(0, personRepository.count())
  }
  
  @Test
  void shouldSaveEmbedded() {
    def point = new OPoint(coordinates: [1.1f, 2.2f])
    def person = personRepository.save(new Person(firstName: 'fred', lastName: 'smith', address: '151 Farmington Ave, Hartford CT, 06108', point: point))
    assertNotNull(person.id)
    def person2 = personRepository.findOne(person.id)
    assertEquals(person, person2)
    assertNotNull(person2.point)
    assertEquals(point, person2.point)
    assertEquals(1, personRepository.count())
    personRepository.delete(person)
    assertEquals(0, personRepository.count())
  }
}

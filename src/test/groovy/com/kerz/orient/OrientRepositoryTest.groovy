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
import com.kerz.orient.dao.PersonSqlRepository
import com.kerz.orient.domain.OPoint
import com.kerz.orient.domain.Person
import com.tinkerpop.blueprints.impls.orient.OrientGraph

@RunWith(SpringJUnit4ClassRunner)
@SpringApplicationConfiguration(classes = [
  OrientRepositoryConfiguration, 
  PersonRepository,
  PersonSqlRepository
  ]
)
class OrientRepositoryTest {
  Logger log = LoggerFactory.getLogger(OrientRepositoryTest)
  
  //def vertexTypes = [Widget, Whatsit, Person]
  def vertexTypes = [Person]

  @Autowired
  OrientGraph g
  
  @Autowired
  OrientHelper oHelper
  
  @Autowired
  PersonRepository personRepository
  
  @Autowired
  PersonSqlRepository personSqlRepository
  
  @Before
  void setUp() {
    oHelper.submitSql('drop class person unsafe')
    oHelper.submitSql('create class Person extends V')
    oHelper.submitSql('create property person.point embedded OPoint')
  }

  @After
  void tearDown() {
    //g.commit()
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
  
  void shouldSort(def repo) {
    def peep1 = repo.save(new Person(firstName: 'abe', lastName: 'beck'))
    def peep2 = repo.save(new Person(firstName: 'andy', lastName: 'smith'))
    def peep3 = repo.save(new Person(firstName: 'fred', lastName: 'smith'))
    def peep4 = repo.save(new Person(firstName: 'jon', lastName: 'jones'))
    def peep5 = repo.save(new Person(firstName: 'jon', lastName: 'zephyr'))
    g.commit()
    
    def sort = new Sort(Sort.Direction.ASC, 'firstName')
    def people = repo.findAll(sort)
    assertEquals(5, people.size)
    assertEquals(peep1, people[0])
    assertEquals(peep3, people[2])
    
    sort = new Sort(Sort.Direction.DESC, 'firstName')
    people = repo.findAll(sort)
    assertEquals(peep2, people[3])
    assertEquals(peep1, people[4])
    
    sort = new Sort(Sort.Direction.ASC, 'lastName')
    people = repo.findAll(sort)
    assertEquals(peep1, people[0])
    assertEquals(peep4, people[1])
    assertEquals(peep5, people[4])
    
    sort = new Sort(Sort.Direction.ASC, 'lastName').and(new Sort(Sort.Direction.DESC, 'firstName'))
    people = repo.findAll(sort)
    assertEquals(peep3, people[2])
    assertEquals(peep2, people[3])
    
    repo.deleteAll()
    g.commit()
    
    assertEquals(0, repo.count())
  }
  
  @Test
  void shouldSort() {
    shouldSort(personRepository)
  }
  
  @Test
  void shouldSortSql() {
    shouldSort(personSqlRepository)
  }
  
  void shouldPage(def repo) {
    def peep1 = repo.save(new Person(firstName: 'abe', lastName: 'beck'))
    def peep2 = repo.save(new Person(firstName: 'andy', lastName: 'smith'))
    def peep3 = repo.save(new Person(firstName: 'fred', lastName: 'smith'))
    def peep4 = repo.save(new Person(firstName: 'jon', lastName: 'jones'))
    def peep5 = repo.save(new Person(firstName: 'jon', lastName: 'zephyr'))
    g.commit()
    
    Pageable pageable = new PageRequest(0, 2, Sort.Direction.ASC, 'firstName')
    Page<Person> page = repo.findAll(pageable)

    assertEquals(5, page.totalElements)
    assertEquals(2, page.size)
    assertEquals(3, page.totalPages)
    assertEquals(2, page.content.size())
    assertEquals(peep1, page.content[0])
    assertEquals(peep2, page.content[1])
    
    page = repo.findAll(page.nextPageable())
    
    assertEquals(2, page.content.size())
    assertEquals(peep3, page.content[0])
    assertEquals(peep4, page.content[1])
    
    page = repo.findAll(page.nextPageable())
    
    assertEquals(1, page.content.size())
    assertEquals(peep5, page.content[0])
    
    assertNull(page.nextPageable())
    
    repo.deleteAll()
    g.commit()
    
    assertEquals(0, repo.count())
  }
  
  @Test
  void shouldPage() {
    shouldPage(personRepository)
  }
  
  @Test
  void shouldPageSql() {
    shouldPage(personSqlRepository)
  }
  
  void shouldCrud(def repo) {
    def person = repo.save(new Person(firstName: 'fred', lastName: 'smith', address: '151 Farmington Ave, Hartford CT, 06108'))
    oHelper.g.commit()
    assertNotNull(person.id)
    log.debug("person.id=$person.id")
    def person2 = repo.findOne(person.id)
    assertEquals(person, person2)
    assertEquals(1, repo.count())
    person2.firstName = 'frank'
    repo.save(person2)
    oHelper.g.commit()
    person2 = repo.findOne(person.id)
    assertEquals('frank', person2.firstName)
    repo.delete(person)
    oHelper.g.commit()
    assertEquals(0, repo.count())
  }
  
  @Test
  void shouldCrud() {
    shouldCrud(personRepository)
  }
  
  @Test
  void shouldCrudSql() {
    shouldCrud(personSqlRepository)
  }
  
  void shouldSaveEmbedded(def repo) {
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
  
  @Test
  void shouldSaveEmbedded() {
    shouldSaveEmbedded(personRepository)
  }
  
  @Test
  void shouldSaveEmbeddedSql() {
    shouldSaveEmbedded(personSqlRepository)
  }
}

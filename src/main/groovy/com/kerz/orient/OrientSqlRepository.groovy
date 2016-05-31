package com.kerz.orient

import java.lang.reflect.ParameterizedType

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.util.Assert

import com.tinkerpop.blueprints.impls.orient.OrientGraph


@Repository
class OrientSqlRepository<T, ID extends Serializable> implements PagingAndSortingRepository<T, ID> {
  static Logger log = LoggerFactory.getLogger(OrientSqlRepository)
  
  Class<T> clazz
  String className
  
  @Autowired
  OrientHelper helper
  
  @Autowired
  OrientGraph g
  
  OrientSqlRepository() {
    clazz = (Class<>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]
    log.debug("<init>: this=${this.class}, clazz=$clazz")
    className = clazz.simpleName
  }
  
  def iterableToList(Iterable iterable) {
    def list = []
    iterable.each { list << "$it.property $it.direction"}
    list
  }
  
  @Override
  public Iterable<T> findAll(Sort sort) {
    def entities = []
    log.debug("sort=$sort")
    String sql = "select from $className order by ${iterableToList(sort).join(',')}"
    helper.submitSql(sql).each { entities << helper.reconstitute(clazz, it) }
    entities
  }

  @Override
  public Page<T> findAll(Pageable pageable) {
    def entities = []    
    String sql = "select from $className order by ${iterableToList(pageable.sort).join(',')} skip $pageable.offset limit $pageable.pageSize"
    helper.submitSql(sql).each { entities << helper.reconstitute(clazz, it) }
    new PageImpl(entities, pageable, count())
  }

  @Override
  public <S extends T> S save(S entity) {
    log.debug("save: entity=$entity")
    helper.save(clazz, entity)
  }

  @Override
  public <S extends T> Iterable<S> save(Iterable<S> entities) {
    entities.each { save(it) }
    entities
  }
  
  def getSingleResult(String sql) {
    def result
    int count = 0
    helper.submitSql(sql).each {
      result = it
      count += 1
    }
    log.debug("get-single-result: result=$result")
    Assert.isTrue(count == 1)
    result
  }
  
  @Override
  public T findOne(ID id) {
    String sql = "select from $className where @rid = $id"
    def result = helper.reconstitute(clazz, getSingleResult(sql))
    log.debug("find-one: result=$result")
    result
  }

  @Override
  public boolean exists(ID id) {
    String sql = "select count(*) from $className where @rid = $id"
    def result = getSingleResult(sql)
    result.count == 1
  }

  @Override
  public Iterable<T> findAll() {
    def entities = []
    String sql = "select from $className"
    helper.submitSql(sql).each { entities << helper.reconstitute(clazz, it) }
    entities
  }

  @Override
  public Iterable<T> findAll(Iterable<ID> ids) {
    def iterables = []
    ids.each { iterables << findOne(it) }
    iterables
  }

  @Override
  public long count() {
    String sql = "select count(*) from $className"
    def result = getSingleResult(sql)
    result.count
  }

  @Override
  public void delete(ID id) {
    // since sql commands are currently submitted outside of local transaction
    // let's not issue deletes this way so they can participate in local tx if required
    //
    // String sql = "delete from $className where @rid = $id unsafe"
    // def response = helper.submitSql(sql)
    // log.debug("delete: response=$response")
    // Assert.isTrue(response.value == 1)
    g.removeVertex(g.v(id))
  }

  @Override
  public void delete(T entity) {
    delete(entity.id)
  }

  @Override
  public void delete(Iterable<? extends T> entities) {
    entities.each { delete(it) }
  }

  @Override
  public void deleteAll() {
    // String sql = "delete from $className unsafe"
    // def response = helper.submitSql(sql)
    // log.debug("delete-all: response=$response")
    g.V('@class', className).remove()
  }
}
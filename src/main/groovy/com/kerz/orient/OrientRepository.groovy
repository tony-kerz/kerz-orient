package com.kerz.orient

import java.lang.reflect.ParameterizedType

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.util.Assert

import com.kerz.orient.domain.GElement
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.Element
import com.tinkerpop.blueprints.impls.orient.OrientGraph

@Repository
class OrientRepository<T extends GElement, ID extends Serializable> implements PagingAndSortingRepository<T, ID> {
  static Log log = LogFactory.getLog(OrientRepository)
  
  Class<T> clazz
  String className
  
  @Autowired
  OrientGraph g
  
  def static nameExcludes = ['metaClass', 'class', 'id', 'element']
  def static classIncludes = [String, Date]
  
  OrientRepository() {
    clazz = (Class<>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]
    log.debug("<init>: this=${this.class}, clazz=$clazz")
    className = clazz.simpleName
  }
  
  // eclipse issue...
  T save(T){} 
    
  @Override
  public Iterable<T> findAll(Sort sort) {
    def entities = []
    g.V('@class', className).order { sortComparator(sort, it.a, it.b) }.each { entities << reconstitute(it) }
    entities
  }
  
  int sortComparator(Sort sort, a, b) {
    int result
    sort.any {
      log.debug("sort:each: prop=$it.property, asc=$it.ascending")
      if (it.ascending) {
        return result = a[it.property] <=> b[it.property]
      } else {
        return result = b[it.property] <=> a[it.property]
      }
    }
    result
  }

  @Override
  public Page<T> findAll(Pageable pageable) {
    int offset = pageable.offset
    def entities = []
    
    g.V('@class', className)
    .order { sortComparator(pageable.sort, it.a, it.b) }
    .range(offset, offset + pageable.pageSize - 1)
    .each { entities << reconstitute(it) }
    
    new PageImpl(entities, pageable, count())
  }

  @Override
  public <S extends T> S save(S entity) {
    log.debug("save: entity=$entity")
    if (!entity.element) {
      String oClassName = "class:$className"
      entity.element = g.addVertex(oClassName)
    }
    
    entity.properties.each { prop, val ->
      MetaProperty metaProp = entity.class.metaClass.getMetaProperty(prop)
      boolean shouldSave = shouldSave(metaProp)
      log.debug("save: save=[$shouldSave], prop=[$prop], type=[$metaProp.type], val=[$val]")
      if (shouldSave && val) {
        entity.element.setProperty(prop, toStorage(metaProp, val))
      }
    }

    entity
  }

  @Override
  public <S extends T> Iterable<S> save(Iterable<S> entities) {
    entities.each { saveElement(it) }
    entities
  }

  @Override
  public T findOne(ID id) {
    reconstitute(g.v(id))
  }

  @Override
  public boolean exists(ID id) {
    g.v(id)
  }

  @Override
  public Iterable<T> findAll() {
    def entities = []
    g.V('@class', className).each { entities << reconstitute(it) }
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
    g.V('@class', className).count()
  }

  @Override
  public void delete(ID id) {
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
    g.V('@class', className).each { g.removeVertex(it) }
  }
  
  T reconstitute(Element element) {
    Assert.notNull(element, 'element required')
    def entity = clazz.newInstance()
    entity.element = element
    log.debug("reconstitute: entity=[$className], id=[$element.id]")
    element.getPropertyKeys().each {
      def val = element.getProperty(it)
      log.debug("reconstitute: key=[$it], val=[$val]")
      MetaProperty metaProp = entity.class.metaClass.getMetaProperty(it)
      entity[it] = fromStorage(metaProp.type, val)
    }
    entity
  }
  
  boolean classExists(name) {
    g.getRawGraph().getMetadata().getSchema().existsClass(name)
  }

  void createVertexType(name) {
    if (!classExists(name)) {
      log.debug("create-vertex-type: name=$name")
      g.createVertexType(name)
    }
  }

  void createEdgeType(name) {
    if (!classExists(name)) {
      log.debug("create-edge-type: name=$name")
      g.createEdgeType(name)
    }
  }

  void dropVertexType(name) {
    if (classExists(name)) {
      log.debug("drop-vertex-type: name=$name")
      g.dropVertexType(name)
    }
  }

  void dropEdgeType(name) {
    if (classExists(name)) {
      log.debug("drop-edge-type: name=$name")
      g.dropEdgeType(name)
    }
  }

  static boolean shouldSave(MetaProperty metaProp) {
    //!(metaProp.name in nameExcludes) && (metaProp.type.isPrimitive() || metaProp.type in classIncludes)
    !(metaProp.name in nameExcludes)
  }
  
  static def toStorage(MetaProperty metaProp, val) {
    if (metaProp.type.isPrimitive() || metaProp.type in classIncludes) {
      val
    } else {
      asDocument(val)
    }
  }
  
  static def asDocument(entity) {
    // to-do: handle nested objects
    Class clazz = entity.class
    def doc = new ODocument(clazz.simpleName)
    entity.properties.each { k, v ->
      MetaProperty metaProp = entity.class.metaClass.getMetaProperty(k)
      boolean should = shouldSave(metaProp)
      log.debug("as-document: should=$should, k=$k, v=$v, type=$metaProp.type")
      if (should && v) {
        doc.field(k, v)
      }
    }
    doc
  }
  
  static def fromStorage(Class clazz, val) {
    log.debug("from-storage: clazz=$clazz, val=$val")
    if (val instanceof ODocument) {
      def instance = clazz.newInstance()
      val.toMap().each {k, v -> 
        boolean should = shouldReconstituteProp(k)
        log.debug("from-storage: should=$should, k=$k, v=$v")
        if (should) {
          instance[k] = v
        }
      }
      instance
    } else {
      val
    }
  }
  
  static def shouldReconstituteProp(String prop) {
    !(prop in ['@rid', '@class'])
  }
  
  void submitSql(String sql) {
    g.command(new OCommandSQL(sql)).execute()
  }
}
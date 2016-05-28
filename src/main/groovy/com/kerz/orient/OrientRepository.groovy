package com.kerz.orient

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.util.Assert

import com.tinkerpop.blueprints.Element
import com.tinkerpop.blueprints.impls.orient.OrientGraph

@Repository
class OrientRepository<T, ID extends Serializable> implements CrudRepository<T, ID> {
  static Log log = LogFactory.getLog(OrientRepository)
  
  Class<T> clazz
  String className
  
  @Autowired
  OrientGraph g
  
  def static nameExcludes = ['metaClass', 'class', 'id']
  def static classIncludes = [String, Date]
  
  OrientRepository() {
    clazz = (Class<>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]
    log.debug("<init>: this=${this.class}, clazz=$clazz")
    className = clazz.simpleName
  }
  
  // eclipse issue...
  //T save(T){} 
    
  @Override
  public <S extends T> S save(S entity) {
    def gElt = g.addVertex("class:$className")
    log.debug("save: vertex=$className")

    def eType = entity.class.metaClass
    entity.properties.each { prop, val ->
      MetaProperty metaProp = eType.getMetaProperty(prop)
      boolean shouldSave = shouldSave(metaProp)
      log.debug("save: save=[$shouldSave], prop=[$prop], type=[$metaProp.type], val=[$val]")
      if (shouldSave) {
        gElt.setProperty(prop, val)
      }
    }
    entity.id = gElt.id
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
    entity.id = element.id
    log.debug("reconstitute: entity=[$className], id=[$element.id]")
    element.getPropertyKeys().each {
      def val = element.getProperty(it)
      // if not primitive, need to recurse or something...
      log.debug("reconstitute: key=[$it], val=[$val]")
      entity[it] = val
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
    !(metaProp.name in nameExcludes) && (metaProp.type.isPrimitive() || metaProp.type in classIncludes)
  }
}
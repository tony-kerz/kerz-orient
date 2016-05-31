package com.kerz.orient

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.Assert

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.Element
import com.tinkerpop.blueprints.impls.orient.OrientGraph

class OrientHelper {
  static Logger log = LoggerFactory.getLogger(OrientHelper)
  
  @Autowired
  OrientGraph g
  
  def static nameExcludes = ['metaClass', 'class', 'id', 'element']
  def static classIncludes = [String, Date]
  
  def save (Class clazz, def entity) {
    if (!entity.element) {
      String oClassName = "class:$clazz.simpleName"
      entity.element = g.addVertex(oClassName)
    }
    
    entity.properties.each { prop, val ->
      MetaProperty metaProp = entity.class.metaClass.getMetaProperty(prop)
      boolean shouldSave = shouldSave(metaProp)
      log.trace("save: save=[$shouldSave], prop=[$prop], type=[$metaProp.type], val=[$val]")
      if (shouldSave && val) {
        entity.element.setProperty(prop, toStorage(metaProp, val))
      }
    }
    
    entity
  }
  
  def reconstitute(Class clazz, Element element) {
    Assert.notNull(element, 'element required')
    def entity = clazz.newInstance()
    entity.element = element
    log.trace("reconstitute: entity=[$clazz], id=[$element.id]")
    element.getPropertyKeys().each {
      def val = element.getProperty(it)
      log.trace("reconstitute: key=[$it], val=[$val]")
      MetaProperty metaProp = entity.class.metaClass.getMetaProperty(it)
      entity[it] = fromStorage(metaProp.type, val)
    }
    entity
  }
  
  boolean shouldSave(MetaProperty metaProp) {
    //!(metaProp.name in nameExcludes) && (metaProp.type.isPrimitive() || metaProp.type in classIncludes)
    !(metaProp.name in nameExcludes)
  }
  
  def toStorage(MetaProperty metaProp, val) {
    if (metaProp.type.isPrimitive() || metaProp.type in classIncludes) {
      val
    } else {
      asDocument(val)
    }
  }
  
  def asDocument(entity) {
    // to-do: handle nested objects
    Class clazz = entity.class
    def doc = new ODocument(clazz.simpleName)
    entity.properties.each { k, v ->
      MetaProperty metaProp = entity.class.metaClass.getMetaProperty(k)
      boolean should = shouldSave(metaProp)
      log.trace("as-document: should=$should, k=$k, v=$v, type=$metaProp.type")
      if (should && v) {
        doc.field(k, v)
      }
    }
    doc
  }
  
  def fromStorage(Class clazz, val) {
    log.trace("from-storage: clazz=$clazz, val=$val")
    if (val instanceof ODocument) {
      def instance = clazz.newInstance()
      val.toMap().each {k, v ->
        boolean should = shouldReconstituteProp(k)
        log.trace("from-storage: should=$should, k=$k, v=$v")
        if (should) {
          instance[k] = v
        }
      }
      instance
    } else {
      val
    }
  }
  
  boolean shouldReconstituteProp(String prop) {
    !(prop in ['@rid', '@class'])
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
  
  def submitSql(String sql) {
    log.debug("submit-sql: sql=$sql")
    g.command(new OCommandSQL(sql)).execute()
  }
}
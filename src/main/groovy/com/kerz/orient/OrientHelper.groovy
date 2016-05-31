package com.kerz.orient

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired

import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.impls.orient.OrientGraph

class OrientHelper {
  static Log log = LogFactory.getLog(OrientHelper)
  
  @Autowired
  OrientGraph g
  
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
  
  void submitSql(String sql) {
    g.command(new OCommandSQL(sql)).execute()
  }
}
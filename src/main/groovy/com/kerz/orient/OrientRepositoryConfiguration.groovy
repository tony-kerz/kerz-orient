package com.kerz.orient

import groovy.transform.CompileStatic

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory
import com.tinkerpop.gremlin.groovy.Gremlin

@CompileStatic
@Configuration
@EnableConfigurationProperties([OrientProperties])
class OrientRepositoryConfiguration {
  static {
    Gremlin.load()
  }

  static Logger log = LoggerFactory.getLogger(OrientRepositoryConfiguration)
  
  @Autowired
  OrientProperties props

  @Autowired
  OrientGraphFactory orientGraphFactory

  @Bean
  OrientGraph graph() {
    orientGraphFactory.tx
  }

  @Bean
  OrientGraphFactory orientGraphFactory() {
    log.debug("props=${props}")
    new OrientGraphFactory("remote:$props.host/$props.db", props.user, props.password)
  }
  
  @Bean
  OrientHelper orientHelper() {
    new OrientHelper()
  }
}



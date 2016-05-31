package com.kerz.orient.domain

import groovy.transform.EqualsAndHashCode

import org.apache.commons.lang3.builder.ReflectionToStringBuilder

@EqualsAndHashCode
class OPoint {
  Float[] coordinates
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this)
  }
}

// http://orientdb.com/docs/2.2/Spatial-Module.html#geometry-data-example
// http://stackoverflow.com/a/37086606/2371903

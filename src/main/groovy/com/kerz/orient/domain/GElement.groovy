package com.kerz.orient.domain

import groovy.transform.EqualsAndHashCode

import org.apache.commons.lang3.builder.ReflectionToStringBuilder

import com.tinkerpop.blueprints.Element

@EqualsAndHashCode
class GElement {
  Element element

  String getId() {
    element.id ?: null
  }
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this)
  }
}

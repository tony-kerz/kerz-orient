package com.kerz.orient.domain

import groovy.transform.EqualsAndHashCode

import org.apache.commons.lang3.builder.ReflectionToStringBuilder

@EqualsAndHashCode
class Person {
  String id
  String firstName
  String lastName
  String address
  float lat
  float lon
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this)
  }
}

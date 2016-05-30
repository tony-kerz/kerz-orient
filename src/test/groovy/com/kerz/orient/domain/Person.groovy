package com.kerz.orient.domain

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Person extends GElement {
  String firstName
  String lastName
  String address
  OPoint point
}

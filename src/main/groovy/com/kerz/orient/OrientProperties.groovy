package com.kerz.orient

import groovy.transform.CompileStatic

import java.lang.reflect.Field

import javax.validation.constraints.NotNull

import org.apache.commons.lang.builder.ReflectionToStringBuilder
import org.springframework.boot.context.properties.ConfigurationProperties

@CompileStatic
@ConfigurationProperties(prefix='orient')
class OrientProperties {
  @NotNull
  String host

  @NotNull
  String db

  @NotNull
  String user

  @NotNull
  String password

  @Override
  public String toString() {
    return (new ReflectionToStringBuilder(this) {
          protected boolean accept(Field f) {
            super.accept(f) && !f.getName().equals('password')
          }
        }).toString();
  }
}

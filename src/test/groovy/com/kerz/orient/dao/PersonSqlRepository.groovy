package com.kerz.orient.dao

import org.springframework.stereotype.Repository

import com.kerz.orient.OrientSqlRepository
import com.kerz.orient.domain.Person

@Repository
class PersonSqlRepository extends OrientSqlRepository<Person, String> {
}

package com.kerz.orient.dao

import org.springframework.stereotype.Repository

import com.kerz.orient.OrientRepository
import com.kerz.orient.domain.Person

@Repository
class PersonRepository extends OrientRepository<Person, String> {
}

package com.kerz.orient.tx

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionException
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.ResourceTransactionManager

import com.tinkerpop.blueprints.impls.orient.OrientGraph

class OrientTransactionManager extends AbstractPlatformTransactionManager implements ResourceTransactionManager {

  static final long serialVersionUID = 1L
  static Logger log = LoggerFactory.getLogger(OrientTransactionManager)

  @Autowired
  OrientGraph g

  @Override
  protected Object doGetTransaction() throws TransactionException {
    log.debug('do-get-tx')
    g // hmmm, what are they gonna do with this :)
  }

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
    log.debug("do-begin: tx=$transaction, tx-def=$definition")
    g.begin()
  }

  @Override
  protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
    log.debug("do-commit: status=$status")
    g.commit() 
  }

  @Override
  protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
    log.debug("do-rollback: status=$status")
    g.rollback()
  } 

  @Override
  public Object getResourceFactory() {
    log.debug('get-resource-factory')
    g
  }
}

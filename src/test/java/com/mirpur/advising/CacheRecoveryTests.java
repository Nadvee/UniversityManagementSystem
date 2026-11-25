package com.mirpur.advising;
import com.mirpur.advising.shared.config.RecoveringCacheManager;
import org.junit.jupiter.api.*;
import org.springframework.cache.*;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.transaction.support.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class CacheRecoveryTests {
 @Test void aFailedPostCommitInvalidationIsRetriedBeforeServingCachedData(){
  var backing=spy(new ConcurrentMapCache("routine"));backing.put("student","stale");
  var redis=mock(RedisCacheManager.class);when(redis.getCache("routine")).thenReturn(backing);
  Cache cache=new RecoveringCacheManager(redis).getCache("routine");
  doThrow(new IllegalStateException("Redis unavailable")).doCallRealMethod().when(backing).clear();
  TransactionSynchronizationManager.initSynchronization();TransactionSynchronizationManager.setActualTransactionActive(true);
  try{cache.clear();verify(backing,never()).clear();for(var sync:TransactionSynchronizationManager.getSynchronizations())sync.afterCommit();}
  finally{TransactionSynchronizationManager.clearSynchronization();TransactionSynchronizationManager.setActualTransactionActive(false);}
  assertThat(cache.get("student",()->"fresh")).isEqualTo("fresh");
  assertThat(cache.get("student",()->"should not load")).isEqualTo("fresh");
  verify(backing,times(2)).clear();
 }
 @Test void rollbackDoesNotEvictCommittedCachedData(){
  var backing=new ConcurrentMapCache("routine");backing.put("student","committed");var redis=mock(RedisCacheManager.class);when(redis.getCache("routine")).thenReturn(backing);
  Cache cache=new RecoveringCacheManager(redis).getCache("routine");TransactionSynchronizationManager.initSynchronization();TransactionSynchronizationManager.setActualTransactionActive(true);
  try{cache.clear();for(var sync:TransactionSynchronizationManager.getSynchronizations())sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);}
  finally{TransactionSynchronizationManager.clearSynchronization();TransactionSynchronizationManager.setActualTransactionActive(false);}
  assertThat(cache.get("student",String.class)).isEqualTo("committed");
 }
}

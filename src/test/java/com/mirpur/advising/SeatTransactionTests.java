package com.mirpur.advising;
import com.mirpur.advising.shared.db.JdbcDatabase;
import com.mirpur.advising.seat.SeatService;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.support.*;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class SeatTransactionTests {
 JdbcDatabase db;StringRedisTemplate redis;SeatService service;
 @BeforeEach void setup(){db=mock(JdbcDatabase.class);redis=mock(StringRedisTemplate.class);service=new SeatService(new com.mirpur.advising.seat.SeatRepository(db),redis,com.mirpur.advising.shared.config.RuntimeSettings.defaults());when(redis.hasKey(anyString())).thenReturn(true);TransactionSynchronizationManager.initSynchronization();}
 @AfterEach void clear(){TransactionSynchronizationManager.clearSynchronization();}
 void finish(int status){for(var s:TransactionSynchronizationManager.getSynchronizations())s.afterCompletion(status);}
 @Test void redisRejectsFullSectionsWithoutTouchingDatabase(){
  when(redis.execute(any(RedisScript.class),anyList())).thenReturn(0L);
  assertThat(service.reserve(7,true)).isFalse();verifyNoInteractions(db);
 }
 @Test void sqlExceptionCompensatesTheRedisReservationOnRollback(){
  when(redis.execute(any(RedisScript.class),anyList())).thenReturn(1L);
  when(db.exec(anyString(),eq(7L))).thenThrow(new DataAccessResourceFailureException("Database unavailable"));
  assertThatThrownBy(()->service.reserve(7,true)).isInstanceOf(DataAccessResourceFailureException.class);
  finish(TransactionSynchronization.STATUS_ROLLED_BACK);
  verify(redis,times(2)).execute(any(RedisScript.class),eq(List.of("mirpur:seat:7")));
 }
 @Test void failedDatabaseGuardIsCompensatedOnlyOnce(){
  when(redis.execute(any(RedisScript.class),anyList())).thenReturn(1L);
  when(db.exec(anyString(),eq(7L))).thenReturn(0);
  assertThat(service.reserve(7,true)).isFalse();finish(TransactionSynchronization.STATUS_ROLLED_BACK);
  verify(redis,times(2)).execute(any(RedisScript.class),eq(List.of("mirpur:seat:7")));
 }
 @Test void redisOutageFallsBackToTheDatabaseGuard(){
  when(redis.hasKey(anyString())).thenThrow(new IllegalStateException("Redis unavailable"));
  when(db.exec(anyString(),eq(7L))).thenReturn(1);
  assertThat(service.reserve(7,true)).isTrue();finish(TransactionSynchronization.STATUS_COMMITTED);
 }
}

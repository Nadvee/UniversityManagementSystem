package com.mirpur.advising;
import com.mirpur.advising.shared.config.CacheConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.*;
class CacheSerializationTests {
 @Test void cachedCourseProjectionRoundTripsWithNumericAndNullFields(){
  var config=new CacheConfig(mock(RedisConnectionFactory.class),new ObjectMapper().findAndRegisterModules(),com.mirpur.advising.shared.config.RuntimeSettings.defaults());
  var pair=config.settings().getValueSerializationPair();
  var row=new LinkedHashMap<String,Object>();row.put("id",5L);row.put("credit",new BigDecimal("3.0"));row.put("faculty",null);row.put("start_at","2026-11-03T09:00");row.put("is_offered",true);
  var value=new ArrayList<Map<String,Object>>();value.add(row);
  var result=(List<Map<String,Object>>)pair.read(pair.write(value));
  assertThat(result).isEqualTo(value);assertThat(result.get(0).get("id")).isInstanceOf(Long.class);
 }
}

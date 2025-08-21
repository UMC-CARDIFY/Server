package com.umc.cardify;

import com.umc.cardify.service.payment.PortoneClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Slf4j
@ActiveProfiles("test")  // 테스트 프로필 사용
class PortoneClientPerformanceTest {

  @Autowired
  private PortoneClient portoneClient;

  @Test
  void testCachePerformance() {
    List<Long> responseTimes = new ArrayList<>();

    // 100번 호출
    for (int i = 0; i < 100; i++) {
      long startTime = System.currentTimeMillis();
      portoneClient.getAccessToken();
      long responseTime = System.currentTimeMillis() - startTime;

      responseTimes.add(responseTime);
      System.out.println("호출 #" + (i+1) + ": " + responseTime + "ms");

    }

    // 통계 출력
    double avg = responseTimes.stream().mapToLong(Long::valueOf).average().orElse(0);
    System.out.println("평균 응답시간: " + avg + "ms");
    System.out.println("총 호출 횟수: " + responseTimes.size());
  }
}
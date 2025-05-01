package com.umc.cardify.config;

import com.siot.IamportRestClient.IamportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IamportConfig {

  @Value("${portone.api-key}")
  private String apiKey;

  @Value("${portone.api-secret}")
  private String apiSecret;

  @Bean
  public IamportClient iamportClient() {
    return new IamportClient(apiKey, apiSecret);
  }
}

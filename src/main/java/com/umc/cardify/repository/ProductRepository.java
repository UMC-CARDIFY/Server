package com.umc.cardify.repository;

import com.umc.cardify.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

  // 활성화된(is_active=true) 상품 목록 조회
  List<Product> findByIsActiveTrue();

  // 현재 유효한 상품 목록 조회 (활성화 상태이며 유효 기간 내인 상품)
  List<Product> findByIsActiveTrueAndValidFromBeforeAndValidUntilAfter(
      Timestamp currentTime, Timestamp currentTime2);

  // 상품명으로 검색
  Optional<Product> findByNameAndIsActiveTrue(String name);

  // 주기별 상품 목록 조회
  List<Product> findByPeriodAndIsActiveTrue(String period);

  // 가격 범위로 상품 목록 조회
  List<Product> findByPriceBetweenAndIsActiveTrue(int minPrice, int maxPrice);
}

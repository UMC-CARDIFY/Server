package com.umc.cardify.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.cardify.domain.Overlay;

public interface OverlayRepository extends JpaRepository<Overlay, Long> {
}

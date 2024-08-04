package com.umc.cardify.repository;

import com.umc.cardify.domain.LibraryCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryCategoryRepository extends JpaRepository<LibraryCategory, Long> {
}

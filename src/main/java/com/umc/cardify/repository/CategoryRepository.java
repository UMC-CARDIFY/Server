package com.umc.cardify.repository;

import com.umc.cardify.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Category findByName(String Name);
}

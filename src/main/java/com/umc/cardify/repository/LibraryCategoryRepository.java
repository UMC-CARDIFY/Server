package com.umc.cardify.repository;

import com.umc.cardify.domain.Category;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.LibraryCategory;
import com.umc.cardify.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibraryCategoryRepository extends JpaRepository<LibraryCategory, Long> {
    List<LibraryCategory> findByCategory(Category category);
}

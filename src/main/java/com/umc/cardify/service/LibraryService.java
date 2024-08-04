package com.umc.cardify.service;

import com.umc.cardify.domain.Category;
import com.umc.cardify.dto.library.LibraryResponse;
import com.umc.cardify.repository.CategoryRepository;
import com.umc.cardify.repository.LibraryCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LibraryService {
    private final CategoryRepository categoryRepository;
    private final LibraryCategoryRepository libraryCategoryRepository;
    public List<LibraryResponse.LibraryInfoDTO> getCategory(){
        List<Category> categoryList = categoryRepository.findAll();
        List<LibraryResponse.LibraryInfoDTO> resultDTO = categoryList.stream()
                .map(category -> {
                    int count = libraryCategoryRepository.findByCategory(category).size();
                    return LibraryResponse.LibraryInfoDTO.builder()
                            .categoryId(category.getCategoryId())
                            .categoryName(category.getName())
                            .noteCount(count)
                            .build();
                })
                .collect(Collectors.toList());
        return resultDTO;
    }
}

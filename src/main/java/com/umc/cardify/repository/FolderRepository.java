package com.umc.cardify.repository;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    Page<Folder> findByUser(User userId, Pageable pageable);

    Optional<Folder> findByFolderIdAndUser(Long folderId, User userId);

    boolean existsByUserAndName(User user, String name);

    Page<Folder> findByUserAndColor(User user, String color, Pageable pageable);
    List<Folder> findByUserAndColor(User user, String color, Sort sort);
}

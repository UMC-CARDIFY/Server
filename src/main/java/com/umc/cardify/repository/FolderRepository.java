package com.umc.cardify.repository;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    Page<Folder> findByUser(User userId, Pageable pageable);

    Optional<Folder> findByFolderIdAndUser(Long folderId, User userId);
}

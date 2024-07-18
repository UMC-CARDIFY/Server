package com.umc.cardify.repository;

import com.umc.cardify.domain.Folder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    Page<Folder> findAllByFolderId(Long folderId, Pageable pageable);
}

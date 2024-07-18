package com.umc.cardify.repository;

import com.umc.cardify.domain.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findAllByFolderId(Long folderId);

}

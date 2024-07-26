package com.umc.cardify.repository;

import com.umc.cardify.domain.Card;
import com.umc.cardify.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByNote(Note note);
}

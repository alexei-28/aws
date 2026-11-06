package com.gmail.alexei28.aws.dbs.repo;

import com.gmail.alexei28.aws.dbs.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/*
    Здесь PostgreSQL отвечает за:
    - регистрацию документов;
    - поиск документов пользователя;
    - проверку принадлежности документа пользователю.
*/
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    Page<DocumentEntity> findAllByUserId(UUID userId, Pageable pageable);

    // метод, используемый сервисом для проверки владельца
    Optional<DocumentEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
package com.ptos.repository;

import com.ptos.domain.ClientRecord;
import com.ptos.domain.ClientRecordNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRecordNoteRepository extends JpaRepository<ClientRecordNote, Long> {

    List<ClientRecordNote> findByClientRecordOrderByCreatedAtDesc(ClientRecord clientRecord);
}

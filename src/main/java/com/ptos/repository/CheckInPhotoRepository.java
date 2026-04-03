package com.ptos.repository;

import com.ptos.domain.CheckIn;
import com.ptos.domain.CheckInPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CheckInPhotoRepository extends JpaRepository<CheckInPhoto, Long> {

    @Query("select photo from CheckInPhoto photo where photo.checkIn = :checkIn")
    List<CheckInPhoto> findByCheckIn(@Param("checkIn") CheckIn checkIn);

    @Query("select photo from CheckInPhoto photo where photo.checkIn.id = :checkInId")
    List<CheckInPhoto> findByCheckInId(@Param("checkInId") Long checkInId);
}

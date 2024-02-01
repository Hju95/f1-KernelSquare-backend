package com.kernel360.kernelsquare.domain.reservation.repository;

import com.kernel360.kernelsquare.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findAllByReservationArticleId(Long articleId);

    Long countByReservationArticleIdAndMemberIdIsNull(Long articleId);

    @Modifying
    @Query("DELETE FROM Reservation a WHERE a.reservationArticle.id = :postId")
    void deleteAllByReservationArticleId(@Param("postId") Long postId);

    Long countAllByReservationArticleId(Long articleId);

    @Query("SELECT MIN(a.startTime) FROM Reservation a WHERE a.reservationArticle.id = :articleId GROUP BY a.reservationArticle.id")
    LocalDateTime findStartTimeByReservationArticleId(@Param("articleId") Long articleId);

}

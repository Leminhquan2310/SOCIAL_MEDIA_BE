package com.social_media_be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "app_access_stats", indexes = {
    @Index(name = "IDX_STAT_DATE", columnList = "access_date")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppAccessStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_date", nullable = false, unique = true)
    private LocalDate accessDate;

    @Builder.Default
    @Column(name = "visit_count", nullable = false)
    private Long visitCount = 0L;
}

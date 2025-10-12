package com.pitterpetter.loventure.territory.domain.coupleregion;

import com.pitterpetter.loventure.territory.domain.common.BaseEntity;
import com.pitterpetter.loventure.territory.domain.region.Region;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "couple_region",
        uniqueConstraints = @UniqueConstraint(columnNames = {"couple_id", "region_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoupleRegion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = true;

    @Column(name = "selected_by", length = 10)
    private String selectedBy;  // "male" | "female"

    @Column(name = "unlock_type", length = 10)
    private String unlockType = "INIT";  // INIT | TICKET

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;
}

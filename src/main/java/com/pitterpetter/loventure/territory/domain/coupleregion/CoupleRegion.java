package com.pitterpetter.loventure.territory.domain.coupleregion;

import com.pitterpetter.loventure.territory.domain.common.BaseEntity;
import com.pitterpetter.loventure.territory.domain.region.Region;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = true;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;
}

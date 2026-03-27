package com.t1.popcon.popup.banners.repository;

import com.t1.popcon.popup.banners.entity.Banner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    @Query("select b from Banner b join fetch b.popup p where b.isActive = true order by b.priority asc")
    List<Banner> findActiveBannersWithPopup(Pageable pageable);
}

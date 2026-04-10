package com.t1.popcon.popup.categories.repository;

import com.t1.popcon.popup.categories.entity.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 활성 카테고리를 priority 오름차순으로 조회
    @Query("select c from Category c join fetch c.popup p where c.isActive = true order by c.priority asc")
    List<Category> findActiveCategoriesWithPopup(Pageable pageable);
}

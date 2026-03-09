package com.t1.popcon.magazine.service;

import com.t1.popcon.magazine.dto.card.MagazineCardDto;
import com.t1.popcon.magazine.dto.section.MagazineSectionResponse;
import com.t1.popcon.magazine.entity.Magazine;
import com.t1.popcon.magazine.repository.MagazineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MagazineService {

    private final MagazineRepository magazineRepository;

    public MagazineSectionResponse getTopMagazines(int limit) {
        List<Magazine> magazines = magazineRepository.findByDeletedFalseOrderByPublishedAtDesc(
                PageRequest.of(0, limit)
        );

        List<MagazineCardDto> items = magazines.stream()
                .map(MagazineCardDto::from)
                .toList();

        return MagazineSectionResponse.of(items);
    }
}

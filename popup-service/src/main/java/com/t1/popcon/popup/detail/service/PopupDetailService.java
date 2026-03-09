package com.t1.popcon.popup.detail.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.t1.popcon.popup.detail.dto.PopupDetailResponse;
import com.t1.popcon.popup.detail.repository.PopupRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopupDetailService {

	private final PopupRepository popupRepository;

	public PopupDetailResponse getPopupDetail(Long popupId) {
		return PopupDetailResponse.ofMock(popupId);
	}
}

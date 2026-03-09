package com.t1.popcon.popup.detail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.t1.popcon.popup.detail.entity.Popup;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long> {

}

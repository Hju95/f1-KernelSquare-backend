package com.kernelsquare.domainmysql.domain.notice.repository;

import org.springframework.stereotype.Component;

import com.kernelsquare.domainmysql.domain.notice.entity.Notice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeStoreImpl implements NoticeStore {
	private final NoticeRepository noticeRepository;

	@Override
	public Notice store(Notice initNotice) {
		return noticeRepository.save(initNotice);
	}

	@Override
	public void delete(String noticeToken) {
		noticeRepository.deleteByNoticeToken(noticeToken);
	}
}

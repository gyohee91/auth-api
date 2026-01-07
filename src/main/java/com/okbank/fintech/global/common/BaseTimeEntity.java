package com.okbank.fintech.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 시간 정보만 포함하는 Base Entity
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @CreatedDate
    @Column(nullable = false, insertable = true, updatable = false)
    @Comment("등록일")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false, insertable = true, updatable = true)
    @Comment("수정일")
    private LocalDateTime updatedAt;
}

package com.okbank.fintech.global.common;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 시간 + 생성자/수정자 정보를 포함하는 Base Entity
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("일련번호")
    private Long id;

    @CreatedBy
    @Column(nullable = false, length = 50, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(nullable = false, length = 50)
    private String updatedBy;
}

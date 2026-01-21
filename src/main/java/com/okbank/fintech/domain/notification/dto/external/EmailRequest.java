package com.okbank.fintech.domain.notification.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    private String emailAddress;
    private String title;
    private String contents;
}

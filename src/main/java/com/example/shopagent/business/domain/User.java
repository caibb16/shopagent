package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {
    private Long id;
    private String name;
    private String level; // NORMAL | VIP | BLACKLIST
}

package com.example.shopagent.business.domain;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user")
public class User {
    @Id
    private Long id;
    private String name;
    private String level; // NORMAL | VIP | BLACKLIST
}

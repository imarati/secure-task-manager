package com.marat.taskmanager.securetaskmanager.entity;

import com.marat.taskmanager.securetaskmanager.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, nullable = false, length = 20)
    private String email;

    @Column(name = "password_hash",nullable = false)
    private String passwordHash;

    @Column(name = "display_name",nullable = false, length = 20)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.role == null) {
            this.role = Role.ROLE_USER;
        }
        this.createdAt = Instant.now();
        this.enabled = true;
    }
}

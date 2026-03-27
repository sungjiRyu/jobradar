package com.jobradar.backend.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 회원 엔티티 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /* 비밀번호는 BCrypt로 암호화하여 저장 */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(updatable = false) // 최초 생성 시에만 설정, 이후 변경 불가
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * @PrePersist: 엔티티가 DB에 처음 저장(INSERT)될 때 자동 실행
     * @PreUpdate: 엔티티가 DB에 수정(UPDATE)될 때 자동 실행
     * → 생성일/수정일 자동 관리
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /* Builder 패턴으로만 생성 가능하도록 강제 */
    @Builder
    public User(String email, String password, String nickname, Role role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = (role != null) ? role : Role.USER; // 기본값 USER
    }

    // ===== 비즈니스 메서드 =====
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // ===== 권한 Enum =====
    public enum Role {
        USER,   // 일반 회원
        ADMIN   // 관리자
    }
}

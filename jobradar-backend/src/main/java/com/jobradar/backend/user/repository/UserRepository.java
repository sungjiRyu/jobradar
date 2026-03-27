package com.jobradar.backend.user.repository;

import com.jobradar.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/** 회원 레포지토리 */
public interface UserRepository extends JpaRepository<User, Long> {
}

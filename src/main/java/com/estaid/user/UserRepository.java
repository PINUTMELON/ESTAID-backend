package com.estaid.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    /** username으로 사용자 조회 (중복 존재 시 첫 번째 결과 반환) */
    Optional<User> findFirstByUsername(String username);
}

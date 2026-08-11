package com.mpa.repository;

import com.mpa.entity.Role;
import com.mpa.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRoleAndActiveTrue(Role role);

    @Query("""
        SELECT u FROM User u
        WHERE ('' = :search
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:role IS NULL OR u.role = :role)
          AND (:active IS NULL OR u.active = :active)
          AND (:maDonViCap6 IS NULL OR u.maDonViCap6 = :maDonViCap6)
        ORDER BY u.fullName
        """)
    Page<User> search(@Param("search") String search,
                       @Param("role") Role role,
                       @Param("active") Boolean active,
                       @Param("maDonViCap6") String maDonViCap6,
                       Pageable pageable);
}

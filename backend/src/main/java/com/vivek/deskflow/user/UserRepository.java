package com.vivek.deskflow.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // Every lookup below includes tenantId: that is the tenant isolation rule in code.
    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    List<User> findByTenantIdOrderByNameAsc(Long tenantId);

    List<User> findByTenantIdAndRoleInAndActiveTrueOrderByNameAsc(Long tenantId, Collection<Role> roles);
}

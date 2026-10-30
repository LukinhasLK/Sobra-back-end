package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository  extends JpaRepository<User,Long> {
}

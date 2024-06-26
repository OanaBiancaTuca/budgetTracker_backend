package com.example.springapp.goals;

import com.example.springapp.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalsRepository extends JpaRepository<Goal, Long> {
    List<Goal> findAllByUser(UserEntity user);

    Optional<Goal> findById(Long id);
}

package com.example.springapp.category;

import com.example.springapp.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Integer> {
    public List<Category> findAllByUserId(UserEntity userId);

    Category findByName(String categoryName);

    Category findByNameAndUserId(String categoryName, UserEntity currentUser);
}

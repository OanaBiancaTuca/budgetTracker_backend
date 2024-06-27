package com.example.springapp.transaction.pdf;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PdfRepository extends JpaRepository<PdfEntity, Long> {
}

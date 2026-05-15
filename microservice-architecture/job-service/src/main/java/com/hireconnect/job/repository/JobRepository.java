package com.hireconnect.job.repository;

import com.hireconnect.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Integer> {
    
    Optional<Job> findByTitle(String title);
    List<Job> findByCategory(String category);
    List<Job> findByLocation(String location);
    List<Job> findByPostedBy(int postedBy);
    List<Job> findByStatus(String status);
    List<Job> findByTitleContainingIgnoreCase(String keyword);

    // Custom Query for the complex searchJobs method
    @Query("SELECT j FROM Job j WHERE " +
           "(:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:category IS NULL OR LOWER(j.category) = LOWER(:category)) AND " +
           "(j.salaryMin >= :minSalary) AND " +
           "(j.salaryMax <= :maxSalary OR :maxSalary = 0.0)")
    List<Job> searchJobs(@Param("title") String title, 
                         @Param("category") String category, 
                         @Param("minSalary") double minSalary, 
                         @Param("maxSalary") double maxSalary);
}
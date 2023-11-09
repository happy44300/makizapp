package fr.makizart.common.database.repositories;

import fr.makizart.common.database.table.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project,Long> {
    @Query(value = "SELECT id from Project")
    List<Long> findAllID();
}


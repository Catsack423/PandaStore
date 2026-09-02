package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.review.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

}

package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.review.ReviewReply;

import java.util.Optional;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {
    Optional<ReviewReply> findByReview_ReviewId(Long reviewId);
}

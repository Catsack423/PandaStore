package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.review.ReviewReply;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {

}

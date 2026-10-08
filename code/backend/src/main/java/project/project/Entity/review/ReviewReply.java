package project.project.Entity.review;

import jakarta.persistence.*;
import project.project.Entity.seller.Seller;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_replies")
public class ReviewReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long replyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(name = "reply_message", columnDefinition = "TEXT", nullable = false)
    private String replyMessage;

    @Column(name = "replied_at", nullable = false)
    private LocalDateTime repliedAt;

    public ReviewReply() {
    }

    public ReviewReply(Long replyId, Review review, Seller seller, String replyMessage, LocalDateTime repliedAt) {
        this.replyId = replyId;
        this.review = review;
        this.seller = seller;
        this.replyMessage = replyMessage;
        this.repliedAt = repliedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.repliedAt = LocalDateTime.now();
    }

    public Long getReplyId() {
        return replyId;
    }

    public void setReplyId(Long replyId) {
        this.replyId = replyId;
    }

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public String getReplyMessage() {
        return replyMessage;
    }

    public void setReplyMessage(String replyMessage) {
        this.replyMessage = replyMessage;
    }

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }
}

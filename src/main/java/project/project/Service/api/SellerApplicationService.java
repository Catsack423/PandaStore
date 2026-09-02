package project.project.Service.api;

import project.project.Entity.seller.SellerApplication;
import java.util.List;
//Admin Function
public interface SellerApplicationService {
    SellerApplication submitApplication(Long userId, SellerApplication application);
    void approveApplication(Long applicationId, Long adminId);
    void rejectApplication(Long applicationId, Long adminId, String reason);
    void requestMoreDocuments(Long applicationId, Long adminId, String message);
    SellerApplication getApplicationById(Long applicationId);
    List<SellerApplication> getPendingApplications();
}

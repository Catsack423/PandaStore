package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.*;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.seller.CreateSellerApplicationRequest;
import project.project.DTO.seller.RejectApplicationRequest;
import project.project.DTO.seller.RequestMoreDocumentsRequest;
import project.project.DTO.seller.SellerApplicationResponse;
import project.project.Entity.seller.SellerApplication;
import project.project.Service.api.SellerApplicationService;
import project.project.Security.CurrentUser;
import project.project.Entity.user.UserRole;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/seller-applications")
public class SellerApplicationController {

    private final SellerApplicationService sellerApplicationService;
    private final CurrentUser currentUser;

    public SellerApplicationController(SellerApplicationService sellerApplicationService, CurrentUser currentUser) {
        this.sellerApplicationService = sellerApplicationService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SellerApplicationResponse>> submitApplication(
            @Valid @RequestBody CreateSellerApplicationRequest request) {
        currentUser.requireCustomerId();
        SellerApplication saved = sellerApplicationService.submitApplication(currentUser.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ยื่นคำขอเปิดร้านค้าสำเร็จ", SellerApplicationResponse.fromEntity(saved)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<SellerApplicationResponse>>> getMyApplications() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success("ดึงรายการใบสมัครของฉันสำเร็จ",
                sellerApplicationService.getApplicationsForUser(currentUser.getCurrentUserId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SellerApplicationResponse>> getApplicationById(@PathVariable Long id) {
        SellerApplicationResponse response = sellerApplicationService.getApplicationResponseById(id);
        var identity = currentUser.requireIdentity();
        if (identity.role() != UserRole.ADMIN && (response.getUserId() == null || identity.userId() != response.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ไม่มีสิทธิ์ดูใบสมัครนี้");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success("ดึงข้อมูลใบสมัครสำเร็จ", response));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<SellerApplicationResponse>>> getPendingApplications() {
        List<SellerApplicationResponse> responses = sellerApplicationService.getPendingApplicationResponses();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success("ดึงรายการใบสมัครที่รอตรวจสอบสำเร็จ", responses));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveApplication(@PathVariable Long id) {
        sellerApplicationService.approveApplication(id, currentUser.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("อนุมัติคำขอเปิดร้านค้าสำเร็จ", null));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectApplication(
            @PathVariable Long id,
            @Valid @RequestBody RejectApplicationRequest request) {
        sellerApplicationService.rejectApplication(id, currentUser.getCurrentUserId(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("ปฏิเสธคำขอเปิดร้านค้าสำเร็จ", null));
    }

    @PutMapping("/{id}/request-docs")
    public ResponseEntity<ApiResponse<Void>> requestMoreDocuments(
            @PathVariable Long id,
            @Valid @RequestBody RequestMoreDocumentsRequest request) {
        sellerApplicationService.requestMoreDocuments(id, currentUser.getCurrentUserId(), request.getMessage());
        return ResponseEntity.ok(ApiResponse.success("ส่งคำขอเอกสารเพิ่มเติมสำเร็จ", null));
    }
}

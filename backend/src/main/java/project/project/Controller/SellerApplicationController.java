package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.seller.CreateSellerApplicationRequest;
import project.project.DTO.seller.RejectApplicationRequest;
import project.project.DTO.seller.RequestMoreDocumentsRequest;
import project.project.DTO.seller.SellerApplicationResponse;
import project.project.Entity.seller.SellerApplication;
import project.project.Service.api.SellerApplicationService;

import java.util.List;

@RestController
@RequestMapping("/api/seller-applications")
public class SellerApplicationController {

    private final SellerApplicationService sellerApplicationService;

    public SellerApplicationController(SellerApplicationService sellerApplicationService) {
        this.sellerApplicationService = sellerApplicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SellerApplicationResponse>> submitApplication(
            @RequestParam Long userId,
            @Valid @RequestBody CreateSellerApplicationRequest request) {
        SellerApplication saved = sellerApplicationService.submitApplication(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ยื่นคำขอเปิดร้านค้าสำเร็จ", SellerApplicationResponse.fromEntity(saved)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SellerApplicationResponse>> getApplicationById(@PathVariable Long id) {
        SellerApplicationResponse response = sellerApplicationService.getApplicationResponseById(id);
        return ResponseEntity.ok(ApiResponse.success("ดึงข้อมูลใบสมัครสำเร็จ", response));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<SellerApplicationResponse>>> getPendingApplications() {
        List<SellerApplicationResponse> responses = sellerApplicationService.getPendingApplicationResponses();
        return ResponseEntity.ok(ApiResponse.success("ดึงรายการใบสมัครที่รอตรวจสอบสำเร็จ", responses));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveApplication(
            @PathVariable Long id,
            @RequestParam Long adminId) {
        sellerApplicationService.approveApplication(id, adminId);
        return ResponseEntity.ok(ApiResponse.success("อนุมัติคำขอเปิดร้านค้าสำเร็จ", null));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectApplication(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @Valid @RequestBody RejectApplicationRequest request) {
        sellerApplicationService.rejectApplication(id, adminId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("ปฏิเสธคำขอเปิดร้านค้าสำเร็จ", null));
    }

    @PutMapping("/{id}/request-docs")
    public ResponseEntity<ApiResponse<Void>> requestMoreDocuments(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @Valid @RequestBody RequestMoreDocumentsRequest request) {
        sellerApplicationService.requestMoreDocuments(id, adminId, request.getMessage());
        return ResponseEntity.ok(ApiResponse.success("ส่งคำขอเอกสารเพิ่มเติมสำเร็จ", null));
    }
}

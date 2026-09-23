package project.project.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.project.ApiResponse.ApiResponse;
import project.project.Config.TestDataInitializer;

@RestController
@RequestMapping("/api/test")
public class TestSupportController {

    private final TestDataInitializer testDataInitializer;

    public TestSupportController(TestDataInitializer testDataInitializer) {
        this.testDataInitializer = testDataInitializer;
    }

    @PostMapping("/reset-data")
    public ResponseEntity<ApiResponse<String>> resetData() {
        testDataInitializer.resetTestData();
        return ResponseEntity.ok(ApiResponse.success("รีเซ็ตและเตรียมข้อมูลทดสอบเรียบร้อย", "OK"));
    }
}

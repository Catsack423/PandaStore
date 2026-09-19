package project.project.DTO.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerBankAccountRequest {

    @NotBlank(message = "ชื่อธนาคารห้ามว่าง")
    @Size(max = 100, message = "ชื่อธนาคารต้องไม่เกิน 100 ตัวอักษร")
    private String bankName;

    @NotBlank(message = "เลขที่บัญชีห้ามว่าง")
    @Size(max = 30, message = "เลขที่บัญชีต้องไม่เกิน 30 ตัวอักษร")
    private String accountNumber;

    @NotBlank(message = "ชื่อบัญชีห้ามว่าง")
    @Size(max = 100, message = "ชื่อบัญชีต้องไม่เกิน 100 ตัวอักษร")
    private String accountName;

    @Size(max = 255, message = "URL รูปภาพต้องไม่เกิน 255 ตัวอักษร")
    private String proofImageUrl;

    public SellerBankAccountRequest() {
    }

    public SellerBankAccountRequest(String bankName, String accountNumber, String accountName, String proofImageUrl) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.proofImageUrl = proofImageUrl;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getProofImageUrl() {
        return proofImageUrl;
    }

    public void setProofImageUrl(String proofImageUrl) {
        this.proofImageUrl = proofImageUrl;
    }
}

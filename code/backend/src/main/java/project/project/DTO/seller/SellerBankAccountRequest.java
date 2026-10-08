package project.project.DTO.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerBankAccountRequest {

    @NotBlank(message = "Bank name is required")
    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    @NotBlank(message = "Bank account number is required")
    @Size(max = 30, message = "Bank account number must not exceed 30 characters")
    private String accountNumber;

    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name must not exceed 100 characters")
    private String accountName;

    @Size(max = 255, message = "Image URL must not exceed 255 characters")
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

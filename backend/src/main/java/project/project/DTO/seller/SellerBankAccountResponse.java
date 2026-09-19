package project.project.DTO.seller;

import lombok.Getter;
import lombok.Setter;
import project.project.Entity.seller.SellerBankAccount;

@Getter
@Setter
public class SellerBankAccountResponse {

    private Long bankAccountId;
    private Long sellerId;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String proofImageUrl;

    public SellerBankAccountResponse() {
    }

    public SellerBankAccountResponse(Long bankAccountId, Long sellerId, String bankName,
                                     String accountNumber, String accountName, String proofImageUrl) {
        this.bankAccountId = bankAccountId;
        this.sellerId = sellerId;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.proofImageUrl = proofImageUrl;
    }

    public static SellerBankAccountResponse fromEntity(SellerBankAccount account) {
        if (account == null) {
            return null;
        }

        Long sellerId = (account.getSeller() != null) ? account.getSeller().getSellerId() : null;

        return new SellerBankAccountResponse(
                account.getBankAccountId(),
                sellerId,
                account.getBankName(),
                account.getAccountNumber(),
                account.getAccountName(),
                account.getProofImageUrl()
        );
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
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

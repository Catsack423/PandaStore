package project.project.Service.api;

import java.util.List;

import project.project.DTO.seller.CreateSellerRequest;
import project.project.DTO.seller.UpdateSellerRequest;
import project.project.Entity.seller.Seller;

public interface SellerService {
    Seller createSeller(CreateSellerRequest request);

    boolean deleteSeller(long id);

    boolean updateSeller(long id, UpdateSellerRequest request);

    Seller getSeller(long id);

    List<Seller> getAllSeller();
}

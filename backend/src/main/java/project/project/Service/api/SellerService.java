package project.project.Service.api;

import java.util.List;

import project.project.Entity.seller.Seller;



public interface SellerService {
    //TODO: Set Parameter
    long createSeller();

    boolean deleteSeller(long id);

    boolean updateSeller(long id );

    Seller getSeller(long id);

    List<Seller> getAllSeller();
}

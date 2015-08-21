package org.openlmis.stockmanagement.repository;

import lombok.NoArgsConstructor;
import org.openlmis.stockmanagement.dto.StockCard;
import org.openlmis.stockmanagement.repository.mapper.StockCardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor
public class StockCardRepository {

  @Autowired
  StockCardMapper mapper;

  public StockCard getStockCard(Long facilityId, Long productId) {
    return mapper.getByFacilityAndProduct(facilityId, productId);
  }

  public StockCard getStockCardById(Long id) {
    return mapper.getById(id);
  }

  public List<StockCard> getStockCards(Long facilityId) {
    return mapper.getAllByFacility(facilityId);
  }
}

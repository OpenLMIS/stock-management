package org.openlmis.stockmanagement.repository;

import lombok.NoArgsConstructor;
import org.openlmis.stockmanagement.domain.Lot;
import org.openlmis.stockmanagement.domain.LotOnHand;
import org.openlmis.stockmanagement.repository.mapper.LotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class LotRepository {

  @Autowired
  LotMapper mapper;

  public LotOnHand getLotOnHandByStockCardAndLot(Long stockCardId, Long lotId) {
    return mapper.getLotOnHandByStockCardAndLot(stockCardId, lotId);
  }

  public LotOnHand getLotOnHandByStockCardAndLotObject(Long stockCardId, Lot lot) {
    return mapper.getLotOnHandByStockCardAndLotObject(stockCardId, lot);
  }

  public Lot getByObject(Lot lot) {
    return mapper.getByObject(lot);
  }

  public void saveLot(Lot lot) {
    mapper.insert(lot);
  }

  public void saveLotOnHand(LotOnHand lotOnHand) {
    if (null == lotOnHand.getId()) {
      mapper.insertLotOnHand(lotOnHand);
    } else {
      mapper.updateLotOnHand(lotOnHand);
    }
  }
}

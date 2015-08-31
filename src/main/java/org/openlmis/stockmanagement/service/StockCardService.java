/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.stockmanagement.service;

import lombok.NoArgsConstructor;
import org.openlmis.core.domain.*;
import org.openlmis.core.service.*;
import org.openlmis.stockmanagement.domain.Lot;
import org.openlmis.stockmanagement.domain.StockCard;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * Exposes the services for handling stock cards.
 */

@Service
@NoArgsConstructor
public class StockCardService {

  @Autowired
  FacilityService facilityService;

  @Autowired
  ProductService productService;

  @Autowired
  StockCardRepository repository;

  public List<Lot> getLots(Long productId) {
    return getTestLots(productId);
  }

  public StockCard getStockCard(Long facilityId, Long productId) {
    return repository.getStockCard(facilityId, productId);
  }

  public StockCard getStockCardById(Long facilityId, Long stockCardId) {
    return repository.getStockCardById(facilityId, stockCardId);
  }

  public List<StockCard> getStockCards(Long facilityId) {
    return repository.getStockCards(facilityId);
  }

  private List<Lot> getTestLots(Long productId) {
    List<Lot> lots = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      Product product = productService.getById(productId);
      if (product != null) {
        Lot lot = new Lot();
        lot.setId((long) i);
        lot.setProduct(product);
        lot.setLotCode("AB-" + i);
        lot.setManufacturerName("MyManufacturer");

        Date now = new Date();
        lot.setManufactureDate(now);

        Calendar c = Calendar.getInstance();
        c.setTime(now);
        c.add(Calendar.DATE, 365);
        lot.setExpirationDate(c.getTime());

        lots.add(lot);
      }
    }

    return lots;
  }
}

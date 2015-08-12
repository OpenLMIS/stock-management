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
import org.openlmis.core.dto.Lot;
import org.openlmis.core.dto.StockCard;
import org.openlmis.core.dto.StockCardLineItem;
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

  public List<Lot> getLots(Long productId) {
    return getTestLots(productId);
  }

  public StockCard getStockCardByProduct(Long facilityId, Long productId) {
    return getTestStockCard(1L, facilityId, productId, 2400L, 2400L);
  }

  public StockCard getStockCard(Long facilityId, Long stockCardId) {
    return getTestStockCard(stockCardId, facilityId, 2412L, 2400L, 2400L);
  }

  public List<StockCard> getStockCards(Long facilityId) {
    Long[] productIdData = {2412L,2418L,2421L,2413L,2416L,2415L,2422L,2423L,2425L,2426L};
    Long[] quantityData = {2400L,1000L,500L,1000L,400L,1000L,300L,500L,1000L,200L};
    Long[] quantityData2 = {200L,2400L,1000L,500L,2400L,1300L,200L,600L,200L,10L};

    List<StockCard> stockCards = new ArrayList<>();
    for (int i = 0; i < productIdData.length; i++) {
      StockCard stockCard = getTestStockCard((long)i, facilityId, productIdData[i], quantityData[i], quantityData2[i]);
      if (stockCard != null) {
        stockCards.add(stockCard);
      }
    }

    return stockCards;
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

  private StockCard getTestStockCard(Long id, Long facilityId, Long productId, Long quantity1, Long quantity2) {
    Facility facility = facilityService.getById(facilityId);
    Product product = productService.getById(productId);

    if (facility == null || product == null) {
      return null;
    }

    StockCard stockCard = new StockCard();

    stockCard.setId(id);
    stockCard.setFacility(facility);
    stockCard.setProduct(product);
    if (facility.getFacilityType().getCode().equalsIgnoreCase("dvs")) {
      stockCard.setTotalQuantityOnHand(quantity1);
    } else {
      stockCard.setTotalQuantityOnHand(quantity2);
    }
    stockCard.setEffectiveDate(new Date());
    stockCard.setNotes("Test stock card for " + product.getPrimaryName());

    String[][] lineItemData = {
            {"2","DEBIT","10"},
            {"3","DEBIT","5"},
            {"4","DEBIT","15"},
            {"5","DEBIT","10"},
            {"6","DEBIT","5"},
            {"7","DEBIT","5"},
            {"8","DEBIT","10"},
            {"9","DEBIT","15"},
            {"10","DEBIT","10"},
            {"11","DEBIT","10"},
            {"1","CREDIT","200"}
    };
    List<StockCardLineItem> lineItems = new ArrayList<>();
    for (String[] item : lineItemData) {
      StockCardLineItem lineItem = new StockCardLineItem();
      lineItem.setId(Long.parseLong(item[0]));
      lineItem.setType(StockCardLineItemType.valueOf(item[1]));
      lineItem.setQuantity(Long.parseLong(item[2]));
      lineItems.add(lineItem);
    }
    stockCard.setLineItems(lineItems);

    return stockCard;
  }
}

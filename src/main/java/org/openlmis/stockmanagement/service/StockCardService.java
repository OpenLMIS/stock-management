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
import org.openlmis.core.repository.ProductRepository;
import org.openlmis.core.service.*;
import org.openlmis.stockmanagement.domain.Lot;
import org.openlmis.stockmanagement.domain.LotOnHand;
import org.openlmis.stockmanagement.domain.StockCard;
import org.openlmis.stockmanagement.domain.StockCardEntry;
import org.openlmis.stockmanagement.repository.LotRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


/**
 * Exposes the services for handling stock cards.
 */

@Service
@NoArgsConstructor
public class StockCardService {

  @Autowired
  FacilityService facilityService;

  @Autowired
  ProductRepository productRepository;

  @Autowired
  LotRepository lotRepository;

  @Autowired
  StockCardRepository repository;

  StockCardService(FacilityService facilityService,
                   ProductRepository productRepository,
                   LotRepository lotRepository,
                   StockCardRepository repository) {
    this.facilityService = Objects.requireNonNull(facilityService);
    this.productRepository = Objects.requireNonNull(productRepository);
    this.lotRepository = Objects.requireNonNull(lotRepository);
    this.repository = Objects.requireNonNull(repository);
  }

  @Transactional
  public LotOnHand getOrCreateLotOnHand(Lot lot, StockCard stockCard) {
    LotOnHand lotOnHand = lotRepository.getLotOnHandByStockCardAndLotObject(stockCard.getId(), lot);
    if (null == lotOnHand) {
      Lot l = lotRepository.getByObject(lot);
      if (null != l) {
        lotRepository.saveLot(lot);
        l = lot;
      }
      lotOnHand = LotOnHand.createZeroedLotOnHand(l, stockCard);
      lotRepository.saveLotOnHand(lotOnHand);
    }

    Objects.requireNonNull(lotOnHand);
    return lotOnHand;
  }

  public StockCard getOrCreateStockCard(Long facilityId, Long productId) {
    return repository.getOrCreateStockCard(facilityId, productId);
  }

  public StockCard getStockCardById(Long facilityId, Long stockCardId) {
    return repository.getStockCardById(facilityId, stockCardId);
  }

  public List<StockCard> getStockCards(Long facilityId) {
    return repository.getStockCards(facilityId);
  }

  @Transactional
  public void addStockCardEntry(StockCardEntry entry) {
    StockCard card = entry.getStockCard();

    card.addToTotalQuantityOnHand(entry.getQuantity());
    repository.persistStockCardEntry(entry);
    repository.updateStockCard(card);

    LotOnHand lotOnHand = entry.getLotOnHand();
    if (null != lotOnHand) {
      lotOnHand.addToQuantityOnHand(entry.getQuantity());
      lotRepository.saveLotOnHand(lotOnHand);
    }
  }

  @Transactional
  public void addStockCardEntries(List<StockCardEntry> entries) {
    for(StockCardEntry entry : entries) addStockCardEntry(entry);
  }
}

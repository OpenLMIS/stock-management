/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.stockmanagement.repository.mapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openlmis.core.builder.FacilityBuilder;
import org.openlmis.core.builder.ProductBuilder;
import org.openlmis.core.domain.Facility;
import org.openlmis.core.domain.Product;
import org.openlmis.core.repository.mapper.FacilityMapper;
import org.openlmis.core.repository.mapper.ProductMapper;
import org.openlmis.db.categories.IntegrationTests;
import org.openlmis.stockmanagement.domain.StockCard;
import org.openlmis.stockmanagement.domain.StockCardEntry;
import org.openlmis.stockmanagement.domain.StockCardEntryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Category(IntegrationTests.class)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:test-applicationContext-stock-management.xml")
@Transactional
@TransactionConfiguration(defaultRollback = true, transactionManager = "openLmisTransactionManager")
public class StockCardMapperIT {

  @Autowired
  private FacilityMapper facilityMapper;

  @Autowired
  private ProductMapper productMapper;

  @Autowired
  private StockCardMapper mapper;

  private StockCard defaultCard;

  @Before
  public void setup() {
    Facility defaultFacility = make(a(FacilityBuilder.defaultFacility));
    Product defaultProduct = make(a(ProductBuilder.defaultProduct));
    facilityMapper.insert(defaultFacility);
    productMapper.insert(defaultProduct);

    defaultCard = new StockCard();
    defaultCard.setFacility(defaultFacility);
    defaultCard.setProduct(defaultProduct);
    defaultCard.setTotalQuantityOnHand(0L);

    mapper.insertStockCard(defaultCard);
  }

  @Test
  public void shouldInsertEntry() {
    StockCardEntry entry = new StockCardEntry(defaultCard, StockCardEntryType.CREDIT, 1L);
    mapper.insertEntry(entry);

    List<StockCardEntry> entries = mapper.getEntries(defaultCard.getId());
    assertThat(entries.size(), is(1));
  }
}

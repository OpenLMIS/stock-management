/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.stockmanagement.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mockito.Mock;
import org.openlmis.authentication.web.UserAuthenticationSuccessHandler;
import org.openlmis.core.builder.FacilityBuilder;
import org.openlmis.core.builder.ProductBuilder;
import org.openlmis.core.domain.Facility;
import org.openlmis.core.domain.Product;
import org.openlmis.core.domain.StockAdjustmentReason;
import org.openlmis.core.repository.FacilityRepository;
import org.openlmis.core.repository.StockAdjustmentReasonRepository;
import org.openlmis.core.service.MessageService;
import org.openlmis.core.service.ProductService;
import org.openlmis.db.categories.UnitTests;
import org.openlmis.stockmanagement.domain.*;
import org.openlmis.stockmanagement.dto.StockEvent;
import org.openlmis.stockmanagement.dto.StockEventType;
import org.openlmis.stockmanagement.repository.LotRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.StockCardService;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Category(UnitTests.class)
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(BlockJUnit4ClassRunner.class)
public class StockCardControllerTest {

  @Mock
  private FacilityRepository facilityRepository;

  @Mock
  private ProductService productService;

  @Mock
  private MessageService messageService;

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private StockAdjustmentReasonRepository stockAdjustmentReasonRepository;

  @Mock
  private LotRepository lotRepository;

  @Mock
  private StockCardService service;

  private StockCardController controller;

  private static final long USER_ID = 1L;
  private static final Facility defaultFacility;
  private static final Product defaultProduct;
  private static final StockCard dummyCard;
  private static final MockHttpServletRequest request = new MockHttpServletRequest();
  private static final MockHttpSession session = new MockHttpSession();

  private long fId;
  private long pId;
  private String reasonName;
  private StockAdjustmentReason reason;
  private StockEvent event;
  private long lotId;
  private Lot lot;
  private LotOnHand lotOnHand;

  static  {
    defaultFacility = make(a(FacilityBuilder.defaultFacility, with(FacilityBuilder.facilityId, 1L)));
    defaultProduct = make(a(ProductBuilder.defaultProduct, with(ProductBuilder.productId, 1L)));
    dummyCard = StockCard.createZeroedStockCard(defaultFacility, defaultProduct);
  }

  @Before
  public void setup() {
    request.setSession(session);
    session.setAttribute(UserAuthenticationSuccessHandler.USER_ID, USER_ID);
    controller =  new StockCardController(messageService,
        facilityRepository,
        productService,
        stockAdjustmentReasonRepository,
        stockCardRepository,
        lotRepository,
        service);
  }

  public void setupEvent() {
    fId = defaultFacility.getId();
    pId = defaultProduct.getId();
    reasonName = "dummyReason";

    reason = new StockAdjustmentReason();
    reason.setAdditive(false);
    reason.setName(reasonName);

    event = new StockEvent();
    event.setType(StockEventType.ADJUSTMENT);
//    event.setFacilityId(fId);
    event.setProductId(pId);
    event.setReasonName(reasonName);
    event.setQuantity(10L);
  }

  public void setupLot() {
    lot = new Lot();
    lot.setId(lotId);
    lot.setProduct(defaultProduct);
    lot.setLotCode("A1");
    lot.setManufacturerName("Manu");
    lot.setManufactureDate(new Date());
    lot.setExpirationDate(new Date());
    event.setLot(lot);

    lotOnHand = LotOnHand.createZeroedLotOnHand(lot, dummyCard);
  }

  @Test
  public void shouldSucceedWithEmptyEntriesForAdjustStock() {
    List<StockEvent> events = Collections.emptyList();
    long facilityId = 1;

    ResponseEntity response = controller.processStock(facilityId, events, request);
    assertThat(response.getStatusCode(),
        is(HttpStatus.OK));
  }

  @Test
  public void shouldErrorWithInvalidStockAdjustment() {
    List<StockEvent> events = Collections.singletonList(new StockEvent());
    long facilityId = 1;

    ResponseEntity response = controller.processStock(facilityId, events, request);
    assertThat(response.getStatusCode(),
        is(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void shouldSucceedWithValidAdjustment() {
    setupEvent();

    // test
    when(facilityRepository.getById(fId)).thenReturn(defaultFacility);
    when(productService.getById(pId)).thenReturn(defaultProduct);
    when(stockAdjustmentReasonRepository.getAdjustmentReasonByName(reasonName)).thenReturn(reason);
    when(service.getOrCreateStockCard(fId, pId)).thenReturn(dummyCard);
    when(lotRepository.getLotOnHandByStockCardAndLot(dummyCard.getId(), lotId)).thenReturn(null);
    ResponseEntity response = controller.processStock(fId, Collections.singletonList(event), request);

    // verify
    StockCardEntry entry = new StockCardEntry(dummyCard, StockCardEntryType.ADJUSTMENT, event.getQuantity() * -1);
    entry.setAdjustmentReason(reason);
    verify(service).addStockCardEntries(Collections.singletonList(entry));
    assertThat(response.getStatusCode(), is(HttpStatus.OK));
  }
}

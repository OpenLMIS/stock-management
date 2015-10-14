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
import org.openlmis.core.web.OpenLmisResponse;
import org.openlmis.db.categories.UnitTests;
import org.openlmis.stockmanagement.domain.*;
import org.openlmis.stockmanagement.dto.StockEvent;
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
import static org.junit.Assert.assertEquals;
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
  private String pCode;
  private String reasonName;
  private StockAdjustmentReason reason;
  private StockEvent event;
  private long lotId;
  private Lot lot;
  private LotOnHand lotOnHand;

  static  {
    defaultFacility = make(a(FacilityBuilder.defaultFacility, with(FacilityBuilder.facilityId, 1L)));
    defaultProduct = make(a(ProductBuilder.defaultProduct, with(ProductBuilder.code, "valid_code")));
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
    pCode = defaultProduct.getCode();
    reasonName = "dummyReason";

    reason = new StockAdjustmentReason();
    reason.setAdditive(false);
    reason.setName(reasonName);

    event = new StockEvent();
    event.setFacilityId(fId);
    event.setProductCode(pCode);
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

    ResponseEntity response = controller.adjustStock(facilityId, events, request);
    assertThat(response.getStatusCode(),
        is(HttpStatus.OK));
  }

  @Test
  public void shouldErrorWithInvalidStockAdjustment() {
    List<StockEvent> events = Collections.singletonList(new StockEvent());
    long facilityId = 1;

    ResponseEntity response = controller.adjustStock(facilityId, events, request);
    assertThat(response.getStatusCode(),
        is(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void shouldSucceedWithValidAdjustment() {
    setupEvent();

    // test
    when(facilityRepository.getById(fId)).thenReturn(defaultFacility);
    when(productService.getByCode(pCode)).thenReturn(defaultProduct);
    when(stockAdjustmentReasonRepository.getAdjustmentReasonByName(reasonName)).thenReturn(reason);
    when(service.getOrCreateStockCard(fId, pCode)).thenReturn(dummyCard);
    when(lotRepository.getLotOnHandByStockCardAndLot(dummyCard.getId(), lotId)).thenReturn(null);
    ResponseEntity response = controller.adjustStock(fId, Collections.singletonList(event), request);

    // verify
    StockCardEntry entry = new StockCardEntry(dummyCard, StockCardEntryType.ADJUSTMENT, event.getQuantity() * -1);
    entry.setAdjustmentReason(reason);
    verify(service).addStockCardEntries(Collections.singletonList(entry));
    assertThat(response.getStatusCode(), is(HttpStatus.OK));
  }

  @Test
  public void shouldErrorWithInvalidLotId() {
    setupEvent();
    lotId = 1;
    event.setLotId(lotId);

    // test
    when(facilityRepository.getById(fId)).thenReturn(defaultFacility);
    when(productService.getByCode(pCode)).thenReturn(defaultProduct);
    when(stockAdjustmentReasonRepository.getAdjustmentReasonByName(reasonName)).thenReturn(reason);
    when(service.getOrCreateStockCard(fId, pCode)).thenReturn(dummyCard);
    when(lotRepository.getLotOnHandByStockCardAndLot(dummyCard.getId(), lotId)).thenReturn(null);
    ResponseEntity response = controller.adjustStock(fId, Collections.singletonList(event), request);

    // verify
    assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void shouldSucceedWithValidLotId() {
    lotId = 1;
    setupEvent();
    setupLot();
    event.setLotId(lotId);

    // test
    when(facilityRepository.getById(fId)).thenReturn(defaultFacility);
    when(productService.getByCode(pCode)).thenReturn(defaultProduct);
    when(stockAdjustmentReasonRepository.getAdjustmentReasonByName(reasonName)).thenReturn(reason);
    when(service.getOrCreateStockCard(fId, pCode)).thenReturn(dummyCard);
    when(lotRepository.getLotOnHandByStockCardAndLot(dummyCard.getId(), lotId)).thenReturn(lotOnHand);
    ResponseEntity response = controller.adjustStock(fId, Collections.singletonList(event), request);

    // verify
    StockCardEntry entry = new StockCardEntry(dummyCard, StockCardEntryType.ADJUSTMENT, event.getQuantity() * -1);
    entry.setAdjustmentReason(reason);
    entry.setLotOnHand(lotOnHand);
    verify(service).addStockCardEntries(Collections.singletonList(entry));
    assertThat(response.getStatusCode(), is(HttpStatus.OK));
  }

  @Test
  public void shouldErrorWithStockCardAndLotNotFound() {
    lotId = 1;
    setupEvent();
    event.setLotId(lotId);

    // test
    when(facilityRepository.getById(fId)).thenReturn(defaultFacility);
    when(productService.getByCode(pCode)).thenReturn(defaultProduct);
    when(stockAdjustmentReasonRepository.getAdjustmentReasonByName(reasonName)).thenReturn(reason);
    when(service.getOrCreateStockCard(fId, pCode)).thenReturn(dummyCard);
    when(lotRepository.getLotOnHandByStockCardAndLot(dummyCard.getId(), lotId)).thenReturn(null);
    ResponseEntity response = controller.adjustStock(fId, Collections.singletonList(event), request);

    // verify
    assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void shouldSucceedWithValidLotObject() {
    setupEvent();
    setupLot();

    // test
    when(facilityRepository.getById(fId)).thenReturn(defaultFacility);
    when(productService.getByCode(pCode)).thenReturn(defaultProduct);
    when(stockAdjustmentReasonRepository.getAdjustmentReasonByName(reasonName)).thenReturn(reason);
    when(service.getOrCreateStockCard(fId, pCode)).thenReturn(dummyCard);
    when(service.getOrCreateLotOnHand(lot, dummyCard)).thenReturn(lotOnHand);
    ResponseEntity response = controller.adjustStock(fId, Collections.singletonList(event), request);

    // verify
    StockCardEntry entry = new StockCardEntry(dummyCard, StockCardEntryType.ADJUSTMENT, event.getQuantity() * -1);
    entry.setAdjustmentReason(reason);
    entry.setLotOnHand(lotOnHand);
    verify(service).addStockCardEntries(Collections.singletonList(entry));
    assertThat(response.getStatusCode(), is(HttpStatus.OK));
  }

  @Test
  public void shouldErrorIfProductCodeDoesNotExist() {
    fId = defaultFacility.getId();
    reasonName = "dummyReason";
    reason = new StockAdjustmentReason();
    reason.setAdditive(false);
    reason.setName(reasonName);

    event = new StockEvent();
    event.setFacilityId(fId);
    event.setProductCode("invalid_code");
    event.setReasonName(reasonName);
    event.setQuantity(10L);

    when(facilityRepository.getById(fId)).thenReturn(defaultFacility);
    when(productService.getByCode(pCode)).thenReturn(null);
    when(messageService.message("error.product.unknown")).thenReturn("Unknown product");
    ResponseEntity<OpenLmisResponse> expected_response = OpenLmisResponse.error("Unknown product", HttpStatus.BAD_REQUEST);

    ResponseEntity actual_response = controller.adjustStock(fId, Collections.singletonList(event), request);
    assertThat(actual_response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    assertEquals(expected_response.getBody().getData(), ((ResponseEntity<OpenLmisResponse>) actual_response).getBody().getData());
  }
}

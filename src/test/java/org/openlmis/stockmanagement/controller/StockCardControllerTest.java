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

import java.util.*;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;


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
  private StockCardService stockCardService;

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
            stockCardService);
  }

  public void setupEvent() {
    fId = defaultFacility.getId();
    pCode = defaultProduct.getCode();
    reasonName = "dummyReason";

    reason = new StockAdjustmentReason();
    reason.setAdditive(false);
    reason.setName(reasonName);

    event = new StockEvent();
    event.setProductCode(pCode);
    event.setType(StockEventType.ADJUSTMENT);
//    event.setFacilityId(fId);
    event.setReasonName(reasonName);
    event.setQuantity(10L);
  }

  public Lot setupLot(Long id)
  {
    Lot lot = new Lot();
    lot.setId(id);
    lot.setProduct(defaultProduct);
    lot.setLotCode("code_" + id);
    lot.setManufacturerName("Manufacturer_of_" + id);
    lot.setManufactureDate(new Date());
    lot.setExpirationDate(new Date());
    //event.setLot(lot);
    return lot;
  }

  //Associate two lots with the specified StockCard - one lot for which we have stockOnHand, and one for which we don't
  public void associateTestLotsWithStockCard(StockCard card)
  {
    LotOnHand lotWithZeroQuantityOnHand = LotOnHand.createZeroedLotOnHand(setupLot(0L), dummyCard);

    LotOnHand lotWithPositiveQuantityOnHand = LotOnHand.createZeroedLotOnHand(setupLot(1L), dummyCard);
    lotWithPositiveQuantityOnHand.setQuantityOnHand(1L);

    List<LotOnHand> lotsOnHand = new LinkedList<>();
    lotsOnHand.add(lotWithZeroQuantityOnHand);
    lotsOnHand.add(lotWithPositiveQuantityOnHand);

    card.setLotsOnHand(lotsOnHand);
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
    when(productService.getByCode(pCode)).thenReturn(defaultProduct);
    when(stockAdjustmentReasonRepository.getAdjustmentReasonByName(reasonName)).thenReturn(reason);
    when(stockCardService.getOrCreateStockCard(fId, pCode)).thenReturn(dummyCard);
    when(lotRepository.getLotOnHandByStockCardAndLot(eq(dummyCard.getId()), any(Long.class))).thenReturn(null);
    ResponseEntity response = controller.processStock(fId, Collections.singletonList(event), request);

    // verify
    StockCardEntry entry = new StockCardEntry(dummyCard, StockCardEntryType.ADJUSTMENT, event.getQuantity() * -1, null);
    entry.setAdjustmentReason(reason);
    verify(stockCardService).addStockCardEntries(Collections.singletonList(entry));
    assertThat(response.getStatusCode(), is(HttpStatus.OK));
  }



  @Test
  public void shouldReturnNullWhenLotsOnHandIsNull()
  {
    //Arbitrary values
    Long facilityId = 1L;
    String productCode = "2";
    Integer numEntries = 100;

    when(stockCardRepository.getStockCardByFacilityAndProduct(any(Long.class), any(String.class))).thenReturn(dummyCard);

    boolean includeEmptyLots = false;
    ResponseEntity response = controller.getStockCard(facilityId, productCode, numEntries, includeEmptyLots);
    StockCard stockCard = (StockCard)response.getBody();
    assertNull(stockCard.getLotsOnHand());

    includeEmptyLots = true;
    response = controller.getStockCard(facilityId, productCode, numEntries, includeEmptyLots);
    stockCard = (StockCard)response.getBody();
    assertNull(stockCard.getLotsOnHand());
  }


  @Test
  public void shouldOnlyReturnEmptyLotsWhenRequested()
  {
    //Arbitrary values
    Long facilityId = 1L;
    String productCode = "2";
    Long stockCardId = 3L;
    Integer numEntries = 100;
    Boolean countOnly = false;

    when(stockCardRepository.getStockCardByFacilityAndProduct(any(Long.class), any(String.class))).thenReturn(dummyCard);
    when(stockCardService.getStockCardById(any(Long.class), any(Long.class))).thenReturn(dummyCard);
    when(stockCardService.getStockCards(any(Long.class))).thenReturn(new LinkedList<StockCard>(Arrays.asList(dummyCard)));


    /* Indicate that empty Lots shouldn’t be included in stockCard.getLotsOnHand().
       Then, below, verify that various method calls return just a single LotOnHand
       (having stripped out the second, empty lot, that otherwise would have been included). */
    boolean includeEmptyLots = false;


    associateTestLotsWithStockCard(dummyCard);
    ResponseEntity response = controller.getStockCard(facilityId, productCode, numEntries, includeEmptyLots);
    StockCard stockCard = (StockCard)response.getBody();
    assertEquals( 1, stockCard.getLotsOnHand().size());

    associateTestLotsWithStockCard(dummyCard);
    response = controller.getStockCardById(facilityId, stockCardId, numEntries, includeEmptyLots);
    stockCard = (StockCard)response.getBody();
    assertEquals( 1, stockCard.getLotsOnHand().size());


    associateTestLotsWithStockCard(dummyCard);
    response = controller.getStockCards(facilityId, numEntries, countOnly, includeEmptyLots);
    OpenLmisResponse openLmisResponse = (OpenLmisResponse)response.getBody();
    List<StockCard> stockCards = (List<StockCard>)openLmisResponse.getData().get("stockCards");
    stockCard = stockCards.get(0);
    assertEquals( 1, stockCard.getLotsOnHand().size());


    /* Indicate that empty Lots should be included in stockCard.getLotsOnHand().
       Then, below, verify that various method calls return both lots (including the empty one)
        associated with our stockCard. */
    includeEmptyLots = true;


    associateTestLotsWithStockCard(dummyCard);
    response = controller.getStockCard(facilityId, productCode, numEntries, includeEmptyLots);
    stockCard = (StockCard)response.getBody();
    assertEquals( 2, stockCard.getLotsOnHand().size());

    associateTestLotsWithStockCard(dummyCard);
    response = controller.getStockCardById(facilityId, stockCardId, numEntries, includeEmptyLots);
    stockCard = (StockCard)response.getBody();
    assertEquals( 2, stockCard.getLotsOnHand().size());

    associateTestLotsWithStockCard(dummyCard);
    response = controller.getStockCards(facilityId, numEntries, countOnly, includeEmptyLots);
    openLmisResponse = (OpenLmisResponse)response.getBody();
    stockCards = (List<StockCard>)openLmisResponse.getData().get("stockCards");
    stockCard = stockCards.get(0);
    assertEquals( 2, stockCard.getLotsOnHand().size());
  }
}

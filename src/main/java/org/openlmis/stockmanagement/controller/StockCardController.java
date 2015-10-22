package org.openlmis.stockmanagement.controller;

/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import lombok.NoArgsConstructor;
import org.apache.log4j.Logger;
import org.openlmis.core.domain.StockAdjustmentReason;
import org.openlmis.core.repository.FacilityRepository;
import org.openlmis.core.repository.StockAdjustmentReasonRepository;
import org.openlmis.core.service.MessageService;
import org.openlmis.core.service.ProductService;
import org.openlmis.core.web.OpenLmisResponse;
import org.openlmis.core.web.controller.BaseController;
import org.openlmis.stockmanagement.domain.*;
import org.openlmis.stockmanagement.dto.StockEvent;
import org.openlmis.stockmanagement.dto.StockEventType;
import org.openlmis.stockmanagement.repository.LotRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.StockCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * This controller provides GET, POST, and DELETE endpoints related to stock cards.
 */

@Controller
@Api(value = "Stock Cards", description = "Track the stock cards (stock on hand) at various facilities.")
@RequestMapping(value = "/api/v2/")
@NoArgsConstructor
public class StockCardController extends BaseController
{
    private static Logger logger = Logger.getLogger(StockCardController.class);

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private StockCardRepository stockCardRepository;

    @Autowired
    private StockAdjustmentReasonRepository stockAdjustmentReasonRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private StockCardService service;

    StockCardController(MessageService messageService,
                        FacilityRepository facilityRepository,
                        ProductService productService,
                        StockAdjustmentReasonRepository stockAdjustmentReasonRepository,
                        StockCardRepository stockCardRepository,
                        LotRepository lotRepository,
                        StockCardService service) {
        this.messageService = Objects.requireNonNull(messageService);
        this.facilityRepository = Objects.requireNonNull(facilityRepository);
        this.productService = Objects.requireNonNull(productService);
        this.stockCardRepository = Objects.requireNonNull(stockCardRepository);
        this.stockAdjustmentReasonRepository = Objects.requireNonNull(stockAdjustmentReasonRepository);
        this.lotRepository = Objects.requireNonNull(lotRepository);
        this.service = Objects.requireNonNull(service);
    }

    //TODO: Determine what the permissions associated with @PreAuthorize should be. (MANAGE_PROGRAM_PRODUCT, below, is just a placeholder).

    @RequestMapping(value = "facilities/{facilityId}/products/{productId}/stockCard", method = GET, headers = ACCEPT_JSON)
    @ApiOperation(value = "Get information about the stock card for the specified facility and product.",
            notes = "Gets stock card information, by facility and product. By default, returns most recent entry. " +
                "Entries number can be specified to get more than one.")
    public ResponseEntity getStockCard(@PathVariable Long facilityId, @PathVariable Long productId,
                                       @RequestParam(value = "entries", defaultValue = "1")Integer entries)
    {
        StockCard stockCard = stockCardRepository.getStockCardByFacilityAndProduct(facilityId, productId);

        if (stockCard != null) {
            filterEntries(stockCard, entries);
            return OpenLmisResponse.response(stockCard);
        }
        else {
            return OpenLmisResponse.error("The specified stock card does not exist." , HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "facilities/{facilityId}/stockCards/{stockCardId}", method = GET, headers = ACCEPT_JSON)
    @ApiOperation(value = "Get information about the specified stock card for the specified facility.",
            notes = "Gets stock card information, by facility and stock card id. If facility does not have specified " +
                "stock card id, returns 404 NOT FOUND. By default, returns most recent entry. Entries number can be " +
                "specified to get more than one.")
    public ResponseEntity getStockCardById(@PathVariable Long facilityId, @PathVariable Long stockCardId,
                                           @RequestParam(value = "entries", defaultValue = "1")Integer entries)
    {
        StockCard stockCard = service.getStockCardById(facilityId, stockCardId);

        if (stockCard != null) {
            filterEntries(stockCard, entries);
            return OpenLmisResponse.response(stockCard);
        }
        else {
            return OpenLmisResponse.error("The specified stock card does not exist." , HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "facilities/{facilityId}/stockCards", method = GET, headers = ACCEPT_JSON)
    @ApiOperation(value = "Get information about all stock cards for the specified facility.",
            notes = "Gets all stock card information, by facility. By default, returns most recent entry. Entries " +
                "number can be specified to get more than one. Can also specify countOnly boolean to only return the " +
                "number of stock cards at the facility.")
    public ResponseEntity getStockCards(@PathVariable Long facilityId,
                                        @RequestParam(value = "entries", defaultValue = "1")Integer entries,
                                        @RequestParam(value = "countOnly", defaultValue = "false")Boolean countOnly)
    {
        List<StockCard> stockCards = service.getStockCards(facilityId);

        if (countOnly) {
            return OpenLmisResponse.response("count", stockCards.size());
        }

        if (stockCards != null) {
            for (StockCard stockCard : stockCards) {
                filterEntries(stockCard, entries);
            }
            return OpenLmisResponse.response("stockCards", stockCards);
        }
        else {
            return OpenLmisResponse.error("The specified stock cards do not exist." , HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "facilities/{facilityId}/stockCards", method = POST, headers = ACCEPT_JSON)
    @ApiOperation(value="Update stock cards at a facility.")
    @Transactional
    public ResponseEntity processStock(@PathVariable long facilityId,
                                       @RequestBody(required = true) List<StockEvent> events,
                                       HttpServletRequest request) {

        // verify we have something to do and facility exists
        if(null == events || 0 >= events.size()) return OpenLmisResponse.success("Nothing to do");
        if(null == facilityRepository.getById(facilityId))
            return OpenLmisResponse.error(messageService.message("error.facility.unknown"), HttpStatus.BAD_REQUEST);

        // convert events to entries
        Long userId = loggedInUserId(request);
        List<StockCardEntry> entries = new ArrayList<>();
        for(StockEvent event : events) {
            logger.debug("Processing event: " + event);

            // validate event
            if(!event.isValidAdjustment() &&
                    !event.isValidIssue() &&
                    !event.isValidReceipt())
                return OpenLmisResponse.error("Invalid stock event", HttpStatus.BAD_REQUEST);

            // validate product
            long productId = event.getProductId();
            if(null == productService.getById(productId))
                return OpenLmisResponse.error(messageService.message("error.product.unknown"), HttpStatus.BAD_REQUEST);

            // validate reason
            StockAdjustmentReason reason = null;
            if (StockEventType.ADJUSTMENT == event.getType()) {
                reason = stockAdjustmentReasonRepository.getAdjustmentReasonByName(
                        event.getReasonName());
                if(null == reason)
                    return OpenLmisResponse.error(messageService.message("error.stockadjustmentreason.unknown"),
                            HttpStatus.BAD_REQUEST);
            }

            // get or create stock card
            //TODO:  this call might create a stock card if it doesn't exist, need to implement permission check
            StockCard card = service.getOrCreateStockCard(facilityId, productId);
            if(null == card)
                return OpenLmisResponse.error("Unable to get/create stock card for facility and product",
                    HttpStatus.INTERNAL_SERVER_ERROR);

            // get or create lot, if lot is being used
            StringBuilder str = new StringBuilder();
            Long lotId = event.getLotId();
            Lot lotObj = event.getLot();
            LotOnHand lotOnHand = service.getLotOnHand(lotId, lotObj, productId, card, str);
            if (!str.toString().equals("")) {
                return OpenLmisResponse.error(messageService.message(str.toString()), HttpStatus.BAD_REQUEST);
            }

            // create entry from event
            long quantity = event.getPositiveOrNegativeQuantity(reason);

            StockCardEntryType entryType = StockCardEntryType.ADJUSTMENT;
            switch (event.getType()) {
                case ISSUE: entryType = StockCardEntryType.DEBIT;
                    break;
                case RECEIPT: entryType = StockCardEntryType.CREDIT;
                    break;
                case ADJUSTMENT: entryType = StockCardEntryType.ADJUSTMENT;
                    break;
                default: break;
            }

            StockCardEntry entry = new StockCardEntry(card, entryType, quantity);
            entry.setAdjustmentReason(reason);
            entry.setLotOnHand(lotOnHand);
            Map<String, String> customProps = event.getCustomProps();
            if (null != customProps) {
                for (String k : customProps.keySet()) {
                    entry.addKeyValue(k, customProps.get(k));
                }
            }
            entry.setCreatedBy(userId);
            entry.setModifiedBy(userId);
            entries.add(entry);
        }

        service.addStockCardEntries(entries);
        return OpenLmisResponse.success("Stock adjusted");
    }

    private void filterEntries(StockCard stockCard, Integer entryCount) {
        List<StockCardEntry> entries = stockCard.getEntries();
        if (entries != null) {
            if (entryCount < 0) {
                stockCard.setEntries(entries.subList(0, 1));
            } else if (entryCount < entries.size()) {
                stockCard.setEntries(entries.subList(0, entryCount));
            }
        }
    }
}
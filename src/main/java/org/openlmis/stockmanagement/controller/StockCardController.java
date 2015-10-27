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
import java.util.*;

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
            notes = "Gets stock card information, by facility and product." +
                    "<p>Path parameters (required):" +
                    "<ul>" +
                    "<li><strong>facilityId</strong> (Long) - facility for the stock card.</li>" +
                    "<li><strong>productId</strong> (Long) - product for the stock card.</li>" +
                    "</ul>" +
                    "<p>" +
                    "<p>Request parameters:" +
                    "<ul>" +
                    "<li><strong>entries</strong> (Integer, optional, default = 1) - Number of stock card entries to " +
                    "get in the result.</li>" +
                    "</ul>")
    public ResponseEntity getStockCard(@PathVariable Long facilityId,
                                       @PathVariable Long productId,
                                       @RequestParam(value = "entries", defaultValue = "1")Integer entries,
                                       @RequestParam(value = "includeEmptyLots", required = false, defaultValue = "false") boolean includeEmptyLots)
    {
        StockCard stockCard = stockCardRepository.getStockCardByFacilityAndProduct(facilityId, productId);

        if (stockCard != null)
        {
            filterEntries(stockCard, entries, includeEmptyLots);
            return OpenLmisResponse.response(stockCard);
        }
        else {
            return OpenLmisResponse.error("The specified stock card does not exist." , HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "facilities/{facilityId}/stockCards/{stockCardId}", method = GET, headers = ACCEPT_JSON)
    @ApiOperation(value = "Get information about the specified stock card for the specified facility.",
            notes = "Gets stock card information, by facility and stock card id." +
                    "<p>If facility does not have specified stock card id, returns 404 NOT FOUND." +
                    "<p>Path parameters (required):" +
                    "<ul>" +
                    "<li><strong>facilityId</strong> (Long) - facility for the stock card.</li>" +
                    "<li><strong>stockCardId</strong> (Long) - the specified stock card.</li>" +
                    "</ul>" +
                    "<p>" +
                    "<p>Request parameters:" +
                    "<ul>" +
                    "<li><strong>entries</strong> (Integer, optional, default = 1) - Number of stock card entries to " +
                    "get in the result.</li>" +
                    "</ul>")
    public ResponseEntity getStockCardById(@PathVariable Long facilityId, @PathVariable Long stockCardId,
                                           @RequestParam(value = "entries", defaultValue = "1")Integer entries,
                                           @RequestParam(value = "includeEmptyLots", required = false, defaultValue = "false") boolean includeEmptyLots)
    {
        StockCard stockCard = service.getStockCardById(facilityId, stockCardId);

        if (stockCard != null) {
            filterEntries(stockCard, entries, includeEmptyLots);
            return OpenLmisResponse.response(stockCard);
        }
        else {
            return OpenLmisResponse.error("The specified stock card does not exist." , HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "facilities/{facilityId}/stockCards", method = GET, headers = ACCEPT_JSON)
    @ApiOperation(value = "Get information about all stock cards for the specified facility.",
            notes = "Gets all stock card information, by facility." +
                    "<p>Path parameters (required):" +
                    "<ul>" +
                    "<li><strong>facilityId</strong> (Long) - facility for the stock cards.</li>" +
                    "</ul>" +
                    "<p>" +
                    "<p>Request parameters:" +
                    "<ul>" +
                    "<li><strong>entries</strong> (Integer, optional, default = 1) - Number of stock card entries to " +
                    "get in the result.</li>" +
                    "<li><strong>countOnly</strong> (Boolean, optional, default = false) - Get only the count of " +
                    "stock cards.</li>" +
                    "</ul>")
    public ResponseEntity getStockCards(@PathVariable Long facilityId,
                                        @RequestParam(value = "entries", defaultValue = "1") Integer entries,
                                        @RequestParam(value = "countOnly", defaultValue = "false") Boolean countOnly,
                                        @RequestParam(value = "includeEmptyLots", required = false, defaultValue = "false") boolean includeEmptyLots)
    {
        List<StockCard> stockCards = service.getStockCards(facilityId);

        if (countOnly) {
            return OpenLmisResponse.response("count", stockCards.size());
        }

        if (stockCards != null) {
            for (StockCard stockCard : stockCards) {
                filterEntries(stockCard, entries, includeEmptyLots);
            }
            return OpenLmisResponse.response("stockCards", stockCards);
        }
        else {
            return OpenLmisResponse.error("The specified stock cards do not exist." , HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "facilities/{facilityId}/stockCards", method = POST, headers = ACCEPT_JSON)
    @ApiOperation(value="Update stock cards at a facility.",
            notes = "Updates stock cards at a facility. This is done by providing a list of stock events." +
                    "<p>Path parameters (required):" +
                    "<ul>" +
                    "<li><strong>facilityId</strong> (Long) - facility for the stock cards in which to update.</li>" +
                    "</ul>" +
                    "<p>" +
                    "<p>Body parameters (required):" +
                    "<ul>" +
                    "<li>" +
                    "<strong>stock events</strong> (Array of stock event objects) - a list of stock events to " +
                    "process for update." +
                    "<p>" +
                    "<p>Stock event properties" +
                    "<ul>" +
                    "<li><strong>type</strong> (String, required) - type code of stock event (choices are ISSUE, " +
                    "RECEIPT, ADJUSTMENT).</li>" +
                    "<li><strong>facilityId</strong> (Long, required for ISSUE/RECEIPT types) - facility id where" +
                    "stock is going to/coming from.</li>" +
                    "<li><strong>productId</strong> (Long, required) - product id of the stock being processed.</li>" +
                    "<li><strong>quantity</strong> (Long, required) - quantity of stock being processed. Specify as a " +
                    "positive number. For ISSUE, this amount is decremented, for RECEIPT, this amount is incremented, " +
                    "for ADJUSTMENT, it depends on the adjustment reason.</li>" +
                    "<li><strong>reasonName</strong> (String, required for ADJUSTMENT types) - reason code for the " +
                    "adjustment.</li>" +
                    "<li><strong>lotId</strong> (Long, optional) - lot id of a particular lot that will be processed. " +
                    "This and lot are optional; if lotId is specified, lot is ignored.</li>" +
                    "<li><strong>lot</strong> (Object, optional) - lot object of a particular lot that will be " +
                    "processed. If lot with the specified code, manufacturerName, and expirationDate do not exist, a " +
                    "lot will be created.</li>" +
                    "<li><strong>customProps</strong> (Object, optional) - an object of custom properties (keys and " +
                    "values) to specify custom fields for the stock event." +
                    "</ul>" +
                    "</li>" +
                    "</ul>" +
                    "<p>" +
                    "<p>Example stock event list JSON:" +
                    "<pre><code>" +
                    "[\n" +
                    "    {\n" +
                    "        \"type\": \"ADJUSTMENT\",\n" +
                    "        \"productId\": 2412,\n" +
                    "        \"quantity\": 50,\n" +
                    "        \"reasonName\": \"TRANSFER_IN\",\n" +
                    "        \"lotId\": 1\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"type\": \"ISSUE\",\n" +
                    "        \"facilityId\": 19077,\n" +
                    "        \"productId\": 2412,\n" +
                    "        \"quantity\": 50,\n" +
                    "        \"customProps\": {\n" +
                    "            \"occurred\": \"2015-10-01\"\n" +
                    "        }\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"type\": \"RECEIPT\",\n" +
                    "        \"facilityId\": 19074,\n" +
                    "        \"productId\": 2412,\n" +
                    "        \"quantity\": 50,\n" +
                    "        \"lot\": {\n" +
                    "            \"lotCode\": \"C1\",\n" +
                    "            \"manufacturerName\": \"Manufacturer 3\",\n" +
                    "            \"expirationDate\": \"2016-07-01\"\n" +
                    "        },\n" +
                    "        \"customProps\": {\n" +
                    "            \"vvmStatus\": \"1\"\n" +
                    "        }\n" +
                    "    }\n" +
                    "]\n" +
                    "</code></pre>")
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



    //Calls filterEntries() for each specified stockCard
    private void filterEntries(List<StockCard> stockCards, Integer entryCount, boolean includeEmptyLots)
    {
        for (StockCard stockCard : stockCards) {
            filterEntries(stockCard, entryCount, includeEmptyLots);
        }
    }

    //Convenience method, calling truncateStockCardEntries() and removeEmptyLotsFromStockCard()
    private void filterEntries(StockCard stockCard, Integer entryCount, boolean includeEmptyLots)
    {
        truncateStockCardEntries(stockCard, entryCount);
        if(!includeEmptyLots)
            removeEmptyLotsFromStockCard(stockCard);
    }

    //Filter stockCard.entries such that it contains only the first entryCount number of items
    private void truncateStockCardEntries(StockCard stockCard, Integer entryCount) {
        List<StockCardEntry> entries = stockCard.getEntries();
        if (entries != null) {
            if (entryCount < 0) {
                stockCard.setEntries(entries.subList(0, 1));
            } else if (entryCount < entries.size()) {
                stockCard.setEntries(entries.subList(0, entryCount));
            }
        }
    }


    //Filter stockCard such that only contains Lots that have a positive quantityOnHand
    private void removeEmptyLotsFromStockCard(StockCard stockCard)
    {
        //Build a list of nonEmptyLots...
        List<LotOnHand> nonEmptyLots = new LinkedList<LotOnHand>();
        for (LotOnHand lot : stockCard.getLotsOnHand())
        {
            if(lot.getQuantityOnHand() > 0)
                nonEmptyLots.add(lot);
        }

        //...and associate it with our StockCard
        stockCard.setLotsOnHand(nonEmptyLots);
    }


}
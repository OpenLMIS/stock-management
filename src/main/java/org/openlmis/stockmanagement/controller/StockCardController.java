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

import org.openlmis.stockmanagement.domain.StockCard;
import org.openlmis.stockmanagement.domain.StockCardEntry;
import org.openlmis.stockmanagement.service.StockCardService;
import org.openlmis.core.web.OpenLmisResponse;
import org.openlmis.core.web.controller.BaseController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * This controller provides GET, POST, and DELETE endpoints related to stock cards.
 */

@Controller
@Api(value = "Stock Cards", description = "Track the stock cards (stock on hand) at various facilities.")
@RequestMapping(value = "/api/v2/")
public class StockCardController extends BaseController
{
    @Autowired
    private StockCardService service;

    //TODO: Determine what the permissions associated with @PreAuthorize should be. (MANAGE_PROGRAM_PRODUCT, below, is just a placeholder).

    @RequestMapping(value = "facilities/{facilityId}/products/{productId}/stockCard", method = GET, headers = ACCEPT_JSON)
    @ApiOperation(value = "Get information about the stock card for the specified facility and product.",
            notes = "Gets stock card information, by facility and product. By default, returns most recent entry. " +
                "Entries number can be specified to get more than one.")
    public ResponseEntity getStockCard(@PathVariable Long facilityId, @PathVariable Long productId,
                                       @RequestParam(value = "entries", defaultValue = "1")Integer entries)
    {
        StockCard stockCard = service.getStockCard(facilityId, productId);
        return getResponse(stockCard, entries);
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
        return getResponse(stockCard, entries);
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
        return getResponse(stockCards, entries, countOnly);
    }

    @RequestMapping(value = "facilities/{facilityId}/programs/{programId}/stockCards", method = GET, headers = ACCEPT_JSON)
    @ApiOperation(value = "Get information about all stock cards for the specified program at the specified facility.",
            notes = "By default, returns only the most recent entry on each stock card. An entry number " +
                    "may be specified to retrieve more than one. Additionally, users may specify countOnly boolean to only return the " +
                    "number of stock cards at the facility.")
    public ResponseEntity getStockCards(@PathVariable Long facilityId,
                                        @PathVariable Long programId,
                                        @RequestParam(value = "entries", defaultValue = "1")Integer entries,
                                        @RequestParam(value = "countOnly", defaultValue = "false")Boolean countOnly)
    {

        List<StockCard> stockCards = service.getStockCards(facilityId, programId);
        return getResponse(stockCards, entries, countOnly);
    }

    private ResponseEntity getResponse(List<StockCard> stockCards, Integer entries, Boolean countOnly)
    {
        if (stockCards == null)
            return getNotFoundResponse();

        else if (countOnly) {
            return OpenLmisResponse.response("count", stockCards.size());
        }

        else {
            for (StockCard stockCard : stockCards) {
                filterEntries(stockCard, entries);
            }
            return OpenLmisResponse.response("stockCards", stockCards);
        }
    }

    private ResponseEntity getResponse(StockCard stockCard, Integer entries)
    {
        if (stockCard == null)
        {
            return getNotFoundResponse();
        }

        else {
            filterEntries(stockCard, entries);
            return OpenLmisResponse.response(stockCard);
        }
    }

    private ResponseEntity getNotFoundResponse()
    {
        return OpenLmisResponse.error("The specified stock card(s) do not exist." , HttpStatus.NOT_FOUND);
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
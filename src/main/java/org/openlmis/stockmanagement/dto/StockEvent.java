/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.stockmanagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.openlmis.core.serializer.DateTimeDeserializer;
import org.openlmis.stockmanagement.domain.Lot;

@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown=true)
public class StockEvent {

  private StockEventType type;
  private Long facilityId;
  private Long productId;

  @JsonDeserialize(using= DateTimeDeserializer.class)
  private DateTime occurred;
  private Long quantity;
  private Long lotId;
  private Lot lot;
  private String reasonName;

  private Integer vvmStatus;

  public StockEvent() {
    facilityId = null;
    productId = null;
    occurred = LocalDateTime.now().toDateTime();
    quantity = null;
    lotId = null;
    reasonName = null;
  }

  public long getQuantity() {return Math.abs(quantity);}

  public boolean isValid() {
    if(null == productId
      || null == quantity)
      return false;

    return true;
  }

  public boolean isValidAdjustment() {
    return isValid() &&
            StockEventType.ADJUSTMENT == type &&
            !StringUtils.isBlank(reasonName);
  }

  public boolean isValidIssue() {
    // Need to know what facility it is going to
    return isValid() &&
            StockEventType.ISSUE == type &&
            null != facilityId;
  }

  public boolean isValidReceipt() {
    // Need to know what facility it is coming from
    return isValid() &&
            StockEventType.RECEIPT == type &&
            null != facilityId;
  }

  public boolean hasLot() {
    //TODO
    return true;
  }
}

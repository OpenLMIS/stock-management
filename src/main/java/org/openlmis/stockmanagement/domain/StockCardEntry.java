package org.openlmis.stockmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openlmis.core.domain.BaseModel;
import org.openlmis.stockmanagement.domain.StockCardEntryType;

@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown=true)
public class StockCardEntry extends BaseModel {

  StockCardEntryType type;

  Long quantity;

  String referenceNumber;

  String adjustmentReason;

  String notes;
}
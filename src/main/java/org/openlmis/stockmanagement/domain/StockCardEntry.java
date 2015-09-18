package org.openlmis.stockmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openlmis.core.domain.BaseModel;
import org.openlmis.core.domain.StockAdjustmentReason;

import java.util.Objects;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown=true)
public class StockCardEntry extends BaseModel {

  @JsonIgnore
  private StockCard stockCard;

  private StockCardEntryType type;

  private Long quantity;

  String referenceNumber;

  private StockAdjustmentReason adjustmentReason;

  String notes;

  public StockCardEntry(StockCard card, StockCardEntryType type, long quantity) {
    this.stockCard = Objects.requireNonNull(card);
    this.type = Objects.requireNonNull(type);
    this.quantity = Objects.requireNonNull(quantity);
  }

  public final boolean isValid() {
    if(null == type) return false;
    if(null == quantity) return false;

    return true;
  }

  public final boolean isValidAdjustment() {
    if(false == isValid()) return false;
    if(StockCardEntryType.ADJUSTMENT == type && null == adjustmentReason) return false;

    return true;
  }
}
package org.openlmis.stockmanagement.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openlmis.core.domain.BaseModel;
import org.openlmis.core.serializer.DateDeserializer;

import java.util.Date;
import java.util.Objects;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown=true)
public class LotOnHand extends BaseModel {

  @JsonIgnore
  private StockCard stockCard;

  private Lot lot;

  private Long quantityOnHand;

  @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
  @JsonDeserialize(using=DateDeserializer.class)
  private Date effectiveDate;

  private LotOnHand(Lot lot, StockCard stockCard) {
    Objects.requireNonNull(lot);
    Objects.requireNonNull(stockCard);
    this.lot = lot;
    this.stockCard = stockCard;
    this.quantityOnHand = 0L;
    this.effectiveDate = new Date();
  }

  public void addToQuantityOnHand(long quantity) {
    this.quantityOnHand += quantity;
  }

  /**
   * This method creates a zeroed lot on hand. If lot or stockCard are null, it will throw an exception, rather than
   * returning null.
   * @param lot
   * @param stockCard
   * @return
   */
  public static final LotOnHand createZeroedLotOnHand(Lot lot, StockCard stockCard) {
    return new LotOnHand(lot, stockCard);
  }
}

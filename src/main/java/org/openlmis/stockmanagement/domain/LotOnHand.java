package org.openlmis.stockmanagement.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.list.SetUniqueList;
import org.openlmis.core.domain.BaseModel;
import org.openlmis.core.serializer.DateDeserializer;
import org.openlmis.stockmanagement.util.LatestRecordedStrategy;
import org.openlmis.stockmanagement.util.StockCardEntryKVReduceStrategy;

import java.util.*;

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

  @JsonIgnore
  private List<StockCardEntryKV> keyValues;

  @JsonIgnore
  private StockCardEntryKVReduceStrategy strategy;

  private LotOnHand(Lot lot, StockCard stockCard, StockCardEntryKVReduceStrategy strategy) {
    Objects.requireNonNull(lot);
    Objects.requireNonNull(stockCard);
    this.lot = lot;
    this.stockCard = stockCard;
    this.quantityOnHand = 0L;
    this.effectiveDate = new Date();
    this.keyValues = new ArrayList<>();
    this.strategy = strategy;
  }

  public Map<String, String> getCustomProps() {
    Map<String, String> customProps = new HashMap<>();
    if (null == strategy) strategy = new LatestRecordedStrategy();

    // Get just the keys in the key-value list
    Collection keys = CollectionUtils.collect(keyValues, new Transformer() {
      @Override
      public Object transform(Object o) {
        return ((StockCardEntryKV)o).getKeyColumn();
      }
    });

    // Get only the unique keys
    SetUniqueList.decorate((List)keys);

    // Iterate through the keys, getting the sub-list matching the key. Then implement the strategy on the sub-list.
    // Put the resulting key-value entry into the map.
    for (final Object item : keys) {
      List<StockCardEntryKV> subList = (List<StockCardEntryKV>)CollectionUtils.select(keyValues, new Predicate() {
        @Override
        public boolean evaluate(Object o) {
          return ((StockCardEntryKV)o).getKeyColumn().equalsIgnoreCase((String)item);
        }
      });
      StockCardEntryKV entry = strategy.reduce(subList);
      customProps.put(entry.getKeyColumn(), entry.getValueColumn());
    }

    return customProps.isEmpty() ? null : customProps;
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
  public static final LotOnHand createZeroedLotOnHand(Lot lot, StockCard stockCard, StockCardEntryKVReduceStrategy strategy) {
    return new LotOnHand(lot, stockCard, strategy);
  }
}

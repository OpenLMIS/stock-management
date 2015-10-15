package org.openlmis.stockmanagement.builder;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import org.joda.time.DateTime;
import org.openlmis.stockmanagement.dto.StockEvent;

import static com.natpryce.makeiteasy.Property.newProperty;

public class StockEventBuilder {

  public static final Long DEFAULT_FACILITY_ID = 123L;
  public static final String DEFAULT_PRODUCT_CODE = "P999";

  private static final DateTime DEFAULT_OCCURRED = new DateTime();
  private static final Long DEFAULT_QUANTITY = 100L;
  private static final String DEFAULT_REASON_NAME = "some reason";


  public static final Property<StockEvent, Long> facilityId = newProperty();
  public static final Property<StockEvent, String> productCode = newProperty();
  public static final Property<StockEvent, Long> quantity = newProperty();
  public static final Property<StockEvent, String> reasonName = newProperty();
  public static final Property<StockEvent, DateTime> occurred = newProperty();

  public static final Instantiator<StockEvent> defaultStockEvent = new Instantiator<StockEvent>() {
    @Override
    public StockEvent instantiate(PropertyLookup<StockEvent> lookup) {
      StockEvent stockEvent = new StockEvent();
      stockEvent.setFacilityId(lookup.valueOf(facilityId, DEFAULT_FACILITY_ID));
      stockEvent.setProductCode(lookup.valueOf(productCode, DEFAULT_PRODUCT_CODE));
      stockEvent.setQuantity(lookup.valueOf(quantity, DEFAULT_QUANTITY));
      stockEvent.setReasonName(lookup.valueOf(reasonName, DEFAULT_REASON_NAME));
      stockEvent.setOccurred(lookup.valueOf(occurred, DEFAULT_OCCURRED));
      return stockEvent;
    }
  };
}

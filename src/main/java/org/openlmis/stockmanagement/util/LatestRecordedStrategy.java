package org.openlmis.stockmanagement.util;

import org.openlmis.stockmanagement.domain.StockCardEntryKV;

import java.util.Date;
import java.util.List;

public class LatestRecordedStrategy implements StockCardEntryKVReduceStrategy {

    @Override
    public StockCardEntryKV reduce(List<StockCardEntryKV> list) {
        if (null == list || list.isEmpty()) return null;
        StockCardEntryKV max = new StockCardEntryKV("","",new Date(0));
        for (StockCardEntryKV item : list) {
            if (item.getRecordedDate().after(max.getRecordedDate())) {
                max = item;
            }
        }
        return max;
    }
}

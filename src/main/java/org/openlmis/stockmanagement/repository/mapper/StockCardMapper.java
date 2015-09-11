package org.openlmis.stockmanagement.repository.mapper;

import org.apache.ibatis.annotations.*;
import org.openlmis.core.domain.Facility;
import org.openlmis.core.domain.Product;
import org.openlmis.stockmanagement.domain.Lot;
import org.openlmis.stockmanagement.domain.LotOnHand;
import org.openlmis.stockmanagement.domain.StockCard;
import org.openlmis.stockmanagement.domain.StockCardEntry;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockCardMapper {

  @Select("SELECT *" +
      " FROM stock_cards" +
      " WHERE facilityid = #{facilityId}" +
      "   AND productid = #{productId}")
  @Results({
      @Result(property = "id", column = "id"),
      @Result(property = "facility", column = "facilityId", javaType = Facility.class,
          one = @One(select = "org.openlmis.core.repository.mapper.FacilityMapper.getById")),
      @Result(property = "product", column = "productId", javaType = Product.class,
          one = @One(select = "org.openlmis.core.repository.mapper.ProductMapper.getById")),
      @Result(property = "entries", column = "id", javaType = List.class,
          many = @Many(select = "getEntries")),
      @Result(property = "lotsOnHand", column = "id", javaType = List.class,
      many = @Many(select = "getLotsOnHand"))
  })
  StockCard getByFacilityAndProduct(@Param("facilityId")Long facilityId, @Param("productId")Long productId);

  @Select("SELECT *" +
      " FROM stock_cards" +
      " WHERE facilityid = #{facilityId}" +
      "   AND id = #{id}")
  @Results({
      @Result(property = "id", column = "id"),
      @Result(property = "facility", column = "facilityId", javaType = Facility.class,
          one = @One(select = "org.openlmis.core.repository.mapper.FacilityMapper.getById")),
      @Result(property = "product", column = "productId", javaType = Product.class,
          one = @One(select = "org.openlmis.core.repository.mapper.ProductMapper.getById")),
      @Result(property = "entries", column = "id", javaType = List.class,
          many = @Many(select = "getEntries")),
      @Result(property = "lotsOnHand", column = "id", javaType = List.class,
          many = @Many(select = "getLotsOnHand"))
  })
  StockCard getByFacilityAndId(@Param("facilityId")Long facilityId, @Param("id")Long id);

  @Select("SELECT *" +
      " FROM stock_cards" +
      " WHERE facilityid = #{facilityId}")
  @Results({
      @Result(property = "id", column = "id"),
      @Result(property = "product", column = "productId", javaType = Product.class,
          one = @One(select = "org.openlmis.core.repository.mapper.ProductMapper.getById")),
      @Result(property = "entries", column = "id", javaType = List.class,
          many = @Many(select = "getEntries")),
      @Result(property = "lotsOnHand", column = "id", javaType = List.class,
          many = @Many(select = "getLotsOnHand"))
  })
  List<StockCard> getAllByFacility(@Param("facilityId")Long facilityId);


    @Select("SELECT *" +
            " FROM vw_stock_cards" +
            " WHERE facilityid = #{facilityId}" +
            " AND programid = #{programId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "product", column = "productId", javaType = Product.class,
                    one = @One(select = "org.openlmis.core.repository.mapper.ProductMapper.getById")),
            @Result(property = "entries", column = "id", javaType = List.class,
                    many = @Many(select = "getEntries")),
            @Result(property = "lotsOnHand", column = "id", javaType = List.class,
                    many = @Many(select = "getLotsOnHand"))
    })
  List<StockCard> getAllByFacilityAndProgram(@Param("facilityId")Long facilityId, @Param("programId")Long programId);


  @Select("SELECT *" +
      " FROM stock_card_entries" +
      " WHERE stockcardid = #{stockCardId}" +
      " ORDER BY createddate DESC")
  List<StockCardEntry> getEntries(@Param("stockCardId")Long stockCardId);

  @Select("SELECT *" +
      " FROM lots_on_hand" +
      " WHERE stockcardid = #{stockCardId}")
  @Results({
      @Result(property = "lot", column = "lotId", javaType = Lot.class,
          one = @One(select = "org.openlmis.stockmanagement.repository.mapper.LotMapper.getById"))
  })
  List<LotOnHand> getLotsOnHand(@Param("stockCardId")Long stockCardId);
}

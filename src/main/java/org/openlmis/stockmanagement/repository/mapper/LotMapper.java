package org.openlmis.stockmanagement.repository.mapper;

import org.apache.ibatis.annotations.*;
import org.openlmis.core.domain.Product;
import org.openlmis.stockmanagement.domain.Lot;
import org.openlmis.stockmanagement.domain.LotOnHand;
import org.springframework.stereotype.Repository;

@Repository
public interface LotMapper {

  @Select("SELECT *" +
      " FROM lots" +
      " WHERE id = #{id}")
  @Results({
      @Result(
          property = "product", column = "productId", javaType = Product.class,
          one = @One(select = "org.openlmis.core.repository.mapper.ProductMapper.getById")),
      @Result(property = "lotCode", column = "lotnumber"),
  })
  Lot getById(@Param("id")Long id);

  @Select("SELECT *" +
      " FROM lots_on_hand" +
      " WHERE lotid = #{lotId}")
  @Results({
      @Result(
          property = "lot", column = "lotId", javaType = Lot.class,
          one = @One(select = "org.openlmis.stockmanagement.repository.mapper.LotMapper.getById"))
  })
  LotOnHand getLotOnHandByLot(@Param("lotId")Long lotId);

  @Select("SELECT *" +
      " FROM lots_on_hand loh" +
      "   JOIN lots l ON l.id = loh.lotid" +
      " WHERE l.lotnumber = #{lotCode}" +
      "   AND l.manufacturername = #{manufacturerName}" +
      "   AND l.expirationdate = #{expirationDate}")
  @Results({
      @Result(
          property = "lot", column = "lotId", javaType = Lot.class,
          one = @One(select = "org.openlmis.stockmanagement.repository.mapper.LotMapper.getById"))
  })
  LotOnHand getLotOnHandByLot(Lot lot);

  @Insert("INSERT into lots " +
      " (productId, lotNumber, manufacturerName, manufactureDate, expirationDate" +
      ", createdBy, createdDate, modifiedBy, modifiedDate) " +
      "values " +
      " (#{productId}, #{lotNumber}, #{manufacturerName}, #{manufactureDate}, #{expirationDate}" +
      ", #{createdBy}, NOW(), #{modifiedBy}, NOW())")
  @Options(useGeneratedKeys = true)
  void insert(Lot lot);

  @Insert("INSERT into lots_on_hand " +
      " (stockCardId, lotId, quantityOnHand, effectiveDate" +
      ", createdBy, createdDate, modifiedBy, modifiedDate) " +
      "values " +
      " (#{stockCardId}, #{lotId}, #{quantityOnHand}, #{effectiveDate}" +
      ", #{createdBy}, NOW(), #{modifiedBy}, NOW())")
  @Options(useGeneratedKeys = true)
  void insertLotOnHand(LotOnHand lotOnHand);

  @Update("UPDATE lots_on_hand " +
      "SET quantityOnHand = #{quantityOnHand}" +
      "WHERE id = #{id}")
  int updateLotOnHand(LotOnHand lotOnHand);
}

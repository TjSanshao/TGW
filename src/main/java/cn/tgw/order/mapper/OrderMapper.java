package cn.tgw.order.mapper;

import cn.tgw.order.model.Order;
import cn.tgw.order.model.OrderQueryModel;
import cn.tgw.user.model.User;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface OrderMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Order record);

    int insertSelective(Order record);

    Order selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);

    //自定义查询开始

    List<Order> selectAllOrders();

    List<Order> selectAllOrdersByUserId(Integer userId);

    List<Order> selectAllOrdersByBusinessmanId(Integer businessmanId);

    List<Order> selectAllOrdersByUserIdAndSellStatusAndStatus(Order order);

    List<Order> selectOrdersStatusNormalByUserId(Integer userId);

    Order selectByUniqueOrderNumber(String orderNumber);

    List<Order> selectOrderInDaysByBusinessmanId(OrderQueryModel query);

    BigDecimal selectTotalInDaysByBusinessmanId(OrderQueryModel query);

    //自定义查询结束



    List<Order> IsRepeatKill(@Param("tgw_user_id") Integer tgw_user_id, @Param("tgw_goods_id") Integer tgw_goods_id,
                       @Param("seckillCreattime") Date seckillCreattime, @Param("seckillEndttime") Date seckillEndttime);


}
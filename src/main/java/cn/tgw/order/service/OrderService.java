package cn.tgw.order.service;

import cn.tgw.admin.model.TgwSeckill;
import cn.tgw.businessman.model.Businessman;
import cn.tgw.goods.model.Goods;
import cn.tgw.order.model.Order;
import cn.tgw.user.model.User;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface OrderService {

    /*
     * @Description:获取所有的订单信息，作测试使用
     * @Param:[]
     * @Return:java.util.List<cn.tgw.order.model.Order>
     * @Author:TjSanshao
     * @Date:2018-12-05
     * @Time:15:38
     **/
    List<Order> getAllOrders();

    /*
     * @Description:根据用户获取该用户所有订单，用户不可使用，因为该方法会查询到逻辑删除的订单
     * @Param:[user]
     * @Return:java.util.List<cn.tgw.order.model.Order>
     * @Author:TjSanshao
     * @Date:2018-12-05
     * @Time:16:12
     **/
    List<Order> getAllOrdersByUser(User user);

    /*
     * @Description:根据用户id获取该用户所有订单，用户不可使用，因为该方法会查询到逻辑删除的订单
     * @Param:[userId]
     * @Return:java.util.List<cn.tgw.order.model.Order>
     * @Author:TjSanshao
     * @Date:2018-12-05
     * @Time:16:12
     **/
    List<Order> getAllOrdersByUserId(int userId);

    /*
     * @Description:根据商家查询所有该商家的订单
     * @Param:[businessman]
     * @Return:java.util.List<cn.tgw.order.model.Order>
     * @Author:TjSanshao
     * @Date:2018-12-05
     * @Time:16:53
     **/
    List<Order> getAllOrdersByBusinessman(Businessman businessman);

    /*
     * @Description:根据商家id查询所有该商家的订单
     * @Param:[businessmanId]
     * @Return:java.util.List<cn.tgw.order.model.Order>
     * @Author:TjSanshao
     * @Date:2018-12-05
     * @Time:16:53
     **/
    List<Order> getAllOrdersByBusinessmanId(int businessmanId);

    /*
     * @Description:根据用户以及商品和数量创建订单
     * @Param:[user, goods, count]
     * @Return:cn.tgw.order.model.Order
     * @Author:TjSanshao
     * @Date:2018-12-05
     * @Time:16:29
     **/
    Order createOrderWithUserAndGoods(User user, Goods goods, int count);

    /*
     * @Description:根据用户id、商品id、数量创建订单
     * @Param:[userId, goodsId, count]
     * @Return:cn.tgw.order.model.Order
     * @Author:TjSanshao
     * @Date:2018-12-05
     * @Time:16:30
     **/
    Order createOrderWithUserIdAndGoodsId(int userId, int goodsId, int count);

    /*
     * @Description:根据用户id、商品、数量创建订单
     * @Param:[userId, goods, count]
     * @Return:cn.tgw.order.model.Order
     * @Author:TjSanshao
     * @Date:2018-12-05
     * @Time:16:33
     **/
    Order createOrderWithUserIdAndGoods(int userId, Goods goods, int count);

    /*
     * @Description:根据用户以及订单状态查询订单，未逻辑删除的
     * @Param:[user, sellStatus]
     * @Return:java.util.List<cn.tgw.order.model.Order>
     * @Author:TjSanshao
     * @Date:2018-12-05
     * @Time:17:07
     **/
    List<Order> getOrdersByUserAndOrderSellStatusAndStatusNormal(User user, Byte sellStatus);

    /*
     * @Description:根据用户id以及订单状态查询订单，未逻辑删除的
     * @Param:[userId, sellStatus]
     * @Return:java.util.List<cn.tgw.order.model.Order>
     * @Author:TjSanshao
     * @Date:2018-12-05
     * @Time:17:18
     **/
    List<Order> getOrdersByUserIdAndOrderSellStatusAndStatusNormal(int userId, Byte sellStatus);

    List<Order> getUserAllOrders(int userId);

    /*
     * @Description:根据用户查询该用户的所有未逻辑删除的订单，用户使用
     * @Param:[user]
     * @Return:java.util.List<cn.tgw.order.model.Order>
     * @Author:TjSanshao
     * @Date:2018-12-11
     * @Time:08:54
     **/
    List<Order> getUserAllOrders(User user);

    /*
     * @Description:RabbitMQ监听消息队列
     * @Param:[id]
     * @Return:void
     * @Author:TjSanshao
     * @Date:2018-12-11
     * @Time:08:36
     **/
    public void orderTimeQueueListener(String id);

    /*
     * @Description:根据订单的ID逻辑删除订单，用于处理用户删除订单的请求
     * @Param:[id]
     * @Return:boolean
     * @Author:TjSanshao
     * @Date:2018-12-11
     * @Time:08:37
     **/
    public boolean deleteByOrderId(int id);

    /*
     * @Description:根据id获取可以支付的订单，有则返回order，无则返回null
     * @Param:[id]
     * @Return:cn.tgw.order.model.Order
     * @Author:TjSanshao
     * @Date:2018-12-11
     * @Time:09:49
     **/
    public Order getOrderByIdPayAble(int id);

    public Order getOrderById(int id);

    public Order createmsKillOrderByUserAndmsKill(User user, TgwSeckill msKill, int count);

    public Order createmsKillOrderByUserIdAndmsKillId(int userId, int msKillId, int count);

    /**
    * @Description:    根据tgw_user_id  tgw_goods_id , Date seckillCreattime,Date seckillEndttime
     *                 判断用户是否重复秒杀
    * @Author:         梁智发
    * @CreateDate:     2018/12/12 0012 9:30
    * @UpdateUser:     梁智发
    * @UpdateDate:     2018/12/12 0012 9:30
    * @UpdateRemark:   修改内容
    * @Version:        1.0
    */
    public List<Order>  IsRepeatKill(Integer tgw_user_id,Integer tgw_goods_id, Date seckillCreattime,Date seckillEndttime);

    public Object getOrdersByBusinessmanIdWithPage(int page, int pageSize, int businessmanId);

    public Order getOrderByUniqueOrderNumber(String orderNumber);

    //更改订单支付状态方法开始

    public boolean orderPaySuccess(Order order);
    public boolean orderPaySuccess(int orderId);

    public boolean orderUseFinish(Order order);
    public boolean orderUseFinish(int orderId);

    public boolean orderCommentFinish(Order order);
    public boolean orderCommentFinish(int orderId);

    //更改订单支付状态方法结束

    public Object getOrdersByBusinessmanIdInDaysWithPage(int page, int pageSize, int businessmanId, Date start, Date end);
    public BigDecimal getOrdersTotalMoneyByBusinessmanIdInDays(int businessmanId, Date start, Date end);

}

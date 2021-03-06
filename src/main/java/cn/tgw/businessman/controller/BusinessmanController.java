package cn.tgw.businessman.controller;

import cn.tgw.businessman.model.Businessman;
import cn.tgw.businessman.model.BusinessmanDetail;
import cn.tgw.businessman.service.BusinessmanService;
import cn.tgw.common.service.MiaoDiService;
import cn.tgw.common.service.SmsVerifyService;
import cn.tgw.common.utils.DateUtil;
import cn.tgw.common.utils.TGWStaticString;
import cn.tgw.goods.model.Goods;
import cn.tgw.goods.service.GoodsService;
import cn.tgw.order.model.Order;
import cn.tgw.order.service.OrderService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * @Project:tgw
 * @Description:businessman controller
 * @Author:TjSanshao
 * @Create:2018-12-03 10:11
 *
 **/

@RestController
public class BusinessmanController {

    @Autowired
    private BusinessmanService businessmanService;

    @Autowired
    private SmsVerifyService smsVerifyService;

    @Autowired
    private MiaoDiService miaoDiService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private GoodsService goodsService;

    /*
     * @Description:商家用户登录功能
     * @Param:[username, password, session]
     * @Return:java.util.Map<java.lang.String,java.lang.Object>
     * @Author:TjSanshao
     * @Date:2018-12-03
     * @Time:10:48
     **/
    @PostMapping("/tjsanshao/businessman/login")
    public Map<String, Object> login(String username, String password, HttpSession session) {
        HashMap<String, Object> loginStatus = new HashMap<>();

        //校验用户名和密码是否为空
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)){
            loginStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "please input username and password");
            loginStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            return loginStatus;
        }

        //调用service，获取记录
        Businessman businessman = businessmanService.getBusinessmanByUsernameOrMobileAndPasswordAndStatus(username, password, new Byte("1"));
        if (businessman != null) {
            loginStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
            loginStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "login success");
            businessman.setPassword(""); //不返回密码到客户端
            loginStatus.put("businessman", businessman);

            //将记录存入session，键为"businessman"，值为businessman对象
            session.setAttribute(TGWStaticString.TGW_BUSINESSMAN, businessman);

            //判断是否已入驻
            boolean settled = businessmanService.checkSettled(businessman);
            if (settled) {
                session.setAttribute(TGWStaticString.TGW_BUSINESSMAN_SETTLE, "settled");
            }
        }else {
            loginStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            loginStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "username or password error");
        }

        return loginStatus;
    }

    /*
     * @Description:商家用户退出登录
     * @Param:[session]
     * @Return:java.util.Map<java.lang.String,java.lang.Object>
     * @Author:TjSanshao
     * @Date:2018-12-12
     * @Time:15:51
     **/
    @RequestMapping("/tjsanshao/businessman/logout")
    public Map<String, Object> logout(HttpSession session) {
        HashMap<String, Object> logoutStatus = new HashMap<>();

        session.setAttribute(TGWStaticString.TGW_BUSINESSMAN, null);

        logoutStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
        logoutStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "success");

        return logoutStatus;
    }

    /*
     * @Description:商家发送验证码功能
     * @Param:[mobileNumber, requestParam, session]
     * @Return:java.util.Map<java.lang.String,java.lang.Object>
     * @Author:TjSanshao
     * @Date:2018-12-03
     * @Time:16:28
     **/
    @GetMapping("/tjsanshao/businessman/sendMsgCode")
    public Map<String, Object> sendMsgCode(String mobileNumber, String requestParam, HttpSession session) {
        HashMap<String, Object> sendMsgStatus = new HashMap<>();

        if (requestParam != null) {
            //是否是修改密码要求的验证码
            if (requestParam.equals("password")) {
                //查询session，用户是否已经登录
                Object sessionBusinessman = session.getAttribute(TGWStaticString.TGW_BUSINESSMAN);

                //这个请求并没有经过cn.tgw.businessman.filter.BusinessmanAuthenticationFilter过滤器，因此要判断是否登录
                if (sessionBusinessman == null) {
                    //没有登录
                    sendMsgStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_AUTH);
                    sendMsgStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "LOGIN FIRST");
                    return sendMsgStatus;
                }

                //已经登录，从session中获取的businessman对象持有id
                Businessman businessmanFromSession = (Businessman)sessionBusinessman;

                //异步发送验证码，返回响应
                smsVerifyService.sendMsgCodeAsync(businessmanFromSession.getMobile(), miaoDiService.generateCode(6));

                sendMsgStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
                sendMsgStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "success");

                return sendMsgStatus;
            }
        }

        //验证手机号码是否为空
        if (StringUtils.isEmpty(mobileNumber)){
            sendMsgStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            sendMsgStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "please input correct phone number");
            return sendMsgStatus;
        }

        //校验手机号码的正则表达式
        String regex = "^((13[0-9])|(14[5,7,9])|(15([0-3]|[5-9]))|(166)|(17[0,1,3,5,6,7,8])|(18[0-9])|(19[8|9]))\\d{8}$";

        if (mobileNumber.length() != 11) {
            //如果不是11位，不是手机号码，直接返回fail
            sendMsgStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            sendMsgStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "please input correct phone number");
            return sendMsgStatus;
        } else {
            //是11位手机号码，开始用正则匹配
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(mobileNumber);
            boolean isMatch = m.matches();
            if (!isMatch) {
                //如果匹配不成功，不合法，返回fail
                sendMsgStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
                sendMsgStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "please input correct phone number");
                return sendMsgStatus;
            } else {
                //验证手机号是否可以发送验证码
                if (!smsVerifyService.enableSend(mobileNumber)) {
                    //该手机号不可以发送验证码，超出了每天发送次数
                    sendMsgStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
                    sendMsgStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "send times out");
                    return sendMsgStatus;
                }

                //异步发送验证码
                smsVerifyService.sendMsgCodeAsync(mobileNumber, miaoDiService.generateCode(6));

                sendMsgStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
                sendMsgStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "success");

                return sendMsgStatus;
            }
        }

    }

    /*
     * @Description:商家注册
     * @Param:[businessman, code, session]
     * @Return:java.util.Map<java.lang.String,java.lang.Object>
     * @Author:TjSanshao
     * @Date:2018-12-20
     * @Time:10:35
     **/
    @PostMapping("/tjsanshao/businessman/register")
    public Map<String, Object> register(Businessman businessman, String code, HttpSession session) {
        HashMap<String, Object> registerStatus = new HashMap<>();

        //验证用户名、密码、手机号码是否为空
        if (businessman.getUsername() == null || businessman.getPassword() == null || StringUtils.isEmpty(businessman.getMobile()) || StringUtils.isEmpty(code)) {
            registerStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            registerStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "please input the whole");
            return registerStatus;
        }

        //验证验证码是否正确
        if (!smsVerifyService.checkCode(businessman.getMobile(), code)){
            registerStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            registerStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "code is invalid");
            return registerStatus;
        }

        //填写了用户名、密码、验证码，验证是否可以注册
        if (!businessmanService.enableBusinessmanRegister(businessman)){
            //用户名已存在，不可注册
            registerStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            registerStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "username exists");
            return registerStatus;
        }

        //存入到数据库
        businessmanService.businessmanRegister(businessman);

        session.setAttribute(TGWStaticString.TGW_BUSINESSMAN, businessman);

        registerStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
        registerStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "register success");

        businessman.setPassword("");
        registerStatus.put("businessman", businessman);

        return registerStatus;
    }

    /*
     * @Description:商家修改密码
     * @Param:[password, code, oldPassword, session]
     * @Return:java.util.Map<java.lang.String,java.lang.Object>
     * @Author:TjSanshao
     * @Date:2018-12-06
     * @Time:09:20
     **/
    @PostMapping("/tjsanshao/businessman/password")
    public Map<String, Object> password(String password, String code, String oldPassword, HttpSession session) {
        HashMap<String, Object> passwordStatus = new HashMap<>();

        //验证用户登录已使用过滤器，详细请查看cn.tgw.user.filter.UserAuthenticationFilter

        //从session中获取商家对象
        Businessman businessmanFromSession = (Businessman)session.getAttribute(TGWStaticString.TGW_BUSINESSMAN);

        //判断是否带有验证码，如果没有，需要发送验证码
        if (StringUtils.isEmpty(code)){
            //请求中没有验证码
            passwordStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            passwordStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "verify code error");
            return passwordStatus;
        }

        //请求中带有验证码，开始验证验证码
        if (!smsVerifyService.checkCode(businessmanFromSession.getMobile(), code)){
            //验证码不通过
            passwordStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            passwordStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "verify code error");
            return passwordStatus;
        }

        //查询数据库，验证旧密码和已登录商家用户是否匹配
        Businessman queryBusinessman = businessmanService.getBusinessmanByUsernameOrMobileAndPasswordAndStatus(businessmanFromSession.getUsername(), oldPassword, new Byte("1"));

        //如果根据username查询为空，尝试使用mobile匹配
        if (queryBusinessman == null) {
            queryBusinessman = businessmanService.getBusinessmanByUsernameOrMobileAndPasswordAndStatus(businessmanFromSession.getMobile(), oldPassword, new Byte("1"));
            if (queryBusinessman == null) {
                //没有查询到用户，即用户名和旧密码不匹配
                passwordStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
                passwordStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "old password error");
                return passwordStatus;
            }
        }

        //更新数据库
        queryBusinessman.setPassword(password);
        queryBusinessman.setStatus(new Byte("1"));
        businessmanService.updateBusinessmanPassword(queryBusinessman);

        //设置验证码状态为已使用
        smsVerifyService.codeUsed(queryBusinessman.getMobile());

        passwordStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
        passwordStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "success");

        return passwordStatus;
    }

    /*
     * @Description:商家申请入驻
     * @Param:[businessmanDetail, session]
     * @Return:java.util.Map<java.lang.String,java.lang.Object>
     * @Author:TjSanshao
     * @Date:2018-12-10
     * @Time:14:53
     **/
    @PostMapping("/tjsanshao/businessman/settle")
    public Map<String, Object> applyToSettle(BusinessmanDetail businessmanDetail, HttpSession session) {
        HashMap<String, Object> applyToSettleStatus = new HashMap<>();

        //使用过滤器验证登录

        //判断非空字段
        if (
                StringUtils.isEmpty(businessmanDetail.getShopName()) ||
                StringUtils.isEmpty(businessmanDetail.getShopLocation()) ||
                StringUtils.isEmpty(businessmanDetail.getShopDesc()) ||
                businessmanDetail.getShopTimeOpen() != null ||
                businessmanDetail.getShopTimeClose() != null ||
                StringUtils.isEmpty(businessmanDetail.getPhoneNumber()) ||
                StringUtils.isEmpty(businessmanDetail.getContactPhoneNumber())
        ) {
            applyToSettleStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            applyToSettleStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "please input the whole");
        }

        Businessman businessman = (Businessman)session.getAttribute(TGWStaticString.TGW_BUSINESSMAN);

        businessmanDetail.setTgwBusinessmanId(businessman.getId());
        businessmanDetail.setShopSettleStatus(new Byte("0"));

        //将申请存进数据库等待审核
        if (businessmanService.applyToSettle(businessmanDetail)) {
            applyToSettleStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
            applyToSettleStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "success");
        } else {
            applyToSettleStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            applyToSettleStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "Unknown error, please try again later");
        }

        return applyToSettleStatus;
    }

    /*
     * @Description:商家查看订单
     * @Param:[session, page, pageSize]
     * @Return:java.util.Map<java.lang.String,java.lang.Object>
     * @Author:TjSanshao
     * @Date:2018-12-12
     * @Time:09:54
     **/
    @RequestMapping("/tjsanshao/businessman/orders")
    public Map<String, Object> watchOrders(HttpSession session, Integer page, Integer pageSize) {

        //如果没有分页参数，给予默认分页参数
        if (page == null) {
            page = 1;
        }

        if (pageSize == null) {
            pageSize = 10;
        }

        HashMap<String, Object> watchOrdersStatus = new HashMap<>();

        Businessman businessman = (Businessman)session.getAttribute(TGWStaticString.TGW_BUSINESSMAN);

        PageInfo<List<Order>> pageInfo = (PageInfo<List<Order>>) orderService.getOrdersByBusinessmanIdWithPage(page, pageSize, businessman.getId());

        watchOrdersStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
        watchOrdersStatus.put("pageInfo", pageInfo);

        return watchOrdersStatus;
    }

    /*
     * @Description:查询商家7天内订单
     * @Param:[session, page, pageSize]
     * @Return:java.util.Map<java.lang.String,java.lang.Object>
     * @Author:TjSanshao
     * @Date:2018-12-14
     * @Time:08:32
     **/
    @RequestMapping("/tjsanshao/businessman/orders7days")
    public Map<String, Object> watchOrders7days(HttpSession session, Integer page, Integer pageSize) {

        //如果没有分页参数，给予默认分页参数
        if (page == null) {
            page = 1;
        }

        if (pageSize == null) {
            pageSize = 10;
        }

        HashMap<String, Object> watchOrdersStatus = new HashMap<>();

        Businessman businessman = (Businessman)session.getAttribute(TGWStaticString.TGW_BUSINESSMAN);

        //日期范围
        Date start = DateUtil.getDateDealWithDays(-7, true);
        Date end = new Date();

        PageInfo<List<Order>> pageInfo = (PageInfo<List<Order>>) orderService.getOrdersByBusinessmanIdInDaysWithPage(page, pageSize, businessman.getId(), start, end);

        BigDecimal totalMoney = orderService.getOrdersTotalMoneyByBusinessmanIdInDays(businessman.getId(), start, end);

        watchOrdersStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
        watchOrdersStatus.put("pageInfo", pageInfo);
        watchOrdersStatus.put("totalMoney", totalMoney);

        return watchOrdersStatus;

    }

    @RequestMapping("/tjsanshao/businessman/orders30days")
    public Map<String, Object> watchOrders30days(HttpSession session, Integer page, Integer pageSize) {
        //如果没有分页参数，给予默认分页参数
        if (page == null) {
            page = 1;
        }

        if (pageSize == null) {
            pageSize = 10;
        }

        HashMap<String, Object> watchOrdersStatus = new HashMap<>();

        Businessman businessman = (Businessman)session.getAttribute(TGWStaticString.TGW_BUSINESSMAN);

        //日期范围
        Date start = DateUtil.getDateDealWithDays(-30, true);
        Date end = new Date();

        PageInfo<List<Order>> pageInfo = (PageInfo<List<Order>>) orderService.getOrdersByBusinessmanIdInDaysWithPage(page, pageSize, businessman.getId(), start, end);

        BigDecimal totalMoney = orderService.getOrdersTotalMoneyByBusinessmanIdInDays(businessman.getId(), start, end);

        watchOrdersStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
        watchOrdersStatus.put("pageInfo", pageInfo);
        watchOrdersStatus.put("totalMoney", totalMoney);

        return watchOrdersStatus;
    }

    /*
     * @Description:自定义日期查询订单，自定义日期中，只有年月日有效，时间无效
     * @Param:[page, pageSize, start, end, session]
     * @Return:java.util.Map<java.lang.String,java.lang.Object>
     * @Author:TjSanshao
     * @Date:2018-12-14
     * @Time:10:43
     **/
    @RequestMapping("/tjsanshao/businessman/ordersCustom")
    public Map<String, Object> watchOrdersCustomDays(Integer page, Integer pageSize, String start, String end, HttpSession session) throws Exception {
        //如果没有分页参数，给予默认分页参数
        if (page == null) {
            page = 1;
        }

        if (pageSize == null) {
            pageSize = 10;
        }

        HashMap<String, Object> watchOrdersStatus = new HashMap<>();

        Businessman businessman = (Businessman)session.getAttribute(TGWStaticString.TGW_BUSINESSMAN);

        //解析日期
        Date startDate = DateUtil.formatString(start, "yyyy-MM-dd");
        Date endDate = DateUtil.formatString(end, "yyyy-MM-dd");

        PageInfo<List<Order>> pageInfo = (PageInfo<List<Order>>) orderService.getOrdersByBusinessmanIdInDaysWithPage(page, pageSize, businessman.getId(), startDate, endDate);

        BigDecimal totalMoney = orderService.getOrdersTotalMoneyByBusinessmanIdInDays(businessman.getId(), startDate, endDate);

        watchOrdersStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
        watchOrdersStatus.put("pageInfo", pageInfo);
        watchOrdersStatus.put("totalMoney", totalMoney);

        return watchOrdersStatus;
    }

    /*
     * @Description:商家查看在售商品，不包括已被商家删除的
     * @Param:[session]
     * @Return:java.util.Map<java.lang.String,java.lang.Object>
     * @Author:TjSanshao
     * @Date:2018-12-12
     * @Time:16:19
     **/
    @RequestMapping("/tjsanshao/businessman/goods")
    public Map<String, Object> watchGoods(HttpSession session) {
        HashMap<String, Object> watchGoodsStatus = new HashMap<>();

        Businessman businessman = (Businessman)session.getAttribute(TGWStaticString.TGW_BUSINESSMAN);

        //根据商家id查询该商家的所有商品
        List<Object> goodsList = goodsService.findGoodsAndGoodsDetailAndGoodsImageWithBussinessId(businessman.getId());

        watchGoodsStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
        watchGoodsStatus.put("list", goodsList);

        return watchGoodsStatus;
    }

    /*
     * @Description:消费订单
     * @Param:[orderNumber, session]
     * @Return:java.util.Map<java.lang.String,java.lang.Object>
     * @Author:TjSanshao
     * @Date:2018-12-19
     * @Time:15:05
     **/
    @RequestMapping("/tjsanshao/businessman/orderConsume")
    public Map<String, Object> orderConsume(String orderNumber, HttpSession session) {

        HashMap<String, Object> consumeStatus = new HashMap<>();

        if (StringUtils.isEmpty(orderNumber)) {
            consumeStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            consumeStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "order is not valid");
            return consumeStatus;
        }

        Businessman businessman = (Businessman) session.getAttribute(TGWStaticString.TGW_BUSINESSMAN);

        Order order = orderService.getOrderByUniqueOrderNumber(orderNumber);

        //判断订单是否存在且状态是已付款
        if (order == null || order.getSellStatus().intValue() != 1) {
            consumeStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            consumeStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "order is not valid");
            return consumeStatus;
        }

        //判断订单是否是该商家的
        if (order.getTgwBusinessmanId().intValue() != businessman.getId().intValue()) {
            consumeStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            consumeStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "illegal operation");
            return consumeStatus;
        }

        //调用业务层接口，使用订单
        if (!orderService.orderUseFinish(order.getId())) {
            consumeStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_FAIL);
            consumeStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "Unknown Error");
            return consumeStatus;
        }

        consumeStatus.put(TGWStaticString.TGW_RESULT_STATUS, TGWStaticString.TGW_RESULT_STATUS_SUCCESS);
        consumeStatus.put(TGWStaticString.TGW_RESULT_MESSAGE, "success");

        return consumeStatus;
    }
}

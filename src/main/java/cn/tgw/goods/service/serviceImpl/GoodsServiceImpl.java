package cn.tgw.goods.service.serviceImpl;

import cn.tgw.admin.mapper.TgwManagerMapper;
import cn.tgw.admin.model.TgwFirstCategory;
import cn.tgw.admin.service.TgwFirstCategoryService;
import cn.tgw.common.utils.QiniuUtil;
import cn.tgw.goods.mapper.GoodsDetailMapper;
import cn.tgw.goods.mapper.GoodsImageMapper;
import cn.tgw.goods.mapper.GoodsMapper;
import cn.tgw.goods.model.Goods;
import cn.tgw.goods.model.GoodsDetail;
import cn.tgw.goods.model.GoodsImage;
import cn.tgw.goods.model.GoodsVO;
import cn.tgw.goods.service.GoodsService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * @Auther: 张华健
 * @Date: 2018/11/29 11:00
 * @Description:商品信息Service实现
 */
@Service

public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsDetailMapper goodsDetailMapper;
    @Autowired
    private GoodsImageMapper goodsImageMapper;
    @Autowired
    private TgwFirstCategoryService tgwFirstCategoryService;

    
    /**
     *
     * 功能描述: 下面三个方法都是用于首页数据的。
     * 参数type说明：
     *      1：查询热门团购展示
     *      2：查询最新上架团购展示
     *      3：按照分类查询团购展示
     *
     * @param:
     * @return: 
     * @auther: 张华健
     * @date:  2018/11/30
     */
    @Override
    public Object findHotGoodsByCity(String city) {

        Map<String,Object> param = new HashMap<>();
        //更正city，用于sql搜索
        String currentCity = "%"+city+"%";
        param.put("city",currentCity);
        param.put("type",1);
        param.put("firstCategoryName",null);
        List<GoodsVO> indexGoodsByCityAndTypeAndFirstCategoryName = goodsMapper.findIndexGoodsByCityAndTypeAndFirstCategoryName(param);
        return indexGoodsByCityAndTypeAndFirstCategoryName;
    }

    /**
     *
     * 功能描述: 寻找最新热卖团购
     *
     * @auther: 张华健
     * @date:  2018/12/13
     */
    @Override
    public Object findNewGoodsByCity(String city) {
        Map<String,Object> param = new HashMap<>();
        //更正city，用于sql搜索
        String currentCity = "%"+city+"%";
        param.put("city",currentCity);
        param.put("type",2);
        param.put("firstCategoryName",null);
        List<GoodsVO> indexGoodsByCityAndTypeAndFirstCategoryName = goodsMapper.findIndexGoodsByCityAndTypeAndFirstCategoryName(param);
        return indexGoodsByCityAndTypeAndFirstCategoryName;
    }

    /**
     *
     * 功能描述:按照一级分类寻找团购
     *
     * @auther: 张华健
     * @date:  2018/12/13
     */
    @Override
    public Object findGoodsByCityAndFirstCategory(String city) {
        Map<String,Object> param = new HashMap<>();
        //更正city，用于sql搜索
        String currentCity = "%"+city+"%";
        param.put("city",currentCity);
        param.put("type",3);


        JSONObject jsonObject = new JSONObject();



        List<TgwFirstCategory> tgwFirstCategories = tgwFirstCategoryService.allTgwFirstCategory();
        for (int i = 0; i < tgwFirstCategories.size(); i++) {
            TgwFirstCategory tgwFirstCategory = tgwFirstCategories.get(i);
            String categoryName = tgwFirstCategory.getCategoryName();
            param.put("firstCategoryName",categoryName);
            List<GoodsVO> indexGoodsByCityAndTypeAndFirstCategoryName = goodsMapper.findIndexGoodsByCityAndTypeAndFirstCategoryName(param);
            jsonObject.put(categoryName,indexGoodsByCityAndTypeAndFirstCategoryName);
        }

        return jsonObject;
    }


    /**
     *
     * 功能描述: 添加团购服务层处理
     *
     * @param: Goods goods, GoodsDetail goodsDetail, GoodsImage goodsImage
     * @return: String
     * @auther: 张华健
     * @date:  2018/12/04
     */
    @Transactional
    @Override
    public String addGoodsAndGoodsDetailAndGoodsImage(Goods goods, GoodsDetail goodsDetail, MultipartFile multipartFile) {

        //上传图片
        GoodsImage goodsImage = new GoodsImage();
        String returnUrl = null;
        try {
            returnUrl = QiniuUtil.uploadImg(multipartFile);
        } catch (IOException e) {
            return "error";
        }


//        //随机生成团购ID
        String toHash = goods.getGoodsTitle()+goods.getGoodsCategory()+goodsDetail.getGoodsDesc()+Math.random();
        int gid = Math.abs(toHash.hashCode());
        goods.setId(gid);
        //设置团购为在售
        goods.setIsOnline(1);
        //插入到数据库
        int resultGoodsInsert = goodsMapper.insertSelective(goods);

        //在团购详情中的tgwGoodsId写上团购id
        goodsDetail.setTgwGoodsId(gid);
        //设置创建团购的时间，为当前时间
        goodsDetail.setCreatGoodsTime(new Date());
        //设置团购的销量，初始为0
        goodsDetail.setSalesVolumn(0);
        //插入到数据库
        int resultGoodsDetailInsert = goodsDetailMapper.insertSelective(goodsDetail);

        //在团购图片中的tgwGoodsId写上团购id
        goodsImage.setTgwGoodsId(gid);
        goodsImage.setImageUrl(returnUrl);
        goodsImage.setIsMain(1);
        //插入到数据库
        int resultGoodsImageInsert = goodsImageMapper.insertSelective(goodsImage);

        //判断插入是否都执行，返回成功或者失败
        if(resultGoodsInsert==1&&resultGoodsDetailInsert==1&&resultGoodsImageInsert==1){
            return "success";
        }
        return "error";
    }

    /**
     *
     * 功能描述: 根据团购id返回团购相关所有信息
     *
     * @param: int id
     * @return:
     * @auther: 张华健
     * @date:  2018/12/06
     */
    @Override
    public Map<String,Object> findGoodsAndGoodsDetailAndGoodsImageWithGoodsId(int id) {
        Goods goods = goodsMapper.selectByPrimaryKey(id);
        Map<String,Object>resultMap = new HashMap<>();
        if(goods!=null){
            GoodsDetail goodsDetail = goodsDetailMapper.selectByTgwGoodsId(id);
            GoodsImage goodsImage = goodsImageMapper.selectByTgwGoodsId(id);

            resultMap.put("goods",goods);
            resultMap.put("goodsDetail",goodsDetail);
            resultMap.put("goodsImage",goodsImage);
        }
        return resultMap;
    }

    /**
     *
     * 功能描述: 根据商家id查询商家下的所有商品（不包括逻辑删除的）
     *
     * @param: Goods goods
     * @return:
     * @auther: 张华健
     * @date:  2018/12/06
     */
    @Override
    public List<Object> findGoodsAndGoodsDetailAndGoodsImageWithBussinessId(int businessmanId) {
        List<Object> resultList = new ArrayList<>();
        List<Goods> resultGoods = goodsMapper.selectByBusinessId(businessmanId);
        for (int i = 0; i < resultGoods.size(); i++) {
            if(resultGoods.get(i).getIsOnline()==1 ||resultGoods.get(i).getIsOnline()==0) {
                Integer id = resultGoods.get(i).getId();
                Map<String, Object> goodsAndGoodsDetailAndGoodsImageWithGoodsId = findGoodsAndGoodsDetailAndGoodsImageWithGoodsId(id);
                resultList.add(goodsAndGoodsDetailAndGoodsImageWithGoodsId);
            }
        }
        return resultList;
    }


    /**
     *
     * 功能描述:
     *
     * @param: 更新团购信息
     * @return: String result
     * @auther: 张华健
     * @date:  2018/12/06
     */
    @Transactional
    @Override
    public String updateGoodsByGoodsId(Goods goods,GoodsDetail goodsDetail,MultipartFile multipartFile) throws Exception {

        GoodsImage goodsImage = new GoodsImage();
        String returnUrl = null;
        //上传图片
        if(multipartFile!=null){


            try {
                returnUrl = QiniuUtil.uploadImg(multipartFile);
            } catch (IOException e) {
                return "error";
            }
        }


        int goodsId = goods.getId();
        GoodsDetail resultGoodsDetail = goodsDetailMapper.selectByTgwGoodsId(goodsId);
        GoodsImage resultGoodsImage = goodsImageMapper.selectByTgwGoodsId(goodsId);
        goodsDetail.setId(resultGoodsDetail.getId());
        goodsImage.setId(resultGoodsImage.getId());
        goodsImage.setIsMain(1);
        if(multipartFile!=null){
            goodsImage.setImageUrl(returnUrl);
        }else{
            goodsImage.setImageUrl(resultGoodsImage.getImageUrl());
        }

        int resNum = goodsMapper.updateByPrimaryKeySelective(goods);
        int resNum2 = goodsDetailMapper.updateByPrimaryKeySelective(goodsDetail);
        int resNum3 = goodsImageMapper.updateByPrimaryKeySelective(goodsImage);
        if(resNum+resNum2+resNum3>0){
            //删除七牛云旧照片
            if(multipartFile!=null){
                String currentUrl = resultGoodsImage.getImageUrl().replaceAll("http://"+QiniuUtil.path+"/","");
                QiniuUtil.deleteImage(currentUrl);
            }
            return "success";
        }else{
            throw new RuntimeException();
        }
    }



    /**
     *
     * 功能描述:
     *
     * @param: 逻辑删除该团购
     * @return:
     * @auther: 张华健
     * @date:  2018/12/10
     */
    @Override
    public Boolean deleteGoods(int goodsId) {
        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        goods.setIsOnline(2);
        int row = goodsMapper.updateByPrimaryKeySelective(goods);
        if(row>0){
            return true;
        }else {
            return false;
        }
    }

    /**
     *
     * 功能描述:
     *
     * @param: 上架该团购
     * @return:
     * @auther: 张华健
     * @date:  2018/12/10
     */
    @Override
    public Boolean upGoods(int goodsId) {
        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        goods.setIsOnline(1);
        int row = goodsMapper.updateByPrimaryKeySelective(goods);
        if(row>0){
            return true;
        }else {
            return false;
        }
    }

    /**
     *
     * 功能描述:
     *
     * @param: 下架该团购
     * @return:
     * @auther: 张华健
     * @date:  2018/12/10
     */
    @Override
    public Boolean downGoods(int goodsId) {
        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        goods.setIsOnline(0);
        int row = goodsMapper.updateByPrimaryKeySelective(goods);
        if(row>0){
            return true;
        }else {
            return false;
        }
    }

    @Override
    public List<GoodsVO> findAllGoodsVO(){
      return   goodsMapper.findAllGoodsVO();
    }



    /**
     *
     * 功能描述: 下单减少商品库存，增加销量
     *
     * @param: 团购id，团购数量
     * @return:
     * @auther: 张华健
     * @date:  2018/12/11
     */
    @Override
    public String updateGoodsRepertory(int goodsId, int num) {
        GoodsDetail resDetail = goodsDetailMapper.selectByTgwGoodsId(goodsId);
        int resRepertory = resDetail.getGoodsRepertory();
        if(resRepertory<num){
            return "error";
        }
        resRepertory = resRepertory-num;
        resDetail.setGoodsRepertory(resRepertory);
        resDetail.setSalesVolumn(resDetail.getSalesVolumn()+num);
        int i = goodsDetailMapper.updateByPrimaryKeySelective(resDetail);
        if(i!=1){
            return "error";
        }
        return "success";
    }

    /**
     *
     * 功能描述:下单增加商品销量（不使用库存的情况下使用）
     * @auther: 张华健
     * @date:  2018/12/13
     */
    @Override
    public String addGoodsSalesVolumn(int goodsId, int num) {
        GoodsDetail resDetail = goodsDetailMapper.selectByTgwGoodsId(goodsId);
        resDetail.setSalesVolumn(resDetail.getSalesVolumn()+num);
        int i = goodsDetailMapper.updateByPrimaryKeySelective(resDetail);
        if(i!=1){
            return "error";
        }
        return "success";

    }

    /**
     *
     * 功能描述:根据商家id查询商品（查询全部的，包括逻辑删除的）
     * @auther: 张华健
     * @date:  2018/12/13
     */
    @Override
    public List<Goods> findGoodsByBusinessmanId(int businessmanId) {
        return goodsMapper.selectByBusinessId(businessmanId);
    }

    /**
     *
     * 功能描述: 根据商家id查询商品(不包括逻辑删除的）
     *
     * @auther: 张华健
     * @date:  2018/12/13
     */
    @Override
    public List<Goods> findGoodsByBusinessmanIdWithIsOnline(int businessmanId){
        return goodsMapper.selectByBusinessIdWithIsOnline(businessmanId);
    }

    @Override
    public Map<String, Object> findGoodsBySearchOptionAndCity(String searchOption, String city, int num, int size) {
        String currentSearchOption = "%"+searchOption+"%";
        String currentCity = "%"+city+"%";
        Map<String, Object>map = new HashMap<>();
        PageHelper.startPage(num,size);
        List<GoodsVO> goodsBySearchOption = goodsMapper.findGoodsBySearchOption(currentSearchOption, currentCity);
        PageInfo<GoodsVO> pageInfo = new PageInfo<>(goodsBySearchOption);
        long total = pageInfo.getTotal();
        List<GoodsVO> list = pageInfo.getList();
        map.put("resultGoods",list);
        map.put("total",total);
        return map;
    }


}

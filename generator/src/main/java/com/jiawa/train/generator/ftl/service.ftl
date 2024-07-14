package com.jiawa.train.${module}.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.${module}.domain.${Domain};
import com.jiawa.train.${module}.domain.${Domain}Example;
import com.jiawa.train.${module}.mapper.${Domain}Mapper;
import com.jiawa.train.${module}.req.${Domain}QueryReq;
import com.jiawa.train.${module}.req.${Domain}SaveReq;
import com.jiawa.train.${module}.resp.${Domain}QueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ${Domain}Service {
    private static final Logger LOG = LoggerFactory.getLogger(${Domain}Service.class);

    @Resource
    private ${Domain}Mapper ${domain}Mapper;

    /**
     * 1.新增乘客  2.修改乘客
     * @param req
     */
    public void save(${Domain}SaveReq req){
        DateTime now = DateTime.now();
        ${Domain} ${domain} = BeanUtil.copyProperties(req, ${Domain}.class);
        if (ObjectUtil.isNull(${domain}.getId())) {
            ${domain}.setId(SnowUtil.getSnowflakeNextId());
            ${domain}.setCreateTime(now);
            ${domain}.setUpdateTime(now);
            ${domain}Mapper.insert(${domain});
        } else {
            ${domain}.setUpdateTime(now);
            ${domain}.setCreateTime(req.getCreateTime());
            ${domain}Mapper.updateByPrimaryKey(${domain});
        }
    }

    /**
     * 乘客查询 1.控制端查询所有乘客  2.${module}查询当前乘客
     * @param req
     */
    public PageResp<${Domain}QueryResp> queryList(${Domain}QueryReq req){
        ${Domain}Example ${domain}Example = new ${Domain}Example();

        ${domain}Example.setOrderByClause("id desc");
        ${Domain}Example.Criteria criteria = ${domain}Example.createCriteria();


        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<${Domain}> ${domain}List = ${domain}Mapper.selectByExample(${domain}Example);

        // 获取总数
        PageInfo<${Domain}> ${domain}PageInfo = new PageInfo<>(${domain}List);
        LOG.info("总行数：{}", ${domain}PageInfo.getTotal());
        LOG.info("总页数：{}", ${domain}PageInfo.getPages());

        List<${Domain}QueryResp> ${domain}QueryRespList = BeanUtil.copyToList(${domain}List, ${Domain}QueryResp.class);

        PageResp<${Domain}QueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(${domain}PageInfo.getTotal());
        objectPageResp.setList(${domain}QueryRespList);
        return objectPageResp;
    }

    /**
     * 乘客删除
     * @param id
     */
    public void delete(Long id){
        ${domain}Mapper.deleteByPrimaryKey(id);
    }
}

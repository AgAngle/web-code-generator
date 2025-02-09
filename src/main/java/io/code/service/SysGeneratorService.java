/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package io.code.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.code.config.MongoManager;
import io.code.dao.GeneratorDao;
import io.code.dao.MongoDBGeneratorDao;
import io.code.factory.MongoDBCollectionFactory;
import io.code.utils.GenUtils;
import io.code.utils.PageUtils;
import io.code.utils.Query;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 代码生成器
 *
 * @author Mark sunlightcs@gmail.com
 */
@Service
public class SysGeneratorService {
    @Autowired
    private GeneratorDao generatorDao;


    public PageUtils queryList(Query query) {
        Page<?> page = PageHelper.startPage(query.getPage(), query.getLimit());
        List<Map<String, Object>> list = generatorDao.queryList(query);
        int total = (int) page.getTotal();
        if (generatorDao instanceof MongoDBGeneratorDao) {
            total = MongoDBCollectionFactory.getCollectionTotal(query);
        }
        return new PageUtils(list, total, query.getLimit(), query.getPage());
    }

    public Map<String, String> queryTable(String tableName) {
        return generatorDao.queryTable(tableName);
    }

    public List<Map<String, String>> queryColumns(String tableName) {
        return generatorDao.queryColumns(tableName);
    }


    public void generatorCode(List<String> tableNames) {

        try {
            FileUtils.deleteDirectory(new File(GenUtils.ROOT_PATH + File.separator + GenUtils.DEFAULT_DIR_NAME));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String tableName : tableNames) {
            //查询表信息
            Map<String, String> table = queryTable(tableName);
            //查询列信息
            List<Map<String, String>> columns = queryColumns(tableName);
            //生成代码
            GenUtils.generatorCode(table, columns);
        }
        if (MongoManager.isMongo()) {
            GenUtils.generatorMongoCode(tableNames);
        }
    }
}

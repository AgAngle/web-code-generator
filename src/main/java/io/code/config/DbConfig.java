/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package io.code.config;

import io.code.dao.GeneratorDao;
import io.code.dao.MySQLGeneratorDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 数据库配置
 *
 * @author Mark sunlightcs@gmail.com
 */
@Configuration
public class DbConfig {
    @Autowired
    private MySQLGeneratorDao mySQLGeneratorDao;


    @Bean
    @Primary
    public GeneratorDao getGeneratorDao() {
        return mySQLGeneratorDao;
    }
}

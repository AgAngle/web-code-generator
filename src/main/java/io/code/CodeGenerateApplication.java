package io.code;

import io.code.service.SysGeneratorService;
import io.code.utils.GenUtils;
import org.apache.commons.configuration.Configuration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

/**
 * @author jianxing
 */
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@MapperScan("io.code.dao")
public class CodeGenerateApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(CodeGenerateApplication.class, args);
		Configuration config = GenUtils.getConfig();
		String[] tableNames = config.getStringArray("tableNames");
		applicationContext.getBean(SysGeneratorService.class)
				.generatorCode(List.of(tableNames));
	}
}

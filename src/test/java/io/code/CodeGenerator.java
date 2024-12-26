package io.code;

import io.code.service.SysGeneratorService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CodeGenerator {

	@Resource
	SysGeneratorService sysGeneratorService;

	@Test
	public void generate() {
		List<String> tableNames = List.of("user");
		sysGeneratorService.generatorCode(tableNames);
	}

}

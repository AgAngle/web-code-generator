package io.code.utils;

import io.code.config.MongoManager;
import io.code.entity.ColumnEntity;
import io.code.entity.TableEntity;
import io.code.entity.mongo.MongoDefinition;
import io.code.entity.mongo.MongoGeneratorEntity;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 代码生成器   工具类
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2016年12月19日 下午11:40:24
 */
public class GenUtils {

    public static final String ROOT_PATH = "src" + File.separator + "main" + File.separator + "resources";
    public static final String DEFAULT_DIR_NAME = "output";

    public static List<Path> getTemplates() {
        String path = "src/main/resources/template";
        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            return paths
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RRException("获取模板文件失败", e);
        }
    }

    public static List<String> getMongoChildTemplates() {
        List<String> templates = new ArrayList<String>();
        templates.add("template/MongoChildrenEntity.java.vm");
        return templates;
    }

    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> table,
                                     List<Map<String, String>> columns) {

        try {

            VelocityContext context = getVelocityContext(table, columns);

            String className = (String) context.get("className");
            //获取模板列表
            List<Path> templates = getTemplates();
            for (Path template : templates) {
                String templateStr = template.toString();

                String filePath = templateStr.replace("template" + File.separator, StringUtils.EMPTY).replace(".vm", StringUtils.EMPTY);
                int divisionIndex = filePath.lastIndexOf(File.separator);
                String dirPath = filePath.substring(0, divisionIndex);
                String fileName = filePath.substring(divisionIndex + 1);
                fileName = fileName.replace("{className}", className);
                dirPath = ROOT_PATH + File.separator + DEFAULT_DIR_NAME + dirPath.replace(ROOT_PATH, StringUtils.EMPTY);
                filePath = dirPath + File.separator + fileName;

                // 创建目录
                new File(dirPath).mkdirs();

                // 创建文件
                File file = new File(filePath);
                file.createNewFile();

                // 写入文件
                //渲染模板
                StringWriter sw = new StringWriter();
                Template tpl = Velocity.getTemplate(templateStr.replace(ROOT_PATH, StringUtils.EMPTY), "UTF-8");
                tpl.merge(context, sw);
                Files.write(Paths.get(filePath), sw.toString().getBytes());

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static VelocityContext getVelocityContext(Map<String, String> table, List<Map<String, String>> columns) {
        //配置信息
        Configuration config = getConfig();
        boolean hasBigDecimal = false;
        boolean hasList = false;
        if (table == null) {
            throw new RRException("表名不存在");
        }
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));
        tableEntity.setComments(table.get("tableComment"));
        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), config.getStringArray("tablePrefix"));
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));

        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        for (Map<String, String> column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName"));
            columnEntity.setDataType(column.get("dataType"));
            columnEntity.setComments(column.get("columnComment"));
            columnEntity.setExtra(column.get("extra"));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), columnToJava(columnEntity.getDataType()));
            columnEntity.setAttrType(attrType);


            if (!hasBigDecimal && attrType.equals("BigDecimal")) {
                hasBigDecimal = true;
            }
            if (!hasList && "array".equals(columnEntity.getExtra())) {
                hasList = true;
            }
            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            if (column.get("length") != null) {
                columnEntity.setLength(Long.valueOf(column.get("length")));
            }
            columnEntity.setIsNullable(column.get("isNullable"));

            columsList.add(columnEntity);
        }
        tableEntity.setColumns(columsList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        String mainPath = config.getString("mainPath");
        mainPath = StringUtils.isBlank(mainPath) ? "io.code" : mainPath;
        String class_name = camelToSnake(tableEntity.getClassname());
        String permissionPrefix = (config.getString("moduleName") + "_" + class_name).toUpperCase();

        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableEntity.getTableName());
        map.put("comments", tableEntity.getComments());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("class_name", class_name);
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("hasList", hasList);
        map.put("mainPath", mainPath);
        map.put("package", config.getString("package"));
        map.put("moduleName", config.getString("moduleName"));
        map.put("author", config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        map.put("permissionPrefix", permissionPrefix);
        return new VelocityContext(map);
    }

    private record Result(TableEntity tableEntity, VelocityContext context) {
    }

    /**
     * 生成mongo其他实体类的代码
     */
    public static void generatorMongoCode(List<String> tableNames) {
        for (String tableName : tableNames) {
            MongoDefinition info = MongoManager.getInfo(tableName);
            List<MongoGeneratorEntity> childrenInfo = info.getChildrenInfo(tableName);
            childrenInfo.remove(0);
            for (MongoGeneratorEntity mongoGeneratorEntity : childrenInfo) {
                generatorChildrenBeanCode(mongoGeneratorEntity);
            }
        }
    }

    private static void generatorChildrenBeanCode(MongoGeneratorEntity mongoGeneratorEntity) {
        //配置信息
        Configuration config = getConfig();
        boolean hasList = false;
        //表信息
        TableEntity tableEntity = mongoGeneratorEntity.toTableEntity();
        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), config.getStringArray("tablePrefix"));
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));
        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        for (Map<String, String> column : mongoGeneratorEntity.getColumns()) {
            ColumnEntity columnEntity = new ColumnEntity();
            String columnName = column.get("columnName");
            if (columnName.contains(".")) {
                columnName = columnName.substring(columnName.lastIndexOf(".") + 1);
            }
            columnEntity.setColumnName(columnName);
            columnEntity.setDataType(column.get("dataType"));
            columnEntity.setExtra(column.get("extra"));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), columnToJava(columnEntity.getDataType()));
            columnEntity.setAttrType(attrType);

            if (!hasList && "array".equals(columnEntity.getExtra())) {
                hasList = true;
            }
            columsList.add(columnEntity);
        }
        tableEntity.setColumns(columsList);

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        String mainPath = config.getString("mainPath");
        mainPath = StringUtils.isBlank(mainPath) ? "io.renren" : mainPath;
        String permissionPrefix = camelToSnake((config.getString("moduleName") + "_" + tableEntity.getClassname()).toUpperCase());
        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableEntity.getTableName());
        map.put("comments", tableEntity.getComments());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasList", hasList);
        map.put("mainPath", mainPath);
        map.put("package", config.getString("package"));
        map.put("moduleName", config.getString("moduleName"));
        map.put("author", config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        map.put("permissionPrefix", permissionPrefix);
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getMongoChildTemplates();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);
        }
    }

    public static String camelToSnake(String camelCase) {
        StringBuilder result = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append('_').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String[] tablePrefixArray) {
        if (null != tablePrefixArray && tablePrefixArray.length > 0) {
            for (String tablePrefix : tablePrefixArray) {
                if (tableName.startsWith(tablePrefix)) {
                    tableName = tableName.replaceFirst(tablePrefix, "");
                }
            }
        }
        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new RRException("获取配置文件失败，", e);
        }
    }

    private static String splitInnerName(String name) {
        name = name.replaceAll("\\.", "_");
        return name;
    }
}

**项目说明** 

该项目可生成entity、xml、dao、service、html、js、sql代码，减少重复的的开发工作

项目参考人人代码生成器：https://gitee.com/renrenio/renren-generator

**实现效果**

项目运行会自动根据 resource/template 中的模板，生成对应的代码到 resource/output 目录下

**使用方法**
- 修改 application.yml，配置 MySQL 账号和密码、数据库名称
- 修改 generator.properties，配置作者、包名、表名等信息
- Eclipse、IDEA运行 SPRING_BOOT 项目，则可启动项目
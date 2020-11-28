

Hope 股票分析系统
======

使用微服务架构构建的股票分析系统，基于Spring Cloud。

spider-microservice  爬虫，抓股票数据
stock-microservice   存储股票数据
analyzer-microservice   分析数据，生成报表

存储使用Redis, 同时提供了Mysql, 文件系统 的备用方案。
报表生成后存放在阿里云OSS



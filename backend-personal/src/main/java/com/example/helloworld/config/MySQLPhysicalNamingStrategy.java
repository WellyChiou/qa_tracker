package com.example.helloworld.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * MySQL 物理命名策略
 * 自動為所有表名和列名添加反引號，處理 MySQL 保留字（如 groups）
 */
public class MySQLPhysicalNamingStrategy extends PhysicalNamingStrategyStandardImpl {

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        String tableName = name.getText();
        // 如果表名沒有反引號，則添加
        if (!tableName.startsWith("`") && !tableName.endsWith("`")) {
            return Identifier.toIdentifier("`" + tableName + "`", name.isQuoted());
        }
        return name;
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        String columnName = name.getText();
        // 如果列名沒有反引號，則添加
        if (!columnName.startsWith("`") && !columnName.endsWith("`")) {
            return Identifier.toIdentifier("`" + columnName + "`", name.isQuoted());
        }
        return name;
    }
}


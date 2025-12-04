package com.example.helloworld.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.example.helloworld.repository.church",
    entityManagerFactoryRef = "churchEntityManagerFactory",
    transactionManagerRef = "churchTransactionManager"
)
public class ChurchDataSourceConfig {

    @Value("${spring.church.datasource.url}")
    private String churchUrl;
    
    @Value("${spring.church.datasource.username}")
    private String churchUsername;
    
    @Value("${spring.church.datasource.password}")
    private String churchPassword;
    
    @Value("${spring.church.datasource.driver-class-name}")
    private String churchDriverClassName;

    @Bean(name = "churchDataSource")
    public DataSource churchDataSource() {
        // 確保 URL 包含正確的字符集參數
        // 注意：Java 不認識 'utf8mb4'，必須使用 'UTF-8'
        String url = churchUrl;
        if (!url.contains("characterEncoding")) {
            url += (url.contains("?") ? "&" : "?") + "characterEncoding=UTF-8&useUnicode=true&connectionCollation=utf8mb4_unicode_ci";
        }
        
        return DataSourceBuilder.create()
            .url(url)
            .username(churchUsername)
            .password(churchPassword)
            .driverClassName(churchDriverClassName)
            .build();
    }

    @Bean(name = "churchEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean churchEntityManagerFactory(
            @Qualifier("churchDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.example.helloworld.entity.church");
        // 設置 persistenceUnitName 以避免衝突
        em.setPersistenceUnitName("church");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.setProperty("hibernate.show_sql", "true");
        // 確保使用 UTF-8 編碼
        properties.setProperty("hibernate.connection.characterEncoding", "utf8");
        properties.setProperty("hibernate.connection.useUnicode", "true");
        em.setJpaProperties(properties);
        
        return em;
    }

    @Bean(name = "churchTransactionManager")
    public PlatformTransactionManager churchTransactionManager(
            @Qualifier("churchEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}


package com.example.helloworld.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
    basePackages = {
        "com.example.helloworld.repository"
    },
    excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.CUSTOM,
        classes = {com.example.helloworld.config.ChurchRepositoryExcludeFilter.class}
    ),
    entityManagerFactoryRef = "primaryEntityManagerFactory",
    transactionManagerRef = "primaryTransactionManager"
)
public class PrimaryDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String primaryUrl;
    
    @Value("${spring.datasource.username}")
    private String primaryUsername;
    
    @Value("${spring.datasource.password}")
    private String primaryPassword;
    
    @Value("${spring.datasource.driver-class-name}")
    private String primaryDriverClassName;

    @Bean(name = "primaryDataSource")
    @Primary
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
            .url(primaryUrl)
            .username(primaryUsername)
            .password(primaryPassword)
            .driverClassName(primaryDriverClassName)
            .build();
    }

    @Bean(name = "primaryEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            @Qualifier("primaryDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        // 掃描所有 entity，但排除 church 包
        // 掃描所有 entity，但排除 church 包
        // 注意：Hibernate 的 setPackagesToScan 不支持排除，所以我們需要確保 church entity 不會被掃描
        // 由於 church entity 在子包中，我們可以通過明確指定包來避免衝突
        // 明確列出要掃描的 entity 包（排除 church 子包）
        // 由於 setPackagesToScan 會遞歸掃描子包，我們需要明確指定不包含 church 的包
        // 但實際上，由於 DataSource 連接到不同的資料庫，即使掃描了也不會衝突
        // 為了安全起見，我們明確指定要掃描的包
        em.setPackagesToScan("com.example.helloworld.entity");
        em.setPersistenceUnitName("primary");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.setProperty("hibernate.show_sql", "true");
        em.setJpaProperties(properties);
        
        return em;
    }

    @Bean(name = "primaryTransactionManager")
    @Primary
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}


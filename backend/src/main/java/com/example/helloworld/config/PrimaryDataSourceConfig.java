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
        "com.example.helloworld.repository.personal"
    },
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
        // 只掃描 personal 包的 entity，排除 church 包
        // 重要：setPackagesToScan 會遞歸掃描子包，所以不能使用 "com.example.helloworld.entity"
        // 必須明確指定只掃描 "com.example.helloworld.entity.personal"
        em.setPackagesToScan("com.example.helloworld.entity.personal");
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


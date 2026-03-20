package com.example.helloworld.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.example.helloworld.repository.invest",
    entityManagerFactoryRef = "investEntityManagerFactory",
    transactionManagerRef = "investTransactionManager"
)
public class InvestDataSourceConfig {

    @Value("${spring.invest.datasource.url}")
    private String investUrl;

    @Value("${spring.invest.datasource.username}")
    private String investUsername;

    @Value("${spring.invest.datasource.password}")
    private String investPassword;

    @Value("${spring.invest.datasource.driver-class-name}")
    private String investDriverClassName;

    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String hibernateDdlAuto;

    @Value("${spring.jpa.show-sql:false}")
    private String hibernateShowSql;

    @Bean(name = "investDataSource")
    public DataSource investDataSource() {
        return DataSourceBuilder.create()
            .url(investUrl)
            .username(investUsername)
            .password(investPassword)
            .driverClassName(investDriverClassName)
            .build();
    }

    @Bean(name = "investEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean investEntityManagerFactory(
            @Qualifier("investDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.example.helloworld.entity.invest");
        em.setPersistenceUnitName("invest");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", hibernateDdlAuto);
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.setProperty("hibernate.show_sql", hibernateShowSql);
        em.setJpaProperties(properties);

        return em;
    }

    @Bean(name = "investTransactionManager")
    public PlatformTransactionManager investTransactionManager(
            @Qualifier("investEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}

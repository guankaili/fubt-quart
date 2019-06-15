package com.fubt.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.beetl.sql.core.ClasspathLoader;
import org.beetl.sql.core.Interceptor;
import org.beetl.sql.core.UnderlinedNameConversion;
import org.beetl.sql.core.db.MySqlStyle;
import org.beetl.sql.ext.DebugInterceptor;
import org.beetl.sql.ext.spring4.BeetlSqlDataSource;
import org.beetl.sql.ext.spring4.SqlManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * <p>@Description: </p>
 *
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public SqlManagerFactoryBean sqlManagerFactoryBean(DataSource dataSource) {
        SqlManagerFactoryBean factory = new SqlManagerFactoryBean();

        BeetlSqlDataSource source = new BeetlSqlDataSource();
        source.setMasterSource(dataSource);
        factory.setCs(source);
        factory.setDbStyle(new MySqlStyle());
        factory.setNc(new UnderlinedNameConversion());
        factory.setInterceptors(new Interceptor[]{new DebugInterceptor()});
        factory.setSqlLoader(new ClasspathLoader("/sql"));

        return factory;
    }

    @Bean
    public DataSource dataSource(Environment environment) {
        DruidDataSource dataSource = new DruidDataSource();

        dataSource.setUrl(environment.getProperty("spring.datasource.url"));
        dataSource.setUsername(environment.getProperty("spring.datasource.username"));
        dataSource.setPassword(environment.getProperty("spring.datasource.password"));
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

}

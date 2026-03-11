package co.kr.coresolutions.quadengine.query.dao;

import co.kr.coresolutions.quadengine.query.configuration.ConnectionInfo;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public abstract class AbstractDao {

    protected SessionFactory createSessionFactory(ConnectionInfo connectionInfo) {
        String dbms = connectionInfo.getDbms();
        Configuration cfg = new Configuration()
                .setProperty("hibernate.dialect",
                        dbms.equalsIgnoreCase("MARIADB") ? "org.hibernate.dialect.MariaDBDialect" :
                            dbms.equalsIgnoreCase("MSSQL") ? "org.hibernate.dialect.SQLServerDialect" :
                                    dbms.equalsIgnoreCase("ORACLE") ? "org.hibernate.dialect.Oracle10gDialect" :
                                            dbms.equalsIgnoreCase("DRUID") ? "org.hibernate.dialect.MySQLDialect" :
                                                    dbms.equalsIgnoreCase("MYSQL") ? "org.hibernate.dialect.MySQLDialect" :
                                                            dbms.equalsIgnoreCase("IGNITE") ? "org.hibernate.dialect.MySQLDialect" :
                                                                    dbms.equalsIgnoreCase("ClickHouse") ? "org.hibernate.dialect.MySQLDialect" :
                                                                            dbms.equalsIgnoreCase("SQLITE") ? "org.hibernate.dialect.MySQLDialect" :
                                                                                    dbms.equalsIgnoreCase("DB2") ? "org.hibernate.dialect.DB2Dialect" :
                                                                                            dbms.equalsIgnoreCase("HANA") ? "org.hibernate.dialect.AbstractHANADialect" :
                                                                                                    dbms.equalsIgnoreCase("POSTGRESQL") ? "hibernate.dialect.PostgreSQL9Dialect" :
                                                                                                            dbms.equalsIgnoreCase("SNOWFLAKE") ? "org.hibernate.dialect.SQLServerDialect" : "")
                .setProperty("hibernate.connection.driver_class", connectionInfo.getDriverClassName())
                .setProperty("hibernate.connection.url", connectionInfo.getUrl())
                .setProperty("hibernate.connection.password", connectionInfo.getPassword())
                .setProperty("hibernate.connection.username", connectionInfo.getUsername())
                .setProperty("hibernate.order_inserts", "true")
                .setProperty("hibernate.connection.CharSet", "utf8")
                .setProperty("hibernate.jdbc.batch_size", "50")
                .setProperty("hibernate.connection.characterEncoding", "utf8")
                .setProperty("hibernate.connection.useUnicode", "true")
                .setProperty("hibernate.jdbc.batch_versioned_data", "true")
                .setProperty("hibernate.generate_statistics", "true")
                .setProperty("hibernate.current_session_context_class", "org.hibernate.context.internal.ThreadLocalSessionContext")
                .setProperty("javax.persistence.query.timeout", "10");
        return cfg.buildSessionFactory();
    }

}

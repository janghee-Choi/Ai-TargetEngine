package co.kr.coresolutions.quadengine.common.config;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.kr.coresolutions.quadengine.query.util.AES256Cipher;

@Configuration
public class SpringDataSourceConfig {
    @Value("${app.encrypt.password}")
    private Boolean encryptPassword;
    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    public DataSource dataSource() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        String decryptedPassword = AES256Cipher.AES_Decode(password, encryptPassword);
        return DataSourceBuilder.create().url(url).username(username).password(decryptedPassword)
                .driverClassName(driverClassName).build();
    }

}
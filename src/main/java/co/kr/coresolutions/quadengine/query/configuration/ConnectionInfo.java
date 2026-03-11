package co.kr.coresolutions.quadengine.query.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password"})
public class ConnectionInfo {

	@JsonProperty("CONNNAME")
	private String connName;

	@JsonProperty("DBMS")
	private String dbms;

	@JsonProperty("DRIVER")
	private String driverClassName;

	@JsonProperty("URL")
	private String url;

	@JsonProperty("ID")
	private String username;

	@JsonProperty("PW")
	private String password;

	@JsonProperty("OWNER")
	private String owner;

	@JsonProperty("ETC")
	private String etc;

	@JsonProperty("SCHEMA")
	private String schema;

	@JsonProperty("Host")
	private String host;

	@JsonProperty("Port")
	private String port;

	@JsonProperty("UrlHttp")
	private String httpUrl;

	@JsonProperty(value = "PW_CONNECTION")
	private String passwordConnection;

	@JsonProperty(value = "dbmsoption_prefix")
	private String optionPrefix;

}

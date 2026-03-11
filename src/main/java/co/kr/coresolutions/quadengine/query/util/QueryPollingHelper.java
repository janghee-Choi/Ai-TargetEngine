package co.kr.coresolutions.quadengine.query.util;

import co.kr.coresolutions.quadengine.query.outbound.connection.KafkaConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


//TODO QueryPollingHelper 제거할것.
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryPollingHelper {

	private static final String KAFKA_GROUP_ID_CONFIG = "easycore";

	private final ObjectMapper objectMapper;

	//TODO JSONObject 제거하고 Jackson으로 변경할것!
//	public String getInsertSqlQuery(Map<String, String> dataTypes, JsonNode jsonNode, String targetTable, String dbms) {
//		StringBuilder stringBuilder = new StringBuilder();
//		StringBuilder globalValues = new StringBuilder();
//		StreamSupport.stream(jsonNode.spliterator(), false).forEach(node -> {
//			JSONObject jsonObject = new JSONObject(node.toString());
//			stringBuilder.append("(");
//			dataTypes.forEach((key, value) -> {
//				String objectValue = getJsonObjectValue(key, jsonObject);
//
//				if (!objectValue.isEmpty()) {
//					if (!value.toLowerCase().contains("char")) {
//						stringBuilder.append(objectValue).append(",");
//					} else {
//						if (objectValue.toLowerCase().equals("null")) {
//							stringBuilder.append(objectValue).append(",");
//						} else {
//							//
//							objectValue = objectValue.replaceAll("'", "''");
//							stringBuilder.append("'").append(objectValue).append("',");
//						}
//					}
//				} else {
//					stringBuilder.append("null").append(",");
//				}
//			});
//
//			globalValues.append(stringBuilder.substring(0, stringBuilder.length() - 1)).append("),,");
//			stringBuilder.delete(0, stringBuilder.length());
//		});
//		stringBuilder.delete(0, stringBuilder.length());
//		stringBuilder.append("(");
//		dataTypes.keySet().forEach(s -> stringBuilder.append(s).append(","));
//		String finalNames = (stringBuilder.substring(0, stringBuilder.length() - 1) + ")").toUpperCase();
//		if (dbms.equalsIgnoreCase("ORACLE")) {
//			return "INSERT ALL\n" + (Arrays.stream(globalValues.substring(0, globalValues.length() - 2).split(",,"))
//					.map(s1 -> " INTO " + targetTable + finalNames + " VALUES " + s1)
//					.collect(Collectors.joining("\n"))
//			) + " \nSELECT COUNT(*) FROM " + targetTable;
//		} else {
//			if (dbms.equalsIgnoreCase("MSSQL")) {
//				return "INSERT INTO " + targetTable + finalNames + " SELECT * FROM (VALUES " +
//						globalValues.substring(0, globalValues.length() - 2).replace(",,", ",") + ") A " + finalNames + ";";
//			}
//			return "INSERT INTO " + targetTable + finalNames + " VALUES " + globalValues.substring(0, globalValues.length() - 2).replace(",,", ",");
//		}
//	}
//
//	private static String getJsonObjectValue(String key, JSONObject jsonObject) {
//		String objectValue = "";
//		if (jsonObject.has(key.toUpperCase())) {
//			objectValue = jsonObject.get(key.toUpperCase()).toString();
//		} else if (jsonObject.has(key.toLowerCase())) {
//			objectValue = jsonObject.get(key.toLowerCase()).toString();
//		}
//		return objectValue;
//	}

	//TODO 삭제
	public List<String> getOutData(Object jsonOrList, String format) {
		if (format.equalsIgnoreCase("csv")) {
			return (List<String>) jsonOrList;
		} else {
			return StreamSupport.stream(((JsonNode) jsonOrList).spliterator(), true).map(JsonNode::toString).collect(Collectors.toList());
		}
	}

	// KafkaSink 안에서 할 것
	public Properties getProperties(String fileContent) {
		try {
			KafkaConnection kafkaConnection = objectMapper.readValue(fileContent, KafkaConnection.class);
			Properties properties = new Properties();
			properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnection.getIp());
			properties.put(ConsumerConfig.GROUP_ID_CONFIG, KAFKA_GROUP_ID_CONFIG);
			properties.put("key.serializer", StringSerializer.class);
			properties.put("value.serializer", StringSerializer.class);
			properties.put("topic", kafkaConnection.getTopic());
			properties.put("batch.size", 10000);
			properties.put("retries", 0);
			properties.put("delivery.timeout.ms", 3000);
			properties.put("connections.max.idle.ms", 10000);
			properties.put("request.timeout.ms", 5000);
			return properties;
		} catch (Exception e) {
			log.error("exception, ", e);
		}
		return null;
	}
}

package co.kr.coresolutions.quadengine.query.executor;

import co.kr.coresolutions.quadengine.query.domain.PollingTarget;
import co.kr.coresolutions.quadengine.query.domain.PollingTaskResult;
import co.kr.coresolutions.quadengine.query.outbound.SinkContext;
import co.kr.coresolutions.quadengine.query.outbound.SinkRouter;
import co.kr.coresolutions.quadengine.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollingExecutor {

	//TODO 제거목록
	private static final Map<String, String> latestOutputFileNameMap = new ConcurrentHashMap<>();
	private static final Map<String, String> latestdpIDTimeValueNMap = new ConcurrentHashMap<>();
	private static final Map<String, String> latestdpIDNameValueNMap = new ConcurrentHashMap<>();
	private static final Map<String, Object> latestdpIDScheduleObjectMap = new ConcurrentHashMap<>();

	//TODO 암호화 관련 따로 컴포넌트로 만들것 (Util)
	@Value("${app.encrypt.password:false}")
	private Boolean encryptPassword;

	private final QueryService queryService;
	private final SinkRouter sinkRouter;

	public PollingTaskResult executeTarget(PollingTarget target, List<Map<String, Object>> originList, String dpSqlField) {
		String dpTarget = Objects.toString(target.getDpTarget(), "");
		String outConnId = Objects.toString(target.getOutConnId(), "");
		String messageId = UUID.randomUUID().toString();

		try {
			//TODO connection 체크 최초 한번만 수행
			if (requiresConnectionFile(dpTarget) && !queryService.isConnectionIdFileExists(outConnId)) {
				String message = "fail due to outConnection file with name\t" + outConnId + "\tdoesn't exist";
				return new PollingTaskResult(target.getDpOutSeq(), false, 0, message);
			}

			int records = originList.size();
			SinkContext context = new SinkContext(this, target, originList, dpSqlField, messageId);
			boolean success = sinkRouter.route(dpTarget).send(context);

			String message = success ? "success" : "failed";

			return new PollingTaskResult(target.getDpOutSeq(), success, records, message);
		} catch (Exception e) {
			log.error("executeTarget failed. dpId={}, outSeq={}", target.getDpId(), target.getDpOutSeq(), e);
			return new PollingTaskResult(target.getDpOutSeq(), false, 0, e.getMessage());
		}
	}

	//TODO connection 체크 최초 한번만 수행
	private boolean requiresConnectionFile(String dpTarget) {
		return !("FILE".equalsIgnoreCase(dpTarget) || "S3".equalsIgnoreCase(dpTarget));
	}

//	public boolean executeTableTarget(PollingTarget target, JsonNode jsonNode, String dpSqlField, String messageId) {
//		String outConnId = Objects.toString(target.getOutConnId(), "");
//		String dbOutMode = Objects.toString(target.getDpOutMode(), "");
//		String tableName = Objects.toString(target.getDpTableName(), "");
//		String dmlSql = Objects.toString(target.getDmlSql(), "");
//		String tableKey = Objects.toString(target.getDpTableKey(), "");
//
//		ConnectionInfo connectionInfoOutput = queryService.getConnectionInfo(outConnId);
//		String dbms = connectionInfoOutput.getDbms();
//
//		if ("U".equalsIgnoreCase(dbOutMode)) {
//			return update(dpSqlField, jsonNode, outConnId, tableName, tableKey.toUpperCase(), dbms, messageId);
//		}
//		if ("I".equalsIgnoreCase(dbOutMode)) {
//			return insert(jsonNode, outConnId, tableName, null, false, dbms, messageId);
//		}
//		if ("T".equalsIgnoreCase(dbOutMode)) {
//			return truncateInsert(jsonNode, outConnId, tableName, null, false, dbms, messageId);
//		}
//		if ("N".equalsIgnoreCase(dbOutMode)) {
//			recreateTable(outConnId, tableName, dmlSql);
//			return insert(jsonNode, outConnId, tableName, null, false, dbms, messageId);
//		}
//
//		String message = "fail due invalid DB_OUTMODE\t" + dbOutMode;
//		PollingService.resultMessage.put(messageId, message);
//		return false;
//	}
//
//	public boolean push(Object jsonOrList, String outConnId, String format) throws Exception {
//		Properties properties = pollingHelper.getProperties(new String(queryService.getConnectionFile(outConnId)));
//		try (
//				AdminClient adminClient = KafkaAdminClient.create(properties);
//				Producer<String, String> producer = new KafkaProducer<>(properties);
//		) {
//			adminClient.listTopics().names().get().parallelStream().filter(s -> s.equals(properties.getProperty("topic"))).findFirst().ifPresent(s -> {
//				String topic = properties.getProperty("topic");
//				pollingHelper.getOutData(jsonOrList, format).parallelStream().forEach(oneLine -> producer.send(new ProducerRecord<String, String>(topic, oneLine)));
//			});
//			return true;
//		} catch (Exception e) {
//			throw e;
//		}
//	}
//
//	public Boolean sendS3(Integer dpOutSeq, String dpID, String dbOutMode, String s3Conn, String outConnId, String dbTableName, List<String> list, String delimeter, String messageId, boolean redshiftInsert) throws IOException {
//		String s3ConnId = null;
//		if (s3Conn != null && !s3Conn.equals("")) {
//			s3ConnId = s3Conn;
//		} else {
//			s3ConnId = outConnId;
//		}
//		S3 s3 = objectMapper.readValue(queryService.getConnectionFile(s3ConnId), S3.class);
//		String dbPollingDir = constants.getDBPollingDir();
//		String loadDttm = new SimpleDateFormat("yyyyMMdd").format(new Date());
//		String fileName = dbTableName + "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".txt";
//		String path = dbPollingDir + dpID + File.separator + loadDttm + File.separator + fileName;
//
//		if (!Files.isDirectory(Paths.get(dbPollingDir + dpID + File.separator + loadDttm))) {
//			Files.createDirectories(Paths.get(dbPollingDir + dpID + File.separator + loadDttm));
//		}
//		if (!Paths.get(path).toFile().exists()) {
//			Files.createFile(Paths.get(path));
//		}
//
//		Files.write(Paths.get(path), list, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//
//		Regions clientRegion = Regions.fromName(s3.getRegion());
//		String bucketName = s3.getBucket();
//
//		AmazonS3 s3Client = null;
//		if (s3.isAwsCredentials()) {
//			AWSCredentials awsCredentials = new BasicAWSCredentials(s3.getAccessKey(), s3.getSecretKey());
//			s3Client = AmazonS3ClientBuilder.standard()
//					.withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
//					.withRegion(clientRegion)
//					.build();
//		} else {
//			s3Client = AmazonS3ClientBuilder.standard()
//					.withRegion(clientRegion)
//					.build();
//		}
//		String bucketPath = bucketName + "/dbpolling/" + dpID + "/" + loadDttm;
//
//		File file = new File(path);
//		PutObjectRequest request = new PutObjectRequest(bucketPath, fileName, file);
//		ObjectMetadata metadata = new ObjectMetadata();
//		metadata.setContentType("plain/text");
//		metadata.setContentLength(file.length());
//		metadata.setContentEncoding("UTF_8");
//		request.setMetadata(metadata);
//		// request.setCannedAcl(CannedAccessControlList.PublicRead);
//		s3Client.putObject(request);
//
//		if (redshiftInsert) {
//			if ("T".equalsIgnoreCase(dbOutMode)) {
//				String truncQuery = nativeQuery.executeQuery(outConnId, "TRUNCATE TABLE " + dbTableName);
//				if (!truncQuery.isEmpty()) {
//					PollingService.resultMessage.put(messageId, truncQuery);
//					return false;
//				}
//			}
//			String command = null;
//			if (s3.getRedshiftCredentials() != null) {
//				if (s3.getRedshiftCredentials().equals("key")) {
//					command = "COPY " + dbTableName + " from 's3://" + bucketPath + "/" + fileName + "' CREDENTIALS 'aws_access_key_id=" + s3.getAccessKey() + ";aws_secret_access_key=" + s3.getSecretKey() + "' CSV DELIMITER '" + delimeter + "'";
//				} else if (s3.getRedshiftCredentials().equals("role")) {
//					command = "COPY " + dbTableName + " from 's3://" + bucketPath + "/" + fileName + "' IAM_ROLE '" + s3.getIamRole() + "' CSV DELIMITER '" + delimeter + "'";
//				} else {
//					command = "COPY " + dbTableName + " from 's3://" + bucketPath + "/" + fileName + "' CSV DELIMITER '" + delimeter + "'";
//				}
//			} else {
//				command = "COPY " + dbTableName + " from 's3://" + bucketPath + "/" + fileName + "' CSV DELIMITER '" + delimeter + "'";
//			}
//
//			String result = nativeQuery.executeQuery(outConnId, command);
//			if (result != null && !result.equals("")) {
//				PollingService.resultMessage.put(messageId, result);
//				return false;
//			}
//		}
//		return true;
//	}
//
//	public String getPwDecode(String pw) {
//		String password = pw;
//		if (encryptPassword) {
//			password = AES256Cipher.AES_Decode(pw);
//		}
//		return password;
//	}
//
//	public boolean rabbitmqSend(Object jsonOrList, String outConnId, String format, String messageId) throws Exception {
//		Rabbitmq rabbitmq = objectMapper.readValue(queryService.getConnectionFile(outConnId), Rabbitmq.class);
//		String destination = rabbitmq.getDestination();
//		String mqQueue = rabbitmq.getQueue();
//		String mqExchangeQueue = rabbitmq.getExchangeQueue();
//
//		if (destination.equalsIgnoreCase("queue")) {
//			ConnectionFactory factory = new ConnectionFactory();
//			String[] mqIpList = rabbitmq.getIp().split(",");
//			Address[] addrList = null;
//			if (mqIpList.length == 2) {
//				addrList = new Address[]{new Address(mqIpList[0], Integer.parseInt(rabbitmq.getPort())), new Address(mqIpList[1], Integer.parseInt(rabbitmq.getPort()))};
//			} else {
//				addrList = new Address[]{new Address(mqIpList[0], Integer.parseInt(rabbitmq.getPort()))};
//			}
//			// factory.setHost(rabbitmq.getIp());
//			// factory.setPort(Integer.parseInt(rabbitmq.getPort()));
//			factory.setUsername(rabbitmq.getId());
//			factory.setPassword(getPwDecode(rabbitmq.getPw()));
//			try (com.rabbitmq.client.Connection connection = factory.newConnection(addrList);
//				 Channel channel = connection.createChannel()) {
//				if (mqQueue != null && !mqQueue.equals("") && mqExchangeQueue != null && !mqExchangeQueue.equals("")) {
//					channel.exchangeDeclare(mqExchangeQueue, "direct", true, false, false, null);
//					channel.queueBind(mqQueue, mqExchangeQueue, "");
//				}
//				AMQP.BasicProperties.Builder props = new AMQP.BasicProperties.Builder().contentType("text/plain").deliveryMode(2);
//				pollingHelper.getOutData(jsonOrList, format).parallelStream().forEachOrdered(oneLine -> {
//					try {
//						channel.basicPublish(mqExchangeQueue, "", props.build(), oneLine.getBytes());
//					} catch (IOException e) {
//						log.error("Exception, ", e);
//					}
//				});
//				return true;
//			} catch (Exception e) {
//				log.error("Exception, ", e);
//				throw e;
//			}
//		} else {
//			PollingService.resultMessage.put(messageId, "rabbitmq destination check failed..");
//			return false;
//		}
//	}
//
//	public boolean insert(JsonNode jsonNode, String connectionId, String tableName, Map<String, String> dataTypes, Boolean isTarget, String dbms, String messageId) {
//		Iterator<JsonNode> iterator = jsonNode.iterator();
//		ArrayNode arrayNode = objectMapper.getNodeFactory().arrayNode();
//		List<String> queries = new ArrayList<>();
//		int counter = 0;
//
//		while (iterator.hasNext()) {
//			arrayNode.add(iterator.next());
//			counter++;
//			if (counter > 0 && counter % 5000 == 0) {
//				queries.add(getStructureInsertQuery(arrayNode, connectionId, tableName, dataTypes, isTarget, dbms, messageId));
//				arrayNode = objectMapper.getNodeFactory().arrayNode();
//			}
//		}
//
//		if (counter > 0 && counter % 5000 != 0) {
//			queries.add(getStructureInsertQuery(arrayNode, connectionId, tableName, dataTypes, isTarget, dbms, messageId));
//		}
//
//		if (!queries.isEmpty()) {
//			String resultValidity = nativeQuery.executeBatchQuery(connectionId, queries);
//			if (isTarget) {
//				PollingService.resultMessage.put(messageId, resultValidity);
//			}
//			return !resultValidity.startsWith("error");
//		}
//		return false;
//	}
//
//	public Boolean truncateInsert(JsonNode jsonNode, String connectionId, String tableName, Map<String, String> dataTypes, Boolean isTarget, String dbms, String messageId) {
//
//		String truncQuery = nativeQuery.executeQuery(connectionId, "TRUNCATE TABLE " + tableName);
//		if (!truncQuery.isEmpty()) {
//			PollingService.resultMessage.put(messageId, truncQuery);
//			return false;
//		}
//		return insert(jsonNode, connectionId, tableName, dataTypes, isTarget, dbms, messageId);
//	}
//
//	public Boolean procedure(String query, String connectionId, String messageId) {
//		String result = nativeQuery.executeQuery(connectionId, query);
//		boolean success = false;
//		if (result.isEmpty()) {
//			success = true;
//		} else {
//			PollingService.resultMessage.put(messageId, "can't call procedure..\t" + result);
//		}
//
//		return success;
//	}
//
//	public String getStructureInsertQuery(JsonNode jsonNode, String connectionId, String tableName, Map<String, String> dataTypes, Boolean isTarget, String dbms, String messageId) {
//		Map<String, Object> columns;
//		if (dataTypes == null) {
//			columns = nativeQuery.getColumns(connectionId, tableName);
//			if (columns.containsKey("datatypes")) {
//				dataTypes = (Map<String, String>) columns.get("datatypes");
//				if (!dataTypes.isEmpty()) {
//					return pollingHelper.getInsertSqlQuery(dataTypes, jsonNode, tableName, dbms);
//				}
//			}
//		} else {
//			return pollingHelper.getInsertSqlQuery(dataTypes, jsonNode, tableName, dbms);
//		}
//		if (!isTarget) {
//			PollingService.resultMessage.put(messageId, "can't get keys and types from the table\t" + tableName + "\t as it maybe missing");
//		}
//		return "";
//	}
//
//	public Boolean update(String dpSqlField, JsonNode jsonNode, String connectionId, String tableName, String dbTableKey, String dbms, String messageId) {
//		Map<String, Object> columns = nativeQuery.getColumns(connectionId, tableName);
//		final String[] type = {""};
//		if (columns.containsKey("datatypes")) {
//			Map<String, String> dataTypes = (Map<String, String>) columns.get("datatypes");
//			if (!dataTypes.isEmpty()) {
//				dataTypes.entrySet().stream().filter(object -> object.getKey().toUpperCase().equals(dbTableKey)).findAny().ifPresent(ob -> type[0] = ob.getValue());
//			}
//			Set<String> valuesToDelete = getValuesOFKeyForDeletion(jsonNode, dbTableKey, type[0]);
//			if (valuesToDelete.isEmpty()) {
//				PollingService.resultMessage.put(messageId, "DB_TABLE_KEY\t" + dbTableKey + "\t doesn't exist in target table\t" + tableName);
//				return false;
//			}
//			JsonNode node = getUniqueMaxRecords(jsonNode, dpSqlField,
//					valuesToDelete.stream().map(s -> s.replace("'", "")).collect(Collectors.toList()), dbTableKey);
//			String resultQuery = nativeQuery.executeQuery(connectionId, "DELETE FROM " + tableName + " WHERE " + dbTableKey + " IN (" + Joiner.on(",").join(valuesToDelete) + ")");
//			PollingService.resultMessage.put(messageId, resultQuery);
//			if (resultQuery.isEmpty()) {
//				return insert(node, connectionId, tableName, dataTypes, false, dbms, messageId);
//			}
//		}
//		return false;
//	}
//
//	private JsonNode getUniqueMaxRecords(JsonNode jsonNode, String primaryKey, List<String> dbTableKeyValues, String dbTableKey) {
//		List list = new ArrayList<>();
//		StreamSupport.stream(jsonNode.spliterator(), true)
//				.collect(Collectors.toCollection(ArrayList::new))
//				.parallelStream()
//				.filter(node -> node.hasNonNull(primaryKey.toUpperCase()) && node.hasNonNull(dbTableKey) && dbTableKeyValues.contains(node.get(dbTableKey).asText()))
//				.collect(Collectors.groupingBy(node -> node.get(dbTableKey).asText()))
//				.forEach((key, value) -> value.parallelStream()
//						.max(Comparator.comparing(jsonNodeObject -> Integer.parseInt(jsonNodeObject.get(primaryKey).toString())))
//						.ifPresent(list::add));
//		return objectMapper.valueToTree(list);
//	}
//
//	private Set<String> getValuesOFKeyForDeletion(JsonNode jsonNode, String key, String type) {
//		Set<String> values = new HashSet<>();
//		StreamSupport.stream(jsonNode.spliterator(), true).forEach(node -> {
//			JSONObject jsonObject = new JSONObject(node.toString());
//			if (jsonObject.has(key)) {
//				values.add(type.contains("char") ? "'" + jsonObject.get(key).toString() + "'" : jsonObject.get(key).toString());
//			}
//		});
//		return values;
//	}
//
//	public void initDBPollingFile(String dbPollingDir, String dpID, String dbTableName, String dpFileRepeat) throws Exception {
//		CronTrigger cronTrigger = null;
//		String format = "yyyyMMdd";
//		if (dpFileRepeat.equalsIgnoreCase("M")) {
//			cronTrigger = new CronTrigger("0 * * ? * *");
//			format = "yyyyMMddHHmm";
//		} else if (dpFileRepeat.equalsIgnoreCase("H")) {
//			cronTrigger = new CronTrigger("0 0 * ? * *");
//			format = "yyyyMMddHH";
//		} else if (dpFileRepeat.equalsIgnoreCase("D")) {
//			cronTrigger = new CronTrigger("0 0 0 * * ?");
//			format = "yyyyMMdd";
//		}
//		if (!latestdpIDTimeValueNMap.containsKey(dpID)) {
//			latestdpIDTimeValueNMap.put(dpID, dpFileRepeat);
//		}
//		if (cronTrigger != null) {
//			String finalFormat = format;
//			//at first time
//			if (!latestOutputFileNameMap.containsKey(dpID)) {
//
//				executeNow(dbPollingDir, dpID, dbTableName, finalFormat, true);
//				latestdpIDScheduleObjectMap.put(dpID, taskScheduler.schedule(() -> {
//					try {
//						executeNow(dbPollingDir, dpID, dbTableName, finalFormat, false);
//					} catch (Exception e) {
//						log.error("Exception, ", e);
//					}
//				}, cronTrigger));
//				latestdpIDNameValueNMap.put(dpID, dbTableName);
//			}
//			//second time
//			else {
//				executeNow(dbPollingDir, dpID, dbTableName, finalFormat, true);
//				//time|name changed
//				if (!latestdpIDTimeValueNMap.get(dpID).equalsIgnoreCase(dpFileRepeat) || !latestdpIDNameValueNMap.get(dpID).equalsIgnoreCase(dbTableName)) {
//					ScheduledFuture oldScheduleFuture = (ScheduledFuture) latestdpIDScheduleObjectMap.get(dpID);
//					oldScheduleFuture.cancel(true);
//					latestdpIDNameValueNMap.put(dpID, dbTableName);
//					latestdpIDTimeValueNMap.put(dpID, dpFileRepeat);
//					latestdpIDScheduleObjectMap.put(dpID, taskScheduler.schedule(() -> {
//						try {
//							executeNow(dbPollingDir, dpID, dbTableName, finalFormat, false);
//						} catch (Exception e) {
//							log.error("Exception, ", e);
//						}
//					}, cronTrigger));
//				}
//			}
//		}
//	}
//
//	private void executeNow(String dbPollingDir, String dpID, String outputName, String format, boolean create) throws Exception {
//		latestOutputFileNameMap.put(dpID, outputName + "_" + new SimpleDateFormat(format).format(new Date()) + ".txt");
//		if (create) {
//			if (!Files.isDirectory(Paths.get(dbPollingDir))) {
//				Files.createDirectory(Paths.get(dbPollingDir));
//			}
//			if (!Paths.get(dbPollingDir + latestOutputFileNameMap.get(dpID)).toFile().exists()) {
//				Files.createFile(Paths.get(dbPollingDir + latestOutputFileNameMap.get(dpID)));
//			}
//		}
//	}
//
//	public boolean saveToFile(Integer dpOutSeq, String dpID, String dbTableName, String dpFileRepeat, List<String> list, String messageId) {
//		try {
//			String dbPollingDir = constants.getDBPollingDir();
//			initDBPollingFile(dbPollingDir, dpID, dbTableName, dpFileRepeat);
//			if (latestOutputFileNameMap.containsKey(dpID)) {
//				long size = Files.size(Paths.get(dbPollingDir + latestOutputFileNameMap.get(dpID)));
//				if (size > 0) {
//					list = list.subList(0, list.size());
//				}
//				Files.write(Paths.get(dbPollingDir + latestOutputFileNameMap.get(dpID)), StringUtils.join(list, "\n").concat("\n").getBytes(),
//						StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//			}
//			return true;
//		} catch (Exception e) {
//			PollingService.resultMessage.put(messageId, e.toString());
//			return false;
//		}
//	}
//
//	public void recreateTable(String connectionId, String tableName, String dmlSql) {
//		nativeQuery.executeQuery(connectionId, "DROP TABLE IF EXISTS " + tableName);
//		nativeQuery.executeQuery(connectionId, dmlSql);
//	}
//
//	public String decimalFormat(int records) {
//		DecimalFormat decFormat = new DecimalFormat("###,###");
//		String str = decFormat.format(records);
//		return str;
//	}
}

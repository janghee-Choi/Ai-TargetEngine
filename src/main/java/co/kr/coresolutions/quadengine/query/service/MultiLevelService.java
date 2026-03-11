package co.kr.coresolutions.quadengine.query.service;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.common.util.SqlUtils;
import co.kr.coresolutions.quadengine.query.model.MultiLevelCategoryRequest;
import co.kr.coresolutions.quadengine.query.model.MultiLevelCategoryResponse;
import co.kr.coresolutions.quadengine.query.model.QueryResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MultiLevelService {

	private final QueryService queryService;
	private final ObjectMapper objectMapper;

	public MultiLevelCategoryResponse selectMlCategory(MultiLevelCategoryRequest multiLevelCategoryRequest,
			String connectionId) {
		String methodType = "POST";
		String partMessage = "{" + connectionId + "}/category, triggered, ";

		String mlName = multiLevelCategoryRequest.getMlName();
		int level = multiLevelCategoryRequest.getLevel();
		String regexYn = multiLevelCategoryRequest.getRegexYn();
		String searchYn = multiLevelCategoryRequest.getSearchYn();
		JsonNode replaceParams = multiLevelCategoryRequest.getReplaceParams();
		log.debug("mlName: {}, level: {}, regexYn: {}", mlName, level, regexYn);

		String sqlList = """
				SELECT ML_QRY_NM, ML_QRY, ML_DBMS_ID, ML_OP, ML_SEQ, ML_QRY_NM
				  FROM T_XLIG_HIERARCHY_LIST MLID,
				       (SELECT DBMS_ID FROM T_XLIG_DIMENSION_LIST WHERE PRMP_KWD = :mlName) MLID2
				 WHERE MLID2.DBMS_ID = MLID.ML_ID
				""";
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("mlName", mlName);
		List<Map<String, Object>> mlQryList = queryService.selectList(connectionId, sqlList, params);
		log.debug("mlQryList : {}", mlQryList);

		if (searchYn.equals("Y")) {
			level = mlQryList.size();
		} else if (level > mlQryList.size() || level <= 0) {
			log.info("Method : {} , Message : {}", methodType, partMessage + "level is invalid : " + level);
			throw new CommonException(ErrorCode.REQUEST_BODY_NOT_VALID, "level is invalid : " + level);
		}

		int finalLevel = level;
		Optional<Map<String, Object>> targetMl = mlQryList.stream().filter(map -> {
			BigDecimal mlSeq = SqlUtils.getBigDecimal(map, "ML_SEQ");
			return mlSeq != null && mlSeq.intValue() == finalLevel;
		}).findFirst();

		if (targetMl.isEmpty()) {
			throw new CommonException(ErrorCode.ML_NOT_FOUND,
					"ml not found (PRMP_KWD: " + mlName + ", ML_SEQ: " + level + ")");
		}

		Map<String, Object> map = targetMl.get();
		String mlQry = Objects.toString(map.get("ML_QRY"), null);
		String mlDbmsId = Objects.toString(map.get("ML_DBMS_ID"), null);
		String mlQryNm = Objects.toString(map.get("ML_QRY_NM"), null);
		String mlOp = Objects.toString(map.get("ML_OP"), null);
		log.info("mlQry : {}: ", mlQry);

		String replacedMlQry = replaceMlCategory(mlQryList, mlQry, replaceParams, regexYn, mlDbmsId);
		log.info("replacedMlQry : {}", replacedMlQry);

		QueryResultResponse result = queryService.selectQueryResult(mlDbmsId, replacedMlQry, null);
		return MultiLevelCategoryResponse.builder().queryResultResponse(result).mlOp(mlOp).mlQryNm(mlQryNm)
				.lastYn(level == mlQryList.size() ? "Y" : "N").searchYn(mlQry.contains("@@SEARCH_WORD@@") ? "Y" : "N")
				.build();
	}

	public Map<String, Object> selectMlSearch(JsonNode jsonNode, String connectionId) {
		String partMessage = "{" + connectionId + "}/search, triggered, ";

		String popupNm = null;
		JsonNode replaceParams = null;
		String regexYn = null;
		String searchYn = null;
		String autorunYn = "Y";
		JsonNode paging = null;
		int sEcho = 0;
		int iDisplayLength = 0;

		for (int i = 0; i < jsonNode.size(); i++) {
			log.debug("jsonNode.get(0)", jsonNode.get(i));
			String name = jsonNode.get(i).get("name").asText();
			JsonNode value = jsonNode.get(i).get("value");
			log.debug("name: {}, value: {}", name, value);
			if (name.equals("popupId")) {
				popupNm = value.asText().replace("'", "''");
			} else if (name.equals("replaceParams")) {
				replaceParams = value;
			} else if (name.equals("regexYn")) {
				regexYn = value.asText().replace("'", "''");
			} else if (name.equals("searchYn")) {
				searchYn = value.asText().replace("'", "''");
			} else if (name.equals("paging")) {
				paging = value;
			} else if (name.equals("iDisplayStart")) {
				sEcho = value.asInt();
			} else if (name.equals("iDisplayLength")) {
				iDisplayLength = value.asInt();
			} else if (name.equals("autorunYn")) {
				autorunYn = value.asText().replace("'", "''");
			}
		}

		log.debug("replaceParams : {}", replaceParams);

		String sqlDimension = """
				SELECT DBMS_ID
				  FROM T_XLIG_DIMENSION_LIST
				 WHERE PRMP_KWD = :popupNm
				""";
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("popupNm", popupNm);
		Map<String, Object> dimension = queryService.selectOne(connectionId, sqlDimension, params);
		log.debug("dimension : {}", dimension);

		if (dimension == null) {
			throw new CommonException(ErrorCode.DIMENSION_NOT_FOUND, "dimension not found (PRMP_KWD: " + popupNm + ")");
		}

		String popupId = Objects.toString(dimension.get("DBMS_ID"), null);

		if ("N".equals(searchYn)) {
			String sqlSearchPopup = """
					SELECT CNDT_NM, SEQ, QRY_DBMS_ID, QRY_META, CNDT_TYPE_CD
					  FROM T_XLIG_SEARCH_POPUP_QRY
					 WHERE POPUP_ID = :popupId
					 ORDER BY SEQ ASC
					""";
			MapSqlParameterSource params2 = new MapSqlParameterSource().addValue("popupId", popupId);
			List<Map<String, Object>> searchPopupList = queryService.selectList(connectionId, sqlSearchPopup, params2);
			int totalCnt = searchPopupList.size();
			log.debug("totalCnt : {}", totalCnt);

			Map<String, Object> resMap = new HashMap<>();
			for (int i = 0; i < totalCnt; i++) {
				Map<String, Object> searchPopup = searchPopupList.get(i);
				String cndtTypeCd = Objects.toString(searchPopup.get("CNDT_TYPE_CD"), null);
				if ("qry".equalsIgnoreCase(cndtTypeCd)) {
					String qryMeta = Objects.toString(searchPopup.get("QRY_META"), null);
					String qryDbmsId = Objects.toString(searchPopup.get("QRY_DBMS_ID"), null);
					String cndtNm = Objects.toString(searchPopup.get("CNDT_NM"), null);
					log.debug("qryMeta : {} | qryDbmsId : {}", qryMeta, qryDbmsId);

					String replacedQryMeta = replaceMlSearch(qryMeta, replaceParams, regexYn, qryDbmsId);
					log.debug("replacedQryMeta : {}", replacedQryMeta);

					List<Map<String, Object>> result = queryService.selectList(qryDbmsId, replacedQryMeta, null);
					log.debug("result : {}", result);

					try {
						String categoryKey = "CATE_" + (i + 1);
						String resultData = objectMapper.writeValueAsString(result);
						resMap.put(categoryKey, Map.of("name", cndtNm, "data", resultData));
					} catch (Exception e) {
						log.error("Failed to serialize result for category {}", cndtNm, e);
						throw new CommonException(ErrorCode.REQUEST_BODY_NOT_VALID, "Failed to serialize result: " + e.getMessage());
					}
				}
			}

			return resMap;
		} else {
			String sqlSearchPopup = """
					SELECT QRY_DBMS_TYPE_ID, QRY_DBMS_ID, QRY_META
					  FROM T_XLIG_SEARCH_POPUP
					 WHERE POPUP_ID = :popupId
					   AND DEL_F = 'N'
					""";
			MapSqlParameterSource params3 = new MapSqlParameterSource().addValue("popupId", popupId);
			Map<String, Object> searchPopup = queryService.selectOne(connectionId, sqlSearchPopup, params3);
			String qryMeta = Objects.toString(searchPopup.get("QRY_META"), null);
			String qryDbmsId = Objects.toString(searchPopup.get("QRY_DBMS_ID"), null);
			String replacedQryMeta = replaceMlSearch(qryMeta, replaceParams, regexYn, qryDbmsId);
			log.info("**replacedQryMeta** : {}", replacedQryMeta);

			String finalSql;
			if (paging != null) {
				String dbms = paging.get("dbms").asText().replace("'", "''");
				log.debug("paging : {}", paging);
				log.debug("dbms : {}", dbms);
				String fileMetaSql = queryService.getFileFromDir(dbms.toUpperCase() + ".txt",
						"connection_info" + File.separator + "paging_query");
				if (fileMetaSql.isEmpty()) {
					log.info("Method : {} , Message : {}", "POST",
							partMessage + "fail due file connection_info/paging_query/" + dbms.toUpperCase()
									+ ".txt doesn't exist or empty");
					throw new CommonException(ErrorCode.FILE_NOT_FOUND, "fail due file connection_info/paging_query/"
							+ dbms.toUpperCase() + ".txt doesn't exist or empty");
				}

				finalSql = fileMetaSql.replace("{SQL}", replacedQryMeta).replace("{STARTROW}", String.valueOf(sEcho))
						.replace("{ENDROW}", String.valueOf(sEcho + iDisplayLength))
						.replace("{NUMROWS}", String.valueOf(iDisplayLength));
			} else {
				finalSql = replacedQryMeta;
			}
			log.info("**finalSql** : {}", finalSql);

			QueryResultResponse result = queryService.selectQueryResult(qryDbmsId, finalSql, null);
			log.debug("result : {}", result);

			Map<String, Object> resMap = new HashMap<>();
			resMap.put("Input", result.getInput());
			resMap.put("Fields", result.getMeta());

			if ("Y".equals(autorunYn)) {
				String sqlTotal = """
						SELECT COUNT(*) CNT FROM ( %s ) T
						""".formatted(replacedQryMeta);
				;
				Map<String, Object> totalMap = queryService.selectOne(qryDbmsId, sqlTotal, null);
				log.debug("totalMap : {}", totalMap);

				BigDecimal cnt = SqlUtils.getBigDecimal(totalMap, "CNT");
				int totalCnt = cnt != null ? cnt.intValue() : 0;
				int offset = sEcho + 1;
				resMap.put("recordsFiltered", totalCnt);
				resMap.put("records", totalCnt);
				resMap.put("total", (int) Math.ceil((double) totalCnt / (double) iDisplayLength));
				resMap.put("page", (int) Math.ceil((double) offset / (double) iDisplayLength));
				resMap.put("data", result.getInput());
			} else {
				resMap.put("recordsFiltered", 0);
				resMap.put("records", 0);
				resMap.put("total", 0);
				resMap.put("page", 0);
				resMap.put("data", Collections.emptyList());
			}

			return resMap;
		}
	}

	private String getMlValueForKey(List<Map<String, Object>> mlQryList, String key) {
		Optional<Map<String, Object>> targetMl = mlQryList.stream().filter(map -> {
			String mlQryNm = Objects.toString(map.get("ML_QRY_NM"), null);
			return mlQryNm != null && (key.equals("@@" + mlQryNm + "@@") || key.equals("$$" + mlQryNm + "$$"));
		}).findFirst();

		if (targetMl.isEmpty()) {
			return "";
		}

		return Objects.toString(targetMl.get().get("ML_OP"), "");
	}

	private String replaceMlCategory(List<Map<String, Object>> mlQryList, String mlQuery, JsonNode replaceParams,
			String regexYn, String dbms) {
		StringBuilder sb1 = new StringBuilder();
		Pattern pattern1 = Pattern.compile("\\[([^\\[]*(@@|\\$\\$|%%)[^\\]]*(@@|\\$\\$|%%)[^\\[]*)\\]");

		Matcher matcher1 = pattern1.matcher(mlQuery);
		if (mlQuery.contains("@@") || mlQuery.contains("$$") || mlQuery.contains("%%")) {
			while (matcher1.find()) {
				String insideBrackets = matcher1.group(1);
				Pattern pattern2 = Pattern.compile("(@@|\\$\\$|%%)[\\w\\d가-힣\\(\\)]+(@@|\\$\\$|%%)");
				Matcher matcher2 = pattern2.matcher(insideBrackets);
				matcher2.find();
				String key = matcher2.group(0);
				log.debug("key : {}", key);

				if (replaceParams.has(key)) {
					if (!replaceParams.get(key).asText().equals("")) {
						if (key.equals("@@SEARCH_WORD@@")) {
							String op = "";
							if (dbms.toUpperCase().equals("REDSHIFT")) {
								op = regexYn.equals("Y") ? "SIMILAR TO" : "ILIKE";
							} else {
								op = regexYn.equals("Y") ? "REGEXP" : "LIKE";
							}
							insideBrackets = insideBrackets.replaceAll("::OP::|::op::|::oP::|::Op::", op);
						} else {
							insideBrackets = insideBrackets.replaceAll("::OP::|::op::|::oP::|::Op::",
									getMlValueForKey(mlQryList, key));
						}

						if (insideBrackets.contains("@@")) {
							if (insideBrackets.toUpperCase().contains("IN")) {
								String[] ar = replaceParams.get(key).asText().split(",");
								insideBrackets = insideBrackets.replace(key,
										"(" + Arrays.stream(ar).map(v -> "'" + v + "'").collect(Collectors.joining(","))
												+ ")");
							} else if (insideBrackets.toUpperCase().contains("=")) {
								insideBrackets = insideBrackets.replace(key,
										"'" + replaceParams.get(key).asText() + "'");
							} else if (insideBrackets.toUpperCase().contains("LIKE")
									|| insideBrackets.toUpperCase().contains("ILIKE")) {
								insideBrackets = insideBrackets.replace(key,
										"'%" + replaceParams.get(key).asText() + "%'");
							} else if (insideBrackets.toUpperCase().contains("REGEXP")) {
								insideBrackets = insideBrackets.replace(key,
										"'" + replaceParams.get(key).asText() + "'");
							} else if (insideBrackets.toUpperCase().contains("SIMILAR TO")) {
								insideBrackets = insideBrackets.replace(key,
										"'" + replaceParams.get(key).asText() + "'");
							} else if (insideBrackets.toUpperCase().contains("BETWEEN")) {
								String value = replaceParams.get(key).asText();
								String[] valueArray = value.split(",");
								insideBrackets = insideBrackets.replace(key,
										"'" + valueArray[0] + "'" + " AND " + "'" + valueArray[1] + "'");
							}
						} else if (insideBrackets.contains("$$") || insideBrackets.contains("%%")) {
							if (insideBrackets.toUpperCase().contains("IN")) {
								String[] ar = replaceParams.get(key).asText().split(",");
								insideBrackets = insideBrackets.replace(key,
										"(" + Arrays.stream(ar).map(v -> v).collect(Collectors.joining(",")) + ")");
							} else if (insideBrackets.toUpperCase().contains("=")) {
								insideBrackets = insideBrackets.replace(key, replaceParams.get(key).asText());
							} else if (insideBrackets.toUpperCase().contains("LIKE")
									|| insideBrackets.toUpperCase().contains("ILIKE")) {
								insideBrackets = insideBrackets.replace(key,
										"'%" + replaceParams.get(key).asText() + "%'");
							} else if (insideBrackets.toUpperCase().contains("REGEXP")) {
								insideBrackets = insideBrackets.replace(key, replaceParams.get(key).asText());
							} else if (insideBrackets.toUpperCase().contains("SIMILAR TO")) {
								insideBrackets = insideBrackets.replace(key,
										"'" + replaceParams.get(key).asText() + "'");
							} else if (insideBrackets.toUpperCase().contains("BETWEEN")) {
								String value = replaceParams.get(key).asText();
								String[] valueArray = value.split(",");
								insideBrackets = insideBrackets.replace(key,
										"'" + valueArray[0] + "'" + " AND " + "'" + valueArray[1] + "'");
							}
						}
						log.debug("insideBrackets : {}", insideBrackets);
					} else {
						insideBrackets = insideBrackets.replace(insideBrackets, "");
					}
				} else {
					insideBrackets = insideBrackets.replace(insideBrackets, "");
				}
				matcher1.appendReplacement(sb1, insideBrackets);
			}
			matcher1.appendTail(sb1);

			mlQuery = sb1.toString();
		}

		StringBuilder sb2 = new StringBuilder();
		Pattern pattern2 = Pattern.compile("(@@|\\$\\$|%%)[\\w\\d가-힣\\(\\)]+(@@|\\$\\$|%%)");
		Matcher matcher2 = pattern2.matcher(mlQuery);
		if (mlQuery.contains("@@") || mlQuery.contains("$$") || mlQuery.contains("%%")) {
			while (matcher2.find()) {
				String insideBrackets = matcher2.group(0);
				if (replaceParams.has(insideBrackets)) {
					if (insideBrackets.contains("@@")) {
						insideBrackets = insideBrackets.replace(insideBrackets,
								"'" + replaceParams.get(insideBrackets).asText() + "'");
					} else if (insideBrackets.contains("$$") || insideBrackets.contains("%%")) {
						insideBrackets = insideBrackets.replace(insideBrackets,
								replaceParams.get(insideBrackets).asText());
					}
					matcher2.appendReplacement(sb2, insideBrackets);
				}
			}
			matcher2.appendTail(sb2);
			return sb2.toString();
		} else {
			return mlQuery;
		}
	}

	private String replaceMlSearch(String query, JsonNode replaceParams, String regexYn, String dbms) {
		StringBuffer sb1 = new StringBuffer();
		Pattern pattern1 = Pattern.compile("\\[([^\\[]*(@@|\\$\\$|%%)[^\\]]*(@@|\\$\\$|%%)[^\\[]*)\\]");
		Matcher matcher1 = pattern1.matcher(query);
		if (query.contains("@@") || query.contains("$$") || query.contains("%%")) {
			while (matcher1.find()) {
				String insideBrackets = matcher1.group(1);
				Pattern pattern2 = Pattern.compile("(@@|\\$\\$|%%)[\\(\\)\\w\\d가-힣]+(@@|\\$\\$|%%)");
				Matcher matcher2 = pattern2.matcher(insideBrackets);
				matcher2.find();
				String key = matcher2.group(0);
				log.debug("key : {}", key);

				if (replaceParams != null && replaceParams.has(key)) {
					log.debug("replaceParams key : {}", replaceParams.get(key));

					if (!"".equals(replaceParams.get(key).asText())) {
						if (key.equals("@@SEARCH_WORD@@")) {
							String op = "LIKE";
							if (regexYn.equals("Y")) {
								switch (dbms.toUpperCase()) {
								case "MARIADB":
								case "MYSQL":
									op = "REGEXP";
									break;
								case "REDSHIFT":
									op = "SIMILAR TO";
									break;
								default:
									break;
								}
							} else {
								if ("REDSHIFT".equals(dbms.toUpperCase())) {
									op = "ILIKE";
								}
							}
							insideBrackets = insideBrackets.replaceAll("::OP::|::op::|::oP::|::Op::", op);
						} else {
							insideBrackets = insideBrackets.replaceAll("::OP::|::op::|::oP::|::Op::", "IN");
						}
						if (insideBrackets.contains("@@")) {
							if (insideBrackets.toUpperCase().contains(" IN ")) {
								String[] ar = replaceParams.get(key).asText().split(",");
								insideBrackets = insideBrackets.replace(key,
										"(" + Arrays.stream(ar).map(v -> "'" + v + "'").collect(Collectors.joining(","))
												+ ")");
							} else if (insideBrackets.toUpperCase().contains("=")) {
								insideBrackets = insideBrackets.replace(key,
										"'" + replaceParams.get(key).asText() + "'");
							} else if (insideBrackets.toUpperCase().contains(" LIKE ")
									|| insideBrackets.toUpperCase().contains("ILIKE")) {
								insideBrackets = insideBrackets.replace(key,
										"'%" + replaceParams.get(key).asText() + "%'");
							} else if (insideBrackets.toUpperCase().contains(" REGEXP ")) {
								insideBrackets = insideBrackets.replace(key,
										"'" + replaceParams.get(key).asText() + "'");
							} else if (insideBrackets.toUpperCase().contains(" SIMILAR TO ")) {
								insideBrackets = insideBrackets.replace(key,
										"'" + replaceParams.get(key).asText() + "'");
							} else if (insideBrackets.toUpperCase().contains(" BETWEEN ")) {
								String value = replaceParams.get(key).asText();
								String[] valueArray = value.split(",");
								insideBrackets = insideBrackets.replace(key,
										"'" + valueArray[0] + "'" + " AND " + "'" + valueArray[1] + "'");
							}
						} else if (insideBrackets.contains("$$") || insideBrackets.contains("%%")) {
							if (insideBrackets.toUpperCase().contains(" IN ")) {
								String[] ar = replaceParams.get(key).asText().split(",");
								insideBrackets = insideBrackets.replace(key,
										"(" + Arrays.stream(ar).map(v -> v).collect(Collectors.joining(",")) + ")");
							} else if (insideBrackets.toUpperCase().contains("=")) {
								insideBrackets = insideBrackets.replace(key, replaceParams.get(key).asText());
							} else if (insideBrackets.toUpperCase().contains(" LIKE ")
									|| insideBrackets.toUpperCase().contains("ILIKE")) {
								insideBrackets = insideBrackets.replace(key,
										"'%" + replaceParams.get(key).asText() + "%'");
							} else if (insideBrackets.toUpperCase().contains(" REGEXP ")) {
								insideBrackets = insideBrackets.replace(key, replaceParams.get(key).asText());
							} else if (insideBrackets.toUpperCase().contains(" SIMILAR TO ")) {
								insideBrackets = insideBrackets.replace(key,
										"'" + replaceParams.get(key).asText() + "'");
							} else if (insideBrackets.toUpperCase().contains(" BETWEEN ")) {
								String value = replaceParams.get(key).asText();
								String[] valueArray = value.split(",");
								insideBrackets = insideBrackets.replace(key,
										"'" + valueArray[0] + "'" + " AND " + "'" + valueArray[1] + "'");
							}
						}
						log.debug("insideBrackets : {}", insideBrackets);
					} else {
						insideBrackets = insideBrackets.replace(insideBrackets, "");
					}
				} else {
					insideBrackets = insideBrackets.replace(insideBrackets, "");
				}
				matcher1.appendReplacement(sb1, insideBrackets);
			}
			matcher1.appendTail(sb1);

			query = sb1.toString();
		}

		StringBuffer sb2 = new StringBuffer();
		Pattern pattern2 = Pattern.compile("(@@|\\$\\$|%%)[\\(\\)\\w\\d가-힣]+(@@|\\$\\$|%%)");
		Matcher matcher2 = pattern2.matcher(query);

		if (replaceParams != null) {
			if (query.contains("@@") || query.contains("$$") || query.contains("%%")) {
				while (matcher2.find()) {
					String insideBrackets = matcher2.group(0);
					if (replaceParams.has(insideBrackets)) {
						if (insideBrackets.contains("@@")) {
							insideBrackets = insideBrackets.replace(insideBrackets,
									"'" + replaceParams.get(insideBrackets).asText() + "'");
						} else if (insideBrackets.contains("$$") || insideBrackets.contains("%%")) {
							insideBrackets = insideBrackets.replace(insideBrackets,
									replaceParams.get(insideBrackets).asText());
						}
						matcher2.appendReplacement(sb2, insideBrackets);
					}
				}
				matcher2.appendTail(sb2);
				return sb2.toString();
			} else {
				return query;
			}
		} else {
			return query;
		}
	}

}

package co.kr.coresolutions.quadengine.querybi.sqlMaker;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class SqlMaker {

    protected int seq;
    protected String fieldName;

    // --- 테이블 및 필드 Alias 생성 로직 ---

    /** 이전 단계 테이블 별칭 (예: t1) */
    public String tableName1() {
        return "t" + (seq - 1);
    }

    /** 이전 단계 조인 필드 (예: t1.TARGET_ID) */
    public String fieldName1() {
        return tableName1() + "." + fieldName;
    }

    /** 현재 단계 테이블 별칭 (예: t2) */
    public String tableName2() {
        return "t" + seq;
    }

    /** 현재 단계 조인 필드 (예: t2.TARGET_ID) */
    public String fieldName2() {
        return tableName2() + "." + fieldName;
    }

    // --- 추상 메서드 정의 ---

    /** 전체 SQL 조립 실행 */
    public abstract String makeSql();

    /** 첫 번째 쿼리 블록 생성 (WITH 절 시작) */
    public abstract String startSql();

    /** 중간 단계 쿼리 블록 생성 (WITH 절 연결) */
    public abstract String middleSql();

    /** 마지막 단계 쿼리 블록 생성 (최종 SELECT) */
    public abstract String endSql();
}
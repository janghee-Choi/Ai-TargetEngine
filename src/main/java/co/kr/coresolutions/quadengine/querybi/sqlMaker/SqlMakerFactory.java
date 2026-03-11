package co.kr.coresolutions.quadengine.querybi.sqlMaker;

import co.kr.coresolutions.quadengine.querybi.sqlMaker.SqlMakerDifference;
import co.kr.coresolutions.quadengine.querybi.sqlMaker.SqlMakerIntersection;
import co.kr.coresolutions.quadengine.querybi.sqlMaker.SqlMakerUnion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SqlMakerFactory {

    // 멀티스레드 세이프한 싱글톤 인스턴스
    private static final SqlMakerFactory INSTANCE = new SqlMakerFactory();

    public static SqlMakerFactory getInstance() {
        return INSTANCE;
    }

    /**
     * 비즈니스 로직에 맞는 SqlMaker 구현체를 생성하여 반환합니다.
     * 
     * @param joinId    조인 방식 코드 (0,1: 교집합, 2: 차집합, 3: 합집합)
     * @param fieldName 조인 기준 필드명
     * @param index     현재 쿼리 순번 (seq)
     * @param query     치환 완료된 서브쿼리 문장
     * @param startF    시작 쿼리 여부 (WITH 절 시작)
     * @param lastF     마지막 쿼리 여부 (최종 SELECT)
     * @return 각 타입에 맞는 SqlMaker 구현체
     */
    public SqlMaker create(int joinId, String fieldName, int index, String query, boolean startF, boolean lastF) {
        // Java 21 패턴 매칭 및 Switch Expression 활용
        return switch (getSqlMakerType(joinId)) {
        case 1 -> new SqlMakerIntersection(fieldName, index, query, startF, lastF); // Inner Join (교집합)
        case 2 -> new SqlMakerDifference(fieldName, index, query, startF, lastF); // Left Join Is Null (차집합)
        case 3 -> new SqlMakerUnion(fieldName, index, query, startF, lastF); // Union (합집합)
        default -> throw new IllegalArgumentException("지원하지 않는 Join ID입니다: " + joinId);
        };
    }

    /**
     * 입력받은 joinId를 내부 처리용 타입 코드로 변환합니다.
     */
    private int getSqlMakerType(int joinId) {
        return switch (joinId) {
        case 0, 1 -> 1; // 기본 및 교집합
        case 2 -> 2; // 차집합
        case 3 -> 3; // 합집합
        default -> 0; // 예외 상황
        };
    }
}

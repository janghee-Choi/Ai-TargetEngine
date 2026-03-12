package co.kr.coresolutions.quadengine.querybi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 데이터가 없는 필드는 JSON 응답에서 제외
public class SetOperationDto {

    /**
     * 연산 타입: UNION, DIFFERENCE, INTERSECTION, SINGLE 등
     */
    private String type;

    /**
     * UNION 또는 SINGLE 타입일 때 사용하는 피연산자 리스트 요소는 String(Audience ID) 또는
     * SetOperationDto(중첩 연산)일 수 있음
     */
    private List<Object> operands;

    /**
     * DIFFERENCE, INTERSECTION 등 이항 연산 시 왼쪽 피연산자 String(Audience ID) 또는
     * SetOperationDto(중첩 연산)
     */
    private Object left;

    /**
     * DIFFERENCE, INTERSECTION 등 이항 연산 시 오른쪽 피연산자
     */
    private Object right;
}
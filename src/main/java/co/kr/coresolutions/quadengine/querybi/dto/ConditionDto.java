package co.kr.coresolutions.quadengine.querybi.dto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ConditionDto {
    private String field;
    private String operator;
    private JsonNode value;  // Can be array or primitive value

    public ConditionDto() {
    }

    public ConditionDto(String field, String operator, JsonNode value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

}

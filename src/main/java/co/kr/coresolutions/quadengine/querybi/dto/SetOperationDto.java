package co.kr.coresolutions.quadengine.querybi.dto;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetOperationDto {
    private String type;
    private List<String> operands;

    public SetOperationDto() {
    }

    public SetOperationDto(String type, List<String> operands) {
        this.type = type;
        this.operands = operands;
    }

}

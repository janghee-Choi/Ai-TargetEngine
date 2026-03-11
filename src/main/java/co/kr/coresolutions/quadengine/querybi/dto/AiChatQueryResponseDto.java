package co.kr.coresolutions.quadengine.querybi.dto;
import java.util.List;

import co.kr.coresolutions.quadengine.querybi.dto.AudienceDto;
import co.kr.coresolutions.quadengine.querybi.dto.SetOperationDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatQueryResponseDto {
    private boolean success;
    private String message;
    private String count;
    private String query;
    private AudienceDto audiences;
    private SetOperationDto setOperation;

}

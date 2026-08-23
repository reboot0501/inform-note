package io.nexcope.inform_note.facade.api.feature.maintenance.command;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.log.entity.vo.WorkStatus;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

@Slf4j
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeWorkStatusCommand implements JsonSerializable {
    //
    private WorkStatus workStatus;
    private int userId;

    public void validate() {
        //
        Assert.notNull(workStatus, "'workStatus' is required");
        Assert.isTrue(userId > 0, "'empNo' is required");
    }

    @Override
    public String toString() {
        //
        return toJson();
    }

    public static ChangeWorkStatusCommand fromJson(String json) {
        //
        return JsonUtil.fromJson(json, ChangeWorkStatusCommand.class);
    }

    public static ChangeWorkStatusCommand sample() {
        //
        return ChangeWorkStatusCommand.builder()
                .workStatus(WorkStatus.IN_PROGRESS)
                .userId(121)
                .build();
    }

    public static void main(String[] args) {
        //
        log.info(sample().toPrettyJson());
    }
}

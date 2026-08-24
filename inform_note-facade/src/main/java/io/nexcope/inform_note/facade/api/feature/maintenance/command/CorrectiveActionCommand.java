package io.nexcope.inform_note.facade.api.feature.maintenance.command;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.card.entity.vo.AssignedTechnician;
import io.nexcope.inform_note.domain.card.entity.vo.PartReplacement;
import io.nexcope.inform_note.domain.card.entity.vo.WorkStatus;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.List;

@Slf4j
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CorrectiveActionCommand implements JsonSerializable {
    //
    private String downEventId;
    private AssignedTechnician assignedTechnician;
    private List<PartReplacement> partReplacements;
    private String contentHtml;
    private WorkStatus workStatus;

    public void validate() {
        //
        Assert.hasText(downEventId, "'downEventId' is required");
        Assert.notNull(assignedTechnician, "'assignedTechnician' is required");
        Assert.notNull(workStatus, "'workStatus' is required");
    }

    @Override
    public String toString() {
        //
        return toJson();
    }

    public static CorrectiveActionCommand fromJson(String json) {
        //
        return JsonUtil.fromJson(json, CorrectiveActionCommand.class);
    }
}

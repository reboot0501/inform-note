package io.nexcope.inform_note.facade.api.feature.maintenance.command;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

@Slf4j
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AssignTechnicianCommand implements JsonSerializable {
    //
    private String userId;

    public void validate() {
        //
        Assert.hasText(userId, "'empNo' is required");
    }

    @Override
    public String toString() {
        //
        return toJson();
    }

    public static AssignTechnicianCommand fromJson(String json) {
        //
        return JsonUtil.fromJson(json, AssignTechnicianCommand.class);
    }

    public static AssignTechnicianCommand sample() {
        //
        return new AssignTechnicianCommand("1234");
    }

    public static void main(String[] args) {
        //
        log.info(sample().toPrettyJson());
    }
}

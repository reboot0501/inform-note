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
public class CompleteActionCommand implements JsonSerializable {
    //
    private String userId;

    public void validate() {
        //
        Assert.hasText(userId, "'userId' is required");
    }

    @Override
    public String toString() {
        //
        return toJson();
    }

    public static CompleteActionCommand fromJson(String json) {
        //
        return JsonUtil.fromJson(json, CompleteActionCommand.class);
    }

    public static CompleteActionCommand sample() {
        //
        return new CompleteActionCommand("123");
    }

    public static void main(String[] args) {
        //
        log.info(sample().toPrettyJson());
    }
}

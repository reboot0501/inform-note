package io.nexcope.inform_note.facade.api.feature.maintenance.fetch;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

@Getter
@Setter
@AllArgsConstructor
public class FindDownContentFetch implements JsonSerializable {
    //
    private String downEventId;

    public void validate() {
        //
        Assert.hasText(downEventId, "'downEventId' is required");
    }

    @Override
    public String toString() {
        //
        return toJson();
    }

    public static FindDownContentFetch fromJson(String json) {
        //
        return JsonUtil.fromJson(json, FindDownContentFetch.class);
    }
}

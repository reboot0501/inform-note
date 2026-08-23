package io.nexcope.inform_note.facade.api.feature.maintenance.fetch;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.log.entity.vo.DownEventLogSearchCriteria;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.Assert;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FindDownEventsFetch implements JsonSerializable {
    //
    private DownEventLogSearchCriteria criteria;

    public void validate() {
        //
        Assert.notNull(criteria, "'criteria' is required");
    }

    @Override
    public String toString() {
        //
        return toJson();
    }

    public static FindDownEventsFetch fromJson(String json) {
        //
        return JsonUtil.fromJson(json, FindDownEventsFetch.class);
    }
}

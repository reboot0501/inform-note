package io.nexcope.inform_note.feature.maintenance.task;

import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.log.entity.vo.AssignedTechnician;
import io.nexcope.inform_note.domain.log.entity.vo.PartReplacement;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MaintenanceUtils {
    //
    public static boolean isEqualAssignTechnician(AssignedTechnician newTechnician, AssignedTechnician oldTechnician) {
        //
        return ! JsonUtil.toJson(newTechnician).equals(JsonUtil.toJson(oldTechnician));
    }

    public static boolean isEqualPartReplacements(List<PartReplacement> newList, List<PartReplacement> oldList) {
        //
        return ! JsonUtil.toJson(newList).equals(JsonUtil.toJson(oldList));
    }
}

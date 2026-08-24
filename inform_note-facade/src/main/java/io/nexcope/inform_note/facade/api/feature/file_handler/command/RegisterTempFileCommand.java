package io.nexcope.inform_note.facade.api.feature.file_handler.command;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.file.entity.vo.FileRefType;
import io.nexcope.inform_note.domain.card.entity.vo.AssignedTechnician;
import lombok.*;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterTempFileCommand implements JsonSerializable {

    // [1] 업로드 파일 바이너리 (필수)
    private MultipartFile file;

    // [2] 파일 업무 구분 (기본값: DOWN_ATTACHMENT)
    @Builder.Default
    private FileRefType refType = FileRefType.DOWN_ATTACHMENT;

    // [3] assignedTechnician.getEmpno
    private AssignedTechnician assignedTechnician;

    // [4] 연관 마스터 ID (신규 작성 시 null, 기존 글 수정 시 downEventId 전달 가능)
    private String refId;

    public void validate() {
        //
        Assert.notNull(file, "'file' is required and must not be empty.");
        Assert.notNull(refType, "'refType' is required and must not be empty.");
        Assert.notNull(assignedTechnician, "'assignedTechnician' is required and must not be empty.");
    }

    @Override
    public String toString() {
        return toJson();
    }

    public static RegisterTempFileCommand fromJson(String json) {
        return JsonUtil.fromJson(json, RegisterTempFileCommand.class);
    }
}

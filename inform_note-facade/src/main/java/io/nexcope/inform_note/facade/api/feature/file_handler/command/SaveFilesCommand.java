package io.nexcope.inform_note.facade.api.feature.file_handler.command;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.file.entity.vo.FileRefType;
import io.nexcope.inform_note.domain.card.entity.vo.AssignedTechnician;
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
public class SaveFilesCommand implements JsonSerializable {
    // [1] 연관 마스터 ID (downEventId - 필수)
    private String refId; // downEventId
    // 기본값을 일반 첨부파일(DOWN_ATTACHMENT)로 설정
    @Builder.Default
    private FileRefType refType = FileRefType.DOWN_ATTACHMENT;
    // [3] 최종 작성/수정된 HTML 본문 (필수: 본문 파싱으로 fileId 자동 추출)
    private String contentHtml;
    // [4] (선택) 클라이언트에서 명시적으로 넘기는 fileId 목록 (없어도 contentHtml에서 자동 파싱 가능)
    private List<String> fileIds;
    // [5] assignedTechnician.getEmpno
    private AssignedTechnician assignedTechnician;


    public void validate() {
        //
        Assert.hasText(refId, "'refId' is required");
        Assert.notNull(refType, "'refType' is required");
        Assert.hasText(contentHtml, "'contentHtml' is required");
        Assert.notNull(assignedTechnician, "'assignedTechnician' is required");
    }

    @Override
    public String toString() {
        //
        return toJson();
    }

    public static SaveFilesCommand fromJson(String json) {
        //
        return JsonUtil.fromJson(json, SaveFilesCommand.class);
    }

}

package io.nexcope.inform_note.domain.file.entity.dto;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.file.entity.AttachedFile;
import io.nexcope.inform_note.domain.file.entity.vo.FileRefType;
import io.nexcope.inform_note.domain.file.entity.vo.FileStatus;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachedFileDto implements JsonSerializable {
    //
    private FileRefType refType;
    private String refId;
    private String originFileName;
//    private String storedFileName;
//    private String filePath;
    private long fileSize;
    private String contentType;
    private FileStatus status;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    public String genId() {
        //
        return AttachedFile.genId();
    }

    public static AttachedFileDto fromJson(String json) {
        //
        return JsonUtil.fromJson(json, AttachedFileDto.class);
    }
}

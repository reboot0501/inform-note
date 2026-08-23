package io.nexcope.inform_note.domain.file.entity;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.base.util.uuid.UUIDv7;
import io.nexcope.inform_note.domain.file.entity.dto.AttachedFileDto;
import io.nexcope.inform_note.domain.file.entity.vo.FileRefType;
import io.nexcope.inform_note.domain.file.entity.vo.FileStatus;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // MyBatis 매핑용 기본 생성자
@AllArgsConstructor
@Builder
public class AttachedFile implements JsonSerializable {
    //
    private String fileId;
    private FileRefType refType;
    private String refId;
    private String originFileName;
    private String storedFileName;
    private String filePath;
    private long fileSize;
    private String contentType;
    private FileStatus status;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    public AttachedFile(AttachedFileDto dto) {
        this.fileId = dto.genId();
        BeanUtils.copyProperties(dto, this);
        this.status = (this.status != null) ? this.status : FileStatus.TEMP;
        this.createdAt = (dto.getCreatedAt() != null) ? dto.getCreatedAt() : OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static String genId() {
        //
        return UUIDv7.random();
    }

    public static AttachedFile fromJson(String json) {
        //
        return JsonUtil.fromJson(json, AttachedFile.class);
    }

    // =================================================================
    // [비즈니스 메서드 (도메인 행위)]
    // =================================================================

    /**
     * 파일 상태를 확정(CONFIRMED)으로 변경하고 연관 ID 매핑
     */
    public void confirm(FileRefType refType, String refId, String modifierId) {
        this.refType = refType;
        this.refId = refId;
        this.status = FileStatus.SAVED;
        this.updatedBy = modifierId;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * 파일 상태를 삭제(DELETED)로 변경
     */
    public void markAsDeleted(String modifierId) {
        this.status = FileStatus.DELETED;
        this.updatedBy = modifierId;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * 파일 오브젝트를 서버 업로드 후 업로드 정보
     */
    public void completeFileUpload(String storedFileName, String filePath) {
        //
        this.storedFileName = storedFileName;
        this.filePath = filePath;
    }
}

package io.nexcope.inform_note.domain.content.entity;

import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.content.entity.dto.DownContentDto;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // MyBatis 매핑용 기본 생성자
@AllArgsConstructor
@Builder
public class DownContent {
    //
    private String downEventId;
    private String contentHtml;    // Froala 에디터 HTML 본문 (Oracle CLOB 매핑)
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    public DownContent(DownContentDto dto) {
        //
        BeanUtils.copyProperties(dto, this);
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static DownContent fromJson(String json) {
        //
        return JsonUtil.fromJson(json, DownContent.class);
    }

    // =================================================================
    // [비즈니스 메서드 (도메인 행위)]
    // =================================================================

    /**
     * 본문 내용 수정
     */
    public void fromNewContent(String newHtml, String modifierId) {
        this.contentHtml = (newHtml != null) ? newHtml : "";
        this.updatedBy = modifierId;
    }

    public void fromNewContent(String newHtml) {
        this.contentHtml = (newHtml != null) ? newHtml : "";
    }

    public boolean hasContent() {
        return this.contentHtml != null && !this.contentHtml.trim().isEmpty();
    }
}

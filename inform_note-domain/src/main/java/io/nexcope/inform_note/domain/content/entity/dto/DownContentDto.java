package io.nexcope.inform_note.domain.content.entity.dto;

import io.nexcope.inform_note.base.util.json.JsonUtil;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DownContentDto {
    //
    private String downEventId;
    private String contentHtml; // Froala 에디터 HTML 본문 (Oracle CLOB 매핑)

    public static DownContentDto fromJson(String json) {
        //
        return JsonUtil.fromJson(json, DownContentDto.class);
    }
}

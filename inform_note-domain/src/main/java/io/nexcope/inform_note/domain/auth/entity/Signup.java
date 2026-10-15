package io.nexcope.inform_note.domain.auth.entity;

import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.base.util.uuid.UUIDv7;
import io.nexcope.inform_note.domain.auth.entity.dto.SignupDto;
import io.nexcope.inform_note.domain.auth.entity.vo.SignupStatus;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // MyBatis 리플렉션 매핑을 위한 기본 생성자
@AllArgsConstructor
@Builder
public class Signup {
    //
    private String signupId;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private SignupStatus signupStatus;
    // [감사 메타]
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    public Signup(SignupDto dto) {
        //
        this.signupId = UUIDv7.random();
        BeanUtils.copyProperties(dto, this);
        this.signupStatus = SignupStatus.PENDING;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static Signup fromJson (String json) {
        //
        return JsonUtil.fromJson(json, Signup.class);
    }
}

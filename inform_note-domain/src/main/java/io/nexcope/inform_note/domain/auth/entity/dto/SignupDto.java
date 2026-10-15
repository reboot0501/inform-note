package io.nexcope.inform_note.domain.auth.entity.dto;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignupDto implements JsonSerializable {
    //
    private String email;
    private String firstName;
    private String lastName;
    private String password;

    public static SignupDto fromJson(String json) {
        //
        return SignupDto.builder()
                .build();
    }
}

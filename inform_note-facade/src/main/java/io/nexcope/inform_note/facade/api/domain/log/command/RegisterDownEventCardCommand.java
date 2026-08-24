package io.nexcope.inform_note.facade.api.domain.log.command;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.card.entity.dto.DownEventCardDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

@Slf4j
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDownEventCardCommand implements JsonSerializable {
    //
    private DownEventCardDto downEventCardDto;

    public void validate() {
        //
        Assert.notNull(downEventCardDto, "'downEventLogDto' is required");
    }

    @Override
    public String toString() {
        //
        return toJson();
    }

    public static RegisterDownEventCardCommand fromJson(String json) {
        //
        return JsonUtil.fromJson(json, RegisterDownEventCardCommand.class);
    }

    public static RegisterDownEventCardCommand sample() {
        //
        return new RegisterDownEventCardCommand(DownEventCardDto.sample());
    }

    public static void main(String[] args) {
        //
        log.info(sample().toPrettyJson());
    }
}


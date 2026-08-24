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

import java.util.List;

@Slf4j
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDownEventCardsCommand implements JsonSerializable {
    //
    private List<DownEventCardDto> downEventCardDtos;

    public void validate() {
        //
        Assert.notNull(downEventCardDtos, "'downEventLogDtos' is required");
    }

    @Override
    public String toString() {
        //
        return toJson();
    }

    public static RegisterDownEventCardsCommand fromJson(String json) {
        //
        return JsonUtil.fromJson(json, RegisterDownEventCardsCommand.class);
    }

    public static RegisterDownEventCardsCommand sample() {
        //
        List<DownEventCardDto> sample = List.of(DownEventCardDto.sample());
        return new RegisterDownEventCardsCommand(sample);
    }

    public static void main(String[] args) {
        //
        log.info(sample().toPrettyJson());
    }
}


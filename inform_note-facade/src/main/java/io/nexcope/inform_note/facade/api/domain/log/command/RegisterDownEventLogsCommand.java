package io.nexcope.inform_note.facade.api.domain.log.command;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.log.entity.dto.DownEventLogDto;
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
public class RegisterDownEventLogsCommand implements JsonSerializable {
    //
    private List<DownEventLogDto> downEventLogDtos;

    public void validate() {
        //
        Assert.notNull(downEventLogDtos, "'downEventLogDtos' is required");
    }

    @Override
    public String toString() {
        //
        return toJson();
    }

    public static RegisterDownEventLogsCommand fromJson(String json) {
        //
        return JsonUtil.fromJson(json, RegisterDownEventLogsCommand.class);
    }

    public static RegisterDownEventLogsCommand sample() {
        //
        List<DownEventLogDto> sample = List.of(DownEventLogDto.sample());
        return new RegisterDownEventLogsCommand(sample);
    }

    public static void main(String[] args) {
        //
        log.info(sample().toPrettyJson());
    }
}


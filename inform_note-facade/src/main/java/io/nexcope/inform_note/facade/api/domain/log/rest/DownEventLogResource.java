package io.nexcope.inform_note.facade.api.domain.log.rest;

import io.nexcope.inform_note.domain.log.entity.DownEventLog;
import io.nexcope.inform_note.domain.log.logic.DownEventLogLogic;
import io.nexcope.inform_note.facade.api.domain.log.command.RegisterDownEventLogCommand;
import io.nexcope.inform_note.facade.api.domain.log.command.RegisterDownEventLogsCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Down Event Log", description = "비가동 이벤트 로그 API")
@RestController
@RequestMapping("/domain/log")
@RequiredArgsConstructor
public class DownEventLogResource {
    //
    private final DownEventLogLogic downEventlogLogic;

    @Operation(summary = "비가동 이벤트 로그 단건 등록", description = "단건의 비가동 이벤트 로그를 등록합니다.")
    @PostMapping("/register/command")
    public String registerDownEventLog(@RequestBody RegisterDownEventLogCommand command) {
        //
        command.validate();
        return downEventlogLogic.register(command.getDownEventLogDto());
    }

    @Operation(summary = "비가동 이벤트 로그 다건 등록", description = "다건의 비가동 이벤트 로그를 일괄 등록합니다.")
    @PostMapping("/registers/command")
    public List<String> registerDownEventLogs(@RequestBody RegisterDownEventLogsCommand command) {
        //
        command.validate();
        return downEventlogLogic.registers(command.getDownEventLogDtos());
    }
}

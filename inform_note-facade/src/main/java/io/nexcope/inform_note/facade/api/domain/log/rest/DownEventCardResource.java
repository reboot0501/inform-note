package io.nexcope.inform_note.facade.api.domain.log.rest;

import io.nexcope.inform_note.domain.card.logic.DownEventCardLogic;
import io.nexcope.inform_note.facade.api.domain.log.command.RegisterDownEventCardCommand;
import io.nexcope.inform_note.facade.api.domain.log.command.RegisterDownEventCardsCommand;
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
@RequestMapping("/domain/card")
@RequiredArgsConstructor
public class DownEventCardResource {
    //
    private final DownEventCardLogic downEventCardLogic;

    @Operation(summary = "비가동 이벤트 로그 단건 등록", description = "단건의 비가동 이벤트 로그를 등록합니다.")
    @PostMapping("/register-down-event-card/command")
    public String registerDownEventCard(@RequestBody RegisterDownEventCardCommand command) {
        //
        command.validate();
        return downEventCardLogic.registerDownEventCard(command.getDownEventCardDto());
    }

    @Operation(summary = "비가동 이벤트 로그 다건 등록", description = "다건의 비가동 이벤트 로그를 일괄 등록합니다.")
    @PostMapping("/register-down-event-cards/command")
    public List<String> registerDownEventCards(@RequestBody RegisterDownEventCardsCommand command) {
        //
        command.validate();
        return downEventCardLogic.registerDownEventCards(command.getDownEventCardDtos());
    }
}

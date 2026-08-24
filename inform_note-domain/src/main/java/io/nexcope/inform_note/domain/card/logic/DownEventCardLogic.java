package io.nexcope.inform_note.domain.card.logic;

import io.nexcope.inform_note.base.domain.entity.OffsetElementList;
import io.nexcope.inform_note.domain.card.entity.DownEventCard;
import io.nexcope.inform_note.domain.card.entity.dto.DownEventCardDto;
import io.nexcope.inform_note.domain.card.entity.vo.DownEventCardSearchCriteria;
import io.nexcope.inform_note.domain.card.mapper.DownEventCardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DownEventCardLogic {
    //
    private final DownEventCardMapper mapper;

    public DownEventCard findById(String downEventId) {
        //
        return mapper.findById(downEventId).orElseGet(null);
    }

    public OffsetElementList<DownEventCard> findOffsetElementListByCriteria(DownEventCardSearchCriteria criteria) {
        //
        List<DownEventCard> results = mapper.findByCriteria(criteria);
        long totalCount = mapper.countByCriteria(criteria);
        int offset = criteria != null ? criteria.getOffset() : 0;
        int limit = criteria != null ? criteria.getLimit() : 20;
        return OffsetElementList.of(results, totalCount, offset, limit);
    }

    public String registerDownEventCard(DownEventCardDto dto) {
        //
        DownEventCard downEventCard = new DownEventCard(dto);
        mapper.insert(downEventCard);
        return downEventCard.getDownEventId();
    }

    public List<String> registerDownEventCards(List<DownEventCardDto> dtos) {
        //
        return dtos.stream().map(dto -> {
            //
            DownEventCard domain = new DownEventCard(dto);
            mapper.insert(domain);
            return domain.getDownEventId();
        }).toList();
    }

    public void modify(DownEventCard domain) {
        //
        mapper.update(domain);
    }

    public int deleteById(String downEventId) {
        //
        return mapper.deleteById(downEventId);
    }

}

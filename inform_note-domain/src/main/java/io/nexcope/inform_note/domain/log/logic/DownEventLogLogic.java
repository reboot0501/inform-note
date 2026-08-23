package io.nexcope.inform_note.domain.log.logic;

import io.nexcope.inform_note.base.domain.entity.OffsetElementList;
import io.nexcope.inform_note.domain.log.entity.DownEventLog;
import io.nexcope.inform_note.domain.log.entity.dto.DownEventLogDto;
import io.nexcope.inform_note.domain.log.entity.vo.DownEventLogSearchCriteria;
import io.nexcope.inform_note.domain.log.mapper.DownEventLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DownEventLogLogic {
    //
    private final DownEventLogMapper mapper;

    public DownEventLog findById(String downEventId) {
        //
        return mapper.findById(downEventId).orElseGet(null);
    }

    public OffsetElementList<DownEventLog> findOffsetElementListByCriteria(DownEventLogSearchCriteria criteria) {
        //
        List<DownEventLog> results = mapper.findByCriteria(criteria);
        long totalCount = mapper.countByCriteria(criteria);
        int offset = criteria != null ? criteria.getOffset() : 0;
        int limit = criteria != null ? criteria.getLimit() : 20;
        return OffsetElementList.of(results, totalCount, offset, limit);
    }

    public String register(DownEventLogDto dto) {
        //
        DownEventLog downEventLog = new DownEventLog(dto);
        mapper.insert(downEventLog);
        return downEventLog.getDownEventId();
    }

    public List<String> registers(List<DownEventLogDto> dtos) {
        //
        return dtos.stream().map(dto -> {
            //
            DownEventLog domain = new DownEventLog(dto);
            mapper.insert(domain);
            return domain.getDownEventId();
        }).toList();
    }

    public void modify(DownEventLog domain) {
        //
        mapper.update(domain);
    }

    public int deleteById(String downEventId) {
        //
        return mapper.deleteById(downEventId);
    }

}

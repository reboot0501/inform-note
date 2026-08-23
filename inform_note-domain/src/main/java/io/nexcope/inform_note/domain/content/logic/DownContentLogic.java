package io.nexcope.inform_note.domain.content.logic;

import io.nexcope.inform_note.domain.content.entity.DownContent;
import io.nexcope.inform_note.domain.content.entity.dto.DownContentDto;
import io.nexcope.inform_note.domain.content.mapper.DownContentMapper;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class DownContentLogic {
    //
    private final DownContentMapper mapper;

    public DownContent findById(String downEventId) {
        //
        return mapper.findById(downEventId).orElse(null);
    }

    public List<DownContent> findByIds(List<String> downEventIds) {
        //
        return mapper.findByIds(downEventIds);
    }

    public DownContent register(DownContentDto dto) {
        //
        DownContent downContent = new DownContent(dto);
        mapper.insert(downContent);
        return downContent;
    }

    public int modify(DownContent domain) {
        //
        return mapper.update(domain);
    }

    public int saveOrUpdate(DownContent domain) {
        //
        return mapper.saveOrUpdate(domain);
    }

    public int deleteById(String downEventId) {
        //
        return mapper.deleteById(downEventId);
    }

}

package io.nexcope.inform_note.domain.card.mapper;

import io.nexcope.inform_note.domain.card.entity.DownEventCard;
import io.nexcope.inform_note.domain.card.entity.vo.DownEventCardSearchCriteria;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DownEventCardMapper {
    // 단건 조회
    Optional<DownEventCard> findById(@Param("downEventId") String downEventId);
    // 전체 목록 조회
    List<DownEventCard> findAll();
    // 조건별 필터링 및 페이징 목록 조회
    List<DownEventCard> findByCriteria(DownEventCardSearchCriteria criteria);
    // 조건별 전체 건수 조회 (페이징용 Count)
    long countByCriteria(DownEventCardSearchCriteria criteria);
    // 등록 (Insert)
    int insert(DownEventCard downEventCard);
    // 수정 (Update)
    int update(DownEventCard downEventCard);
    // 삭제 (Delete)
    int deleteById(@Param("downEventId") String downEventId);
}

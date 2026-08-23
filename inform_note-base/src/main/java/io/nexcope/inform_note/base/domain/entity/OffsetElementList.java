package io.nexcope.inform_note.base.domain.entity;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OffsetElementList<T> implements JsonSerializable {
    //
    private List<T> results = new ArrayList<>();
    private long totalCount;
    private int offset;
    private int limit;
    private int page;
    private int size;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static <T> OffsetElementList<T> of(List<T> results, long totalCount) {
        int defaultLimit = results != null && !results.isEmpty() ? results.size() : 20;
        return of(results, totalCount, 0, defaultLimit);
    }

    public static <T> OffsetElementList<T> of(List<T> results, long totalCount, int offset, int limit) {
        int safeLimit = limit <= 0 ? (results != null && !results.isEmpty() ? results.size() : 20) : limit;
        int safeOffset = Math.max(0, offset);
        int page = (safeOffset / safeLimit) + 1;
        int totalPages = safeLimit > 0 ? (int) Math.ceil((double) totalCount / safeLimit) : 1;
        boolean hasNext = page < totalPages;
        boolean hasPrevious = page > 1;

        return new OffsetElementList<>(
                results != null ? results : new ArrayList<>(),
                totalCount,
                safeOffset,
                safeLimit,
                page,
                safeLimit,
                totalPages,
                hasNext,
                hasPrevious
        );
    }

    public static <T> OffsetElementList<T> empty() {
        return of(new ArrayList<>(), 0L, 0, 20);
    }

    @Override
    public String toString() {
        return toJson();
    }

    public static <T> OffsetElementList fromJson(String json) {
        return JsonUtil.fromJson(json, OffsetElementList.class);
    }
}

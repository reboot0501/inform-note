package io.nexcope.inform_note.domain.auth.entity.vo;

public enum SignupStatus {
    //
    PENDING,    // 회원가입 처리 대기
    COMPLETED,  // User 생성 완료
    FAILED      // User 생성 실패
}

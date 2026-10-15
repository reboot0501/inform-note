package io.nexcope.inform_note.domain.auth.mapper;

import io.nexcope.inform_note.domain.auth.entity.Signup;
import io.nexcope.inform_note.domain.auth.entity.vo.SignupStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SignupMapper {
    // 회원 가입 단건 조회
    Optional<Signup> findById(@Param("signupId") String signupId);
    // 회원 이메일로 존재여부 boolean 판단
    boolean existsByEmail(@Param("email") String email);
    // 회원 가입 상태별 목록 조회
    List<Signup> findBySignupStatus(@Param("signupStatus") SignupStatus signupStatus);
    // 회원 가입 등록
    int insert(Signup signup);
    // 회원 정보 수정
    int update(Signup signup);
    // 회원 정보 삭제
    int deleteById(@Param("signupId") String signupId);
}
